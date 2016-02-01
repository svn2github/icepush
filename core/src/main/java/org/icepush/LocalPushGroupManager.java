/*
 * Copyright 2004-2014 ICEsoft Technologies Canada Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS
 * IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.icepush;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.icepush.servlet.ServletContextConfiguration;
import org.icesoft.notify.cloud.core.CloudNotificationService;

public class LocalPushGroupManager
extends AbstractPushGroupManager
implements InternalPushGroupManager, PushGroupManager {
    private static final Logger LOGGER = Logger.getLogger(LocalPushGroupManager.class.getName());
    static final int DEFAULT_NOTIFICATIONQUEUE_SIZE = 1000;
    static final int DEFAULT_MIN_CLOUDPUSH_INTERVAL = 10 * 1000;
    static final int DEFAULT_CLOUDPUSHID_TIMEOUT = 30 * 60 * 1000;
    static final int DEFAULT_PUSHID_TIMEOUT = 2 * 60 * 1000;
    static final int DEFAULT_GROUP_TIMEOUT = 2 * 60 * 1000;
    private static final int GROUP_SCANNING_TIME_RESOLUTION = 3000; // ms
    private static final Comparator<Notification> ScheduledAtComparator = new Comparator<Notification>() {
        public int compare(Notification a, Notification b) {
            return (int) (a.getPushConfiguration().getScheduledAt() - b.getPushConfiguration().getScheduledAt());
        }
    };
    private final Notification NOOP =
        new Notification("---", null) {
            public void run() {
            }
        };
    private final Map<String, BlockingConnectionServer> blockingConnectionServerMap =
        new ConcurrentHashMap<String, BlockingConnectionServer>();
    private final ConcurrentMap<String, Browser> browserMap = new ConcurrentHashMap<String, Browser>();
    private final ConcurrentMap<String, Group> groupMap = new ConcurrentHashMap<String, Group>();
    private final ConcurrentMap<String, PushID> pushIDMap = new ConcurrentHashMap<String, PushID>();
    private final ConcurrentMap<String, ConfirmationTimeout> confirmationTimeoutMap =
        new ConcurrentHashMap<String, ConfirmationTimeout>();
    private final ConcurrentMap<String, ExpiryTimeout> expiryTimeoutMap =
        new ConcurrentHashMap<String, ExpiryTimeout>();
    private final ConcurrentMap<String, NotifyBackURI> parkedPushIDs = new ConcurrentHashMap<String, NotifyBackURI>();
    /*
        There is no ConcurrentSet or ConcurrentHashSet.  As of JDK 1.6 there is a static method in the Collections class
        <E> Set<E> newSetFromMap(Map<e, Boolean> map) that can be used to create a Set backed by a ConcurrentMap.  But
        ICEpush needs to be JDK 1.5 compatible.  Therefor, a ReentrantLock is used for this Set.
     */
    private final ReentrantLock pendingNotifiedPushIDSetLock = new ReentrantLock();
    private final Set<NotificationEntry> pendingNotifiedPushIDSet = new HashSet<NotificationEntry>();
    private final LocalNotificationBroadcaster outboundNotifier = new LocalNotificationBroadcaster();
    private final Timer timer = new Timer("Notification queue consumer.", true);
    private final TimerTask queueConsumer;
    private final Lock notificationQueueLock = new ReentrantLock();
    private final Condition notificationAvailableCondition = getNotificationQueueLock().newCondition();
    private final Queue<Notification> notificationQueue;
    private final long groupTimeout;
    private final long cloudPushIDTimeout;
    private final long pushIDTimeout;
    private final long minCloudPushInterval;
    private final ServletContext context;

    private long lastTouchScan = System.currentTimeMillis();
    private long lastExpiryScan = System.currentTimeMillis();

    public LocalPushGroupManager(final ServletContext servletContext) {
        this.context = servletContext;
        Configuration configuration = new ServletContextConfiguration("org.icepush", servletContext);
        this.groupTimeout = configuration.getAttributeAsLong("groupTimeout", DEFAULT_GROUP_TIMEOUT);
        this.pushIDTimeout = configuration.getAttributeAsLong("pushIdTimeout", DEFAULT_PUSHID_TIMEOUT);
        this.cloudPushIDTimeout = configuration.getAttributeAsLong("cloudPushIdTimeout", DEFAULT_CLOUDPUSHID_TIMEOUT);
        this.minCloudPushInterval = configuration.getAttributeAsLong("minCloudPushInterval", DEFAULT_MIN_CLOUDPUSH_INTERVAL);
        int notificationQueueSize = configuration.getAttributeAsInteger("notificationQueueSize", DEFAULT_NOTIFICATIONQUEUE_SIZE);
        this.notificationQueue = new LinkedBlockingQueue<Notification>(notificationQueueSize);
        this.queueConsumer = new QueueConsumerTask();
        this.timer.schedule(queueConsumer, 0);
    }
    
    public void addBlockingConnectionServer(final String browserID, final BlockingConnectionServer server) {
        blockingConnectionServerMap.put(browserID, server);
    }

    public boolean addBrowser(final Browser browser) {
        return addBrowser(getModifiableBrowserMap(), browser);
    }

    public boolean addMember(final String groupName, final String pushID) {
        return addMember(getModifiableGroupMap(), getModifiablePushIDMap(), groupName, pushID);
    }

    public boolean addMember(final String groupName, final String pushID, final PushConfiguration pushConfiguration) {
        return addMember(getModifiableGroupMap(), getModifiablePushIDMap(), groupName, pushID, pushConfiguration);
    }

    public void addNotificationReceiver(final NotificationBroadcaster.Receiver observer) {
        outboundNotifier.addReceiver(observer);
    }

    public void backOff(final String browserID, final long delay) {
        BlockingConnectionServer server = blockingConnectionServerMap.get(browserID);
        if (server != null) {
            server.backOff(delay);
        }
    }

    public boolean cancelConfirmationTimeout(final String browserID) {
        ConfirmationTimeout confirmationTimeout = getConfirmationTimeoutMap().remove(browserID);
        if (confirmationTimeout != null) {
            confirmationTimeout.cancel();
            confirmationTimeout = null;
            getBrowser(browserID).setPushConfiguration(null);
            return true;
        }
        return false;
    }

    public boolean cancelExpiryTimeout(final String pushID) {
        ExpiryTimeout expiryTimeout = getExpiryTimeoutMap().remove(pushID);
        if (expiryTimeout != null) {
            expiryTimeout.cancel();
            expiryTimeout = null;
            return true;
        }
        return false;
    }

    public void cancelExpiryTimeouts(final String browserID) {
        Browser browser = getBrowser(browserID);
        for (final String pushIDString : browser.getPushIDSet()) {
            PushID pushID = getPushIDMap().get(pushIDString);
            if (pushID != null) {
                pushID.cancelExpiryTimeout();
            }
        }
    }

    public void clearPendingNotification(final String pushID) {
        getPendingNotifiedPushIDSetLock().lock();
        try {
            clearPendingNotifications(getModifiablePendingNotifiedPushIDSet(), pushID);
        } finally {
            getPendingNotifiedPushIDSetLock().unlock();
        }
    }

    public void clearPendingNotifications(final Set<String> pushIDSet) {
        getPendingNotifiedPushIDSetLock().lock();
        try {
            clearPendingNotifications(getModifiablePendingNotifiedPushIDSet(), pushIDSet);
        } finally {
            getPendingNotifiedPushIDSetLock().unlock();
        }
    }

    public void deleteNotificationReceiver(final NotificationBroadcaster.Receiver observer) {
        outboundNotifier.deleteReceiver(observer);
    }

    public Browser getBrowser(final String browserID) {
        if (browserID == null) {
            return null;
        }
        return getBrowserMap().get(browserID);
    }

    public Map<String, Browser> getBrowserMap() {
        return Collections.unmodifiableMap(getModifiableBrowserMap());
    }

    public Group getGroup(final String groupName) {
        return getGroup(getModifiableGroupMap(), groupName);
    }

    public Map<String, Group> getGroupMap() {
        return Collections.unmodifiableMap(getModifiableGroupMap());
    }

    public Map<String, String[]> getGroupPushIDsMap() {
        return getGroupPushIDsMap(getModifiableGroupMap());
    }

    public CloudNotificationService getCloudNotificationService() {
        return (CloudNotificationService)context.getAttribute(CloudNotificationService.class.getName());
    }

    public Set<NotificationEntry> getPendingNotificationSet() {
        getPendingNotifiedPushIDSetLock().lock();
        try {
            return new HashSet<NotificationEntry>(getPendingNotifiedPushIDSet());
        } finally {
            getPendingNotifiedPushIDSetLock().unlock();
        }
    }

    public Set<NotificationEntry> getPendingNotifiedPushIDSet() {
        return Collections.unmodifiableSet(getModifiablePendingNotifiedPushIDSet());
    }

    public PushID getPushID(final String pushID) {
        return getPushIDMap().get(pushID);
    }

    public Map<String, PushID> getPushIDMap() {
        return Collections.unmodifiableMap(getModifiablePushIDMap());
    }

    public boolean isParked(final String pushID) {
        return parkedPushIDs.containsKey(pushID);
    }

    public NotifyBackURI newNotifyBackURI(final String uri) {
        return new NotifyBackURI(uri);
    }

    public void park(final String pushID, final NotifyBackURI notifyBackURI) {
        PushID _pushID = getPushID(pushID);
        if (_pushID != null) {
            if (_pushID.isCloudPushEnabled()) {
                parkedPushIDs.put(pushID, notifyBackURI);
            }
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Unknown Push-ID '" + pushID + "' not eligible for parking.");
            }
        }
    }

    public void pruneParkedIDs(final NotifyBackURI notifyBackURI, final Set<String> listenedPushIDSet) {
        for (final Map.Entry<String, NotifyBackURI> parkedPushIDEntry : parkedPushIDs.entrySet()) {
            String parkedPushID = parkedPushIDEntry.getKey();
            if (parkedPushIDEntry.getValue().getURI().equals(notifyBackURI.getURI()) &&
                !listenedPushIDSet.contains(parkedPushID)) {

                parkedPushIDs.remove(parkedPushID);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Removed unlistened parked PushID '" + parkedPushID + "' for " +
                            "NotifyBackURI '" + notifyBackURI + "'.");
                }
            }
        }
    }

    public void push(final String groupName) {
        push(groupName, (String)null);
    }

    public void push(final String groupName, final String payload) {
        if (LOGGER.isLoggable(Level.FINE)) {
            if (payload != null && payload.trim().length() != 0) {
                LOGGER.log(
                    Level.FINE,
                    "Request for a Push Notification with Payload '" + payload + "' for Group '" + groupName + "'."
                );
            } else {
                LOGGER.log(
                    Level.FINE,
                    "Request for a Push Notification for Group '" + groupName + "'."
                );
            }
        }
        Notification _notification = newNotification(groupName, payload);
        getNotificationQueueLock().lock();
        try {
            if (!getNotificationQueue().contains(_notification)) {
                if (getNotificationQueue().offer(_notification)) {
                    getNotificationAvailableCondition().signalAll();
                } else {
                    // Leave at INFO
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.log(
                            Level.INFO,
                            "Request for a Push Notification for Group '" + groupName + "' was dropped, " +
                                "maximum size queue reached."
                        );
                    }
                }
            } else {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Request for a Push Notification for Push Group '" + groupName + "' was ignored, " +
                            "duplicate detected."
                    );
                }
            }
        } finally {
            getNotificationQueueLock().unlock();
        }
    }

    public void push(final String groupName, final PushConfiguration pushConfiguration) {
        push(groupName, (String)null, pushConfiguration);
    }

    public void push(final String groupName, final String payload, final PushConfiguration pushConfiguration) {
        if (LOGGER.isLoggable(Level.FINE)) {
            if (payload != null && payload.trim().length() != 0) {
                LOGGER.log(
                    Level.FINE,
                    "Request for a Push Notification with Payload '" + payload + "' for Group '" + groupName + "'.  " +
                        "(Push Configuration: '" + pushConfiguration + "')"
                );
            } else {
                LOGGER.log(
                    Level.FINE,
                    "Request for a Push Notification for Group '" + groupName + "'.  " +
                        "(Push Configuration: '" + pushConfiguration + "')"
                );
            }
        }
        Notification _notification;
        if (isOutOfBandNotification(pushConfiguration)) {
            _notification = newOutOfBandNotification(groupName, payload, pushConfiguration);
        } else {
            _notification = newNotification(groupName, payload, pushConfiguration);
        }
        getNotificationQueueLock().lock();
        try {
            if (!getNotificationQueue().contains(_notification)) {
                //add this notification to a blocking queue
                getNotificationQueue().add(_notification);
                getNotificationAvailableCondition().signalAll();
            } else {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Request for a Push Notification for Push Group '" + groupName + "' was ignored, " +
                            "duplicate detected."
                    );
                }
            }
        } finally {
            getNotificationQueueLock().unlock();
        }
    }

    public void removeBlockingConnectionServer(final String browserID) {
        blockingConnectionServerMap.remove(browserID);
    }

    public boolean removeBrowser(final Browser browser) {
        return removeBrowser(getModifiableBrowserMap(), browser);
    }

    public boolean removeGroup(final String groupName) {
        return removeGroup(getModifiableGroupMap(), groupName);
    }

    public boolean removeMember(final String groupName, final String pushID) {
        return removeMember(getModifiableGroupMap(), groupName, pushID);
    }

    public void removePendingNotification(final String pushID) {
        clearPendingNotification(pushID);
    }

    public void removePendingNotifications(final Set<String> pushIDSet) {
        clearPendingNotifications(pushIDSet);
    }

    public boolean removePushID(final String pushID) {
        return removePushID(getModifiablePushIDMap(), pushID);
    }

    public void scan(final String[] confirmedPushIDs) {
        scan(getModifiableGroupMap(), confirmedPushIDs);
    }

    public void shutdown() {
        outboundNotifier.shutdown();
        queueConsumer.cancel();
        timer.cancel();
    }

    public void startConfirmationTimeout(final Set<NotificationEntry> notificationSet) {
        for (final NotificationEntry _notificationEntry : notificationSet) {
            startConfirmationTimeout(_notificationEntry);
        }
    }

    public boolean startConfirmationTimeout(
        final String browserID, final String groupName) {

        return startConfirmationTimeout(browserID, groupName, getBrowser(browserID).getSequenceNumber());
    }

    public boolean startConfirmationTimeout(
        final String browserID, final String groupName, final long sequenceNumber) {

        Browser browser = getBrowser(browserID);
        if (browser.isCloudPushEnabled()) {
            NotifyBackURI notifyBackURI = browser.getNotifyBackURI();
            if (notifyBackURI != null) {
                long now = System.currentTimeMillis();
                long timeout = browser.getStatus().getConnectionRecreationTimeout() * 2;
                LOGGER.log(Level.FINE, "Calculated confirmation timeout: '" + timeout + "'");
                if (notifyBackURI.getTimestamp() + browser.getMinCloudPushInterval() <= now + timeout) {
                    return startConfirmationTimeout(browserID, groupName, sequenceNumber, timeout);
                } else {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(
                            Level.FINE,
                            "Timeout is within the minimum Cloud Push interval for URI '" + notifyBackURI + "'. (" +
                                "timestamp: '" + notifyBackURI.getTimestamp() + "', " +
                                "minCloudPushInterval: '" + browser.getMinCloudPushInterval() + "', " +
                                "now: '" + now + "', " +
                                "timeout: '" + timeout + "'" +
                            ")");
                    }
                }
            }
        }
        return false;
    }

    public boolean startConfirmationTimeout(
        final String browserID, final String groupName, final long sequenceNumber, final long timeout) {

        Browser browser = getBrowser(browserID);
        if (browser.isCloudPushEnabled()) {
            NotifyBackURI notifyBackURI = browser.getNotifyBackURI();
            if (notifyBackURI != null &&
                notifyBackURI.getTimestamp() + browser.getMinCloudPushInterval() <=
                    System.currentTimeMillis() + timeout &&
                isOutOfBandNotification(browser.getPushConfiguration())) {

                ConfirmationTimeout confirmationTimeout = getConfirmationTimeoutMap().get(browserID);
                if (confirmationTimeout == null) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(
                            Level.FINE,
                            "Start confirmation timeout for Browser '" + browserID + "' (" +
                                "URI: '" + notifyBackURI + "', " +
                                "timeout: '" + timeout + "', " +
                                "sequence number: '" + sequenceNumber + "'" +
                            ").");
                    }
                    try {

                        ((Timer)PushInternalContext.getInstance().getAttribute(Timer.class.getName() + "$confirmation")
                        ).
                            schedule(
                                confirmationTimeout = newConfirmationTimeout(browserID, groupName, timeout), timeout
                            );
                        getConfirmationTimeoutMap().put(browserID, confirmationTimeout);
                        return true;
                    } catch (final IllegalStateException exception) {
                        // timeoutTimer was cancelled or its timer thread terminated.
                        return false;
                    }
                }
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Confirmation timeout already scheduled for PushID '" + browserID + "' " +
                            "(URI: '" + notifyBackURI + "', timeout: '" + timeout + "').");
                }
            }
        }
        return false;
    }

    public boolean startExpiryTimeout(final String pushID) {
        PushID _pushID = getPushID(pushID);
        if (_pushID != null) {
            String _browserID = _pushID.getBrowserID();
            return startExpiryTimeout(pushID, null, _browserID != null ? getBrowser(_browserID).getSequenceNumber() : -1);
        } else {
            return startExpiryTimeout(pushID, null, -1);
        }
    }

    public boolean startExpiryTimeout(final String pushID, final String browserID, final long sequenceNumber) {
        PushID _pushID = getPushID(pushID);
        boolean _isCloudPushID = browserID != null && getBrowser(browserID).getNotifyBackURI() != null;
        if (!getExpiryTimeoutMap().containsKey(pushID)) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Start expiry timeout for PushID '" + pushID + "' (" +
                        "timeout: '" +
                            (!_isCloudPushID ? _pushID.getPushIDTimeout() : _pushID.getCloudPushIDTimeout()) +
                        "', " +
                        "sequence number: '" + sequenceNumber + "'" +
                    ").");
            }
            try {
                ExpiryTimeout _expiryTimeout = newExpiryTimeout(pushID, _isCloudPushID);
                ((Timer)PushInternalContext.getInstance().getAttribute(Timer.class.getName() + "$expiry")).
                    schedule(
                        _expiryTimeout,
                        !_isCloudPushID ? pushIDTimeout : cloudPushIDTimeout);
                getExpiryTimeoutMap().put(pushID, _expiryTimeout);
                return true;
            } catch (final IllegalStateException exception) {
                // timeoutTimer was cancelled or its timer thread terminated.
                return false;
            }
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                Level.FINE,
                "Expiry timeout already scheduled for PushID '" + pushID + "' (" +
                    "timeout: '" +
                        (!_isCloudPushID ? _pushID.getPushIDTimeout() : _pushID.getCloudPushIDTimeout()) +
                    "'" +
                ").");
        }
        return false;
    }

    public void startExpiryTimeouts(final String browserID) {
        Browser browser = getBrowser(browserID);
        for (final String pushIDString : browser.getPushIDSet()) {
            PushID pushID = getPushID(pushIDString);
            if (pushID != null) {
                try {
                    pushID.startExpiryTimeout(browserID, browser.getSequenceNumber());
                } catch (final NullPointerException exception) {
                    throw exception;
                }
            }
        }
    }

    protected boolean addBrowser(final Map<String, org.icepush.Browser> browserMap, final Browser browser) {
        boolean _modified = false;
        if (!browserMap.containsKey(browser.getID())) {
            browserMap.put(browser.getID(), browser);
            _modified = true;
        }
        return _modified;
    }

    protected boolean addMember(
        final Map<String, Group> groupMap, final Map<String, PushID> pushIDMap, final String groupName,
        final String pushID) {

        return addMember(groupMap, pushIDMap, groupName, pushID, null);
    }

    protected boolean addMember(
        final Map<String, Group> groupMap, final Map<String, PushID> pushIDMap, final String groupName,
        final String pushID, final PushConfiguration pushConfiguration) {

        boolean _modified = false;
        if (groupMap != null && pushIDMap != null && groupName != null && pushID != null) {
            PushID _pushID;
            if (pushIDMap.containsKey(pushID)) {
                _pushID = pushIDMap.get(pushID);
            } else {
                _pushID = newPushID(pushID);
                pushIDMap.put(pushID, _pushID);
                addBrowser(newBrowser(_pushID.getBrowserID(), getMinCloudPushInterval()));
                _pushID.startExpiryTimeout();
                _modified = true;
            }
            _modified |= _pushID.addToGroup(groupName, pushConfiguration);
            _modified |= addToGroup(groupMap, groupName, pushID);
            memberAdded(groupName, pushID);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Added PushID '" + pushID + "' to Push Group '" + groupName + "' with " +
                        "Push Configuration '" + pushConfiguration + "'.");
            }
        }
        return _modified;
    }

    protected boolean addToGroup(
        final String groupName, final String pushID) {

        return addToGroup(getModifiableGroupMap(), groupName, pushID);
    }

    protected boolean addToGroup(
        final Map<String, Group> groupMap, final String groupName, final String pushID) {

        boolean _modified = false;
        Group _group;
        if (groupMap.containsKey(groupName)) {
            _group = groupMap.get(groupName);
        } else {
            _group = newGroup(groupName);
            groupMap.put(groupName, _group);
            _modified = true;
        }
        _modified |= _group.addPushID(pushID);
        return _modified;
    }

    protected void clearPendingNotifications(
        final Set<NotificationEntry> pendingNotifiedPushIDSet, final String pushID) {

        Iterator<NotificationEntry> pendingNotifiedPushIDIterator =
            new HashSet<NotificationEntry>(pendingNotifiedPushIDSet).iterator();
        while (pendingNotifiedPushIDIterator.hasNext()) {
            NotificationEntry _pendingNotifiedPushID = pendingNotifiedPushIDIterator.next();
            if (_pendingNotifiedPushID.getPushID().equals(pushID)) {
                pendingNotifiedPushIDSet.remove(_pendingNotifiedPushID);
            }
        }
    }

    protected void clearPendingNotifications(
        final Set<NotificationEntry> pendingNotifiedPushIDSet, final Set<String> pushIDSet) {

        Set<NotificationEntry> _copyPendingNotifiedPushIDSet =
            new HashSet<NotificationEntry>(pendingNotifiedPushIDSet);
        Iterator<NotificationEntry> _pendingNotifiedPushIDIterator =
            _copyPendingNotifiedPushIDSet.iterator();
        while (_pendingNotifiedPushIDIterator.hasNext()) {
            NotificationEntry _pendingNotifiedPushID = _pendingNotifiedPushIDIterator.next();
            if (pushIDSet.contains(_pendingNotifiedPushID.getPushID())) {
                _pendingNotifiedPushIDIterator.remove();
            }
        }
        pendingNotifiedPushIDSet.clear();
        pendingNotifiedPushIDSet.addAll(_copyPendingNotifiedPushIDSet);
    }

    protected long getCloudPushIDTimeout() {
        return cloudPushIDTimeout;
    }

    protected Map<String, ConfirmationTimeout> getConfirmationTimeoutMap() {
        return confirmationTimeoutMap;
    }

    protected Map<String, ExpiryTimeout> getExpiryTimeoutMap() {
        return expiryTimeoutMap;
    }

    protected Group getGroup(final Map<String, Group> groupMap, final String groupName) {
        return groupMap.get(groupName);
    }

    protected Map<String, String[]> getGroupPushIDsMap(final Map<String, Group> groupMap) {
        Map<String, String[]> groupPushIDsMap = new HashMap<String, String[]>();
        for (Group group : new ArrayList<Group>(groupMap.values())) {
            groupPushIDsMap.put(group.getName(), group.getPushIDs());
        }
        return groupPushIDsMap;
    }

    protected long getGroupTimeout() {
        return groupTimeout;
    }

    protected long getMinCloudPushInterval() {
        return minCloudPushInterval;
    }

    protected ConcurrentMap<String, Browser> getModifiableBrowserMap() {
        return browserMap;
    }

    protected ConcurrentMap<String, Group> getModifiableGroupMap() {
        return groupMap;
    }

    protected Set<NotificationEntry> getModifiablePendingNotifiedPushIDSet() {
        return pendingNotifiedPushIDSet;
    }

    protected ConcurrentMap<String, PushID> getModifiablePushIDMap() {
        return pushIDMap;
    }

    protected Condition getNotificationAvailableCondition() {
        return notificationAvailableCondition;
    }

    protected Queue<Notification> getNotificationQueue() {
        return notificationQueue;
    }

    protected Lock getNotificationQueueLock() {
        return notificationQueueLock;
    }

    protected Lock getPendingNotifiedPushIDSetLock() {
        return pendingNotifiedPushIDSetLock;
    }

    protected long getPushIDTimeout() {
        return pushIDTimeout;
    }

    protected boolean isOutOfBandNotification(final PushConfiguration pushConfiguration) {
        return pushConfiguration != null && pushConfiguration.getAttributes().containsKey("subject");
    }

    protected Browser newBrowser(final String browserID, final long minCloudPushInterval) {
        return new Browser(browserID, minCloudPushInterval);
    }

    protected ConfirmationTimeout newConfirmationTimeout(
        final String browserID, final String groupName, final long timeout) {

        return new ConfirmationTimeout(browserID, groupName, timeout, getBrowser(browserID).getMinCloudPushInterval());
    }

    protected ExpiryTimeout newExpiryTimeout(final String pushID, final boolean isCloudPushID) {
        return new ExpiryTimeout(pushID, isCloudPushID);
    }

    protected Group newGroup(final String name) {
        return new Group(name, getGroupTimeout());
    }

    protected Notification newNotification(
        final String groupName, final String payload) {

        return new Notification(groupName, payload);
    }

    protected Notification newNotification(
        final String groupName, final String payload, final PushConfiguration pushConfiguration) {

        return new Notification(groupName, payload, pushConfiguration);
    }

    protected OutOfBandNotification newOutOfBandNotification(
        final String groupName, final String payload, final PushConfiguration pushConfiguration) {

        return new OutOfBandNotification(groupName, payload, pushConfiguration);
    }

    protected PushID newPushID(final String id) {
        return new PushID(id, getPushIDTimeout(), getCloudPushIDTimeout());
    }

    protected boolean removeBrowser(final Map<String, org.icepush.Browser> browserMap, final Browser browser) {
        return browserMap.remove(browser.getID()) != null;
    }

    protected boolean removeGroup(final Map<String, Group> groupMap, final String groupName) {
        return groupMap.remove(groupName) != null;
    }

    protected boolean removeMember(final Map<String, Group> groupMap, final String groupName, final String pushID) {
        boolean _modified = false;
        if (groupMap != null && groupName != null && pushID != null) {
            Group group = groupMap.get(groupName);
            if (group != null) {
                _modified = group.removePushID(pushID);
                PushID id = pushIDMap.get(pushID);
                if (id != null) {
                    _modified |= id.removeFromGroup(groupName);
                }
                memberRemoved(groupName, pushID);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Removed PushID '" + pushID + "' from Push Group '" + groupName + "'.");
                }
            }
        }
        return _modified;
    }

    protected boolean removePushID(final Map<String, PushID> pushIDMap, final String pushID) {
        return pushIDMap.remove(pushID) != null;
    }

    protected void scan(final Map<String, Group> groupMap, final String[] confirmedPushIDs) {
        Set<String> pushIDSet = new HashSet<String>(Arrays.asList(confirmedPushIDs));
        long now = System.currentTimeMillis();
        for (final Group group : groupMap.values()) {
            group.touchIfMatching(pushIDSet);
        }
        //avoid to scan/touch the groups on each notification
        if (lastTouchScan + GROUP_SCANNING_TIME_RESOLUTION < now) {
            try {
                for (final Group group : groupMap.values()) {
                    group.discardIfExpired();
                }
            } finally {
                lastTouchScan = now;
                lastExpiryScan = now;
            }
        }
    }

    protected void scanForExpiry() {
        scanForExpiry(getModifiableGroupMap());
    }

    protected void scanForExpiry(final Map<String, Group> groupMap) {
        long now = System.currentTimeMillis();
        //avoid to scan/touch the groups on each notification
        if (lastExpiryScan + GROUP_SCANNING_TIME_RESOLUTION < now) {
            try {
                for (final Group group : groupMap.values()) {
                    group.discardIfExpired();
                }
            } finally {
                lastExpiryScan = now;
            }
        }
    }

    protected void startConfirmationTimeout(final NotificationEntry notificationEntry) {
        PushID _pushID = getPushID(notificationEntry.getPushID());
        if (_pushID != null) {
            Browser _browser = getBrowser(_pushID.getBrowserID());
            if (_browser != null) {
                _browser.startConfirmationTimeout(notificationEntry.getGroupName());
            }
        }
    }

    protected class Notification
    implements Runnable {
        protected final Set<String> exemptPushIDSet = new HashSet<String>();

        protected final String groupName;
        protected final String payload;
        protected final PushConfiguration pushConfiguration;

        protected Notification(
            final String groupName, final String payload) {

            this(groupName, payload, new PushConfiguration());
        }

        protected Notification(
            final String groupName, final String payload, final PushConfiguration pushConfiguration) {

            this.groupName = groupName;
            this.payload = payload;
            this.pushConfiguration = pushConfiguration;
            Set<String> pushIDSet = (Set<String>)this.pushConfiguration.getAttributes().get("pushIDSet");
            if (pushIDSet != null) {
                this.getExemptPushIDSet().addAll(pushIDSet);
            }
        }

        public void coalesceWith(Notification nextNotification) {
            Group group = getModifiableGroupMap().get(getGroupName());
            if (group != null) {
                nextNotification.getExemptPushIDSet().addAll(Arrays.asList(group.getPushIDs()));
            }
        }

        @Override
        public boolean equals(final Object object) {
            return
                object instanceof Notification &&
                    ((Notification)object).getExemptPushIDSet().containsAll(getExemptPushIDSet()) &&
                    ((Notification)object).getExemptPushIDSet().size() == getExemptPushIDSet().size() &&
                    ((Notification)object).getGroupName().equals(getGroupName()) &&
                    (
                        (((Notification)object).getPayload() == null && getPayload() == null) ||
                        (
                            ((Notification)object).getPayload() != null &&
                            ((Notification)object).getPayload().equals(getPayload())
                        )
                    ) &&
                    ((Notification)object).getPushConfiguration().equals(getPushConfiguration());
        }

        @Override
        public int hashCode() {
            return getGroupName().hashCode();
        }

        public void run() {
            try {
                Group group = getModifiableGroupMap().get(getGroupName());
                if (group != null) {
                    Set<String> pushIDSet = new HashSet<String>(Arrays.asList(group.getPushIDs()));
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(
                            Level.FINE,
                            "Notification triggered for Group '" + getGroupName() + "' with " +
                                "original Push-ID Set '" + pushIDSet + "'.");
                    }
                    pushIDSet.removeAll(getExemptPushIDSet());
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(
                            Level.FINE,
                            "Notification triggered for Group '" + getGroupName() + "' with " +
                                "Push-ID Set '" + pushIDSet + "' after exemption.");
                    }
                    Set<NotificationEntry> notificationEntrySet = new HashSet<NotificationEntry>();
                    for (final String pushID : pushIDSet) {
                        notificationEntrySet.add(
                            newNotificationEntry(pushID, getGroupName(), getPayload(), getPushConfiguration())
                        );
                    }
                    filterNotificationEntrySet(notificationEntrySet);
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(
                            Level.FINE,
                            "Notification triggered for Group '" + getGroupName() + "' with " +
                                "Notification Entry Set '" + notificationEntrySet + "' after filtering.");
                    }
                    for (final NotificationEntry notificationEntry : notificationEntrySet) {
                        Browser browser = getBrowser(getPushID(notificationEntry.getPushID()).getBrowserID());
                        if (browser != null) {
                            browser.setPushConfiguration(getPushConfiguration());
                        }
                    }
                    getPendingNotifiedPushIDSetLock().lock();
                    try {
                        getModifiablePendingNotifiedPushIDSet().addAll(notificationEntrySet);
                    } finally {
                        getPendingNotifiedPushIDSetLock().unlock();
                    }
                    beforeBroadcast(notificationEntrySet);
                    outboundNotifier.broadcast(notificationEntrySet, getPushConfiguration().getDuration());
                    pushed(getGroupName());
                }
            } finally {
                scanForExpiry();
            }
        }

        protected void beforeBroadcast(final Set<NotificationEntry> notificationEntrySet) {
        }

        protected void filterNotificationEntrySet(final Set<NotificationEntry> notificationEntrySet) {
        }

        protected Set<String> getExemptPushIDSet() {
            return exemptPushIDSet;
        }

        protected String getGroupName() {
            return groupName;
        }

        protected String getPayload() {
            return payload;
        }

        protected PushConfiguration getPushConfiguration() {
            return pushConfiguration;
        }

        protected NotificationEntry newNotificationEntry(
            final String pushID, final String groupName, final String payload,
            final PushConfiguration pushConfiguration) {

            return new NotificationEntry(pushID, groupName, payload, pushConfiguration);
        }
    }

    protected class OutOfBandNotification
    extends Notification
    implements Runnable {
        protected OutOfBandNotification(
            final String groupName, final String payload, final PushConfiguration pushConfiguration) {

            super(groupName, payload, pushConfiguration);
        }

        @Override
        protected void beforeBroadcast(final Set<NotificationEntry> notificationEntrySet) {
            // Only needed for Cloud Push
            startConfirmationTimeout(notificationEntrySet);
        }
    }

    private class QueueConsumerTask extends TimerTask {
        private boolean running = true;

        public void run() {
            try {
                //take tasks from the queue and execute them serially
                while (running) {
                    try {
                        long _currentTime = System.currentTimeMillis();
                        //block until notifications are available
                        getNotificationQueueLock().lock();
                        try {
                            if (getNotificationQueue().isEmpty()) {
                                getNotificationAvailableCondition().await();
                            }
//                            Notification notification = getNotificationQueue().poll();
//                            //put back notification, need to extract the next scheduled notification
//                            queue.offer(notification);
                            //search what notification needs to be fired now
                            TreeSet<Notification> _notificationTreeSet =
                                new TreeSet<Notification>(ScheduledAtComparator);
                            _notificationTreeSet.addAll(getNotificationQueue());
                            Notification _scheduledNotification = _notificationTreeSet.first();
                            long _scheduledAt = _scheduledNotification.getPushConfiguration().getScheduledAt();
                            if (_scheduledAt < _currentTime) {
                                //ready to send
                                getNotificationQueue().remove(_scheduledNotification);
                                long _duration = _scheduledNotification.getPushConfiguration().getDuration();
                                long _endOfScheduledNotification = _scheduledAt + _duration;

                                for (final Notification _nextScheduledNotification : _notificationTreeSet) {
                                    //skip first notification
                                    if (_nextScheduledNotification == _scheduledNotification) {
                                        continue;
                                    }
                                    //test if it overlaps
                                    if (_endOfScheduledNotification >
                                            _nextScheduledNotification.getPushConfiguration().getScheduledAt()) {

                                        //coalesce current notification with next overlapping notification
                                        _scheduledNotification.coalesceWith(_nextScheduledNotification);
                                    } else {
                                        //stop when notification windows (durations) do not overlap anymore
                                        break;
                                    }
                                }
                                _scheduledNotification.run();
                            }
                        } catch (NoClassDefFoundError e) {
                            //ignore the application WAR was removed from the file system
                        } finally {
                            getNotificationQueueLock().unlock();
                        }
                    } catch (Throwable t)  {
                        LOGGER.log(Level.WARNING, "Notification queue encountered ", t);
                    }
                }
            } catch (Exception exception) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(
                        Level.WARNING, "Exception caught on " + this.getClass().getName() + " TimerTask.", exception);
                }
            }
        }

        public boolean cancel() {
            running = false;
            getNotificationQueueLock().lock();
            try {
                getNotificationQueue().offer(NOOP);//add noop to unblock the queue
                getNotificationAvailableCondition().signalAll();
                return super.cancel();
            } finally {
                getNotificationQueueLock().unlock();
            }
        }
    }
}
