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
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.icepush.servlet.ServletContextConfiguration;

public class LocalPushGroupManager
extends AbstractPushGroupManager
implements InternalPushGroupManager, PushGroupManager {
    private static final Logger LOGGER = Logger.getLogger(LocalPushGroupManager.class.getName());
    private static final String[] STRINGS = new String[0];
    private static final int GROUP_SCANNING_TIME_RESOLUTION = 3000; // ms
    private static final OutOfBandNotifier NOOPOutOfBandNotifier = new OutOfBandNotifier() {
        public void broadcast(final PushNotification notification, final String[] browserIDs, final String groupName) {
            System.out.println("Message send " + notification + " to " + Arrays.asList(browserIDs) + " for " + groupName);
        }

        public void registerProvider(String protocol, NotificationProvider provider) {
        }
        public void trace(String message)  {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, "NOOPOutOfBandNotifier discarding notification " + message);
            }
        }
    };
    private static final Comparator<Notification> ScheduledAtComparator = new Comparator<Notification>() {
        public int compare(Notification a, Notification b) {
            return (int) (a.configuration.getScheduledAt() - b.configuration.getScheduledAt());
        }
    };
    private final Notification NOOP = new Notification("---") {
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
    private final BlockingQueue<Notification> queue;
    private final long groupTimeout;
    private final long cloudPushIDTimeout;
    private final long pushIDTimeout;
    private final ServletContext context;

    private long lastTouchScan = System.currentTimeMillis();
    private long lastExpiryScan = System.currentTimeMillis();

    public LocalPushGroupManager(final ServletContext servletContext) {
        this.context = servletContext;
        Configuration configuration = new ServletContextConfiguration("org.icepush", servletContext);
        this.groupTimeout = configuration.getAttributeAsLong("groupTimeout", 2 * 60 * 1000);
        this.pushIDTimeout = configuration.getAttributeAsLong("pushIdTimeout", 2 * 60 * 1000);
        this.cloudPushIDTimeout = configuration.getAttributeAsLong("cloudPushIdTimeout", 30 * 60 * 1000);
        int notificationQueueSize = configuration.getAttributeAsInteger("notificationQueueSize", 1000);
        this.queue = new LinkedBlockingQueue<Notification>(notificationQueueSize);
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

    public OutOfBandNotifier getOutOfBandNotifier() {
        Object attribute = context.getAttribute(OutOfBandNotifier.class.getName());
        return attribute == null ? NOOPOutOfBandNotifier : (OutOfBandNotifier) attribute;
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

    public void park(final String pushId, final NotifyBackURI notifyBackURI) {
        parkedPushIDs.put(pushId, notifyBackURI);
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
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Push Notification request for Push Group '" + groupName + "'.");
        }
        if (!queue.offer(new Notification(groupName))) {
            // Leave at INFO
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(
                    Level.INFO,
                    "Push Notification request for Push Group '" + groupName + "' was dropped, " +
                        "queue maximum size reached.");
            }
        }
    }

    public void push(final String groupName, final PushConfiguration pushConfiguration) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                Level.FINE,
                "Push Notification request for Push Group '" + groupName + "' " +
                    "(Push configuration: '" + pushConfiguration + "').");
        }
        Notification notification;
        if (pushConfiguration.getAttributes().get("subject") != null) {
            notification = new OutOfBandNotification(groupName, pushConfiguration);
        } else {
            notification = new Notification(groupName, pushConfiguration);
        }
        //add this notification to a blocking queue
        queue.add(notification);
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
        clearPendingNotifications(new HashSet<String>(Arrays.asList(pushID)));
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
        for (final NotificationEntry notificationEntry : notificationSet) {
            PushID _pushID = getPushID(notificationEntry.getPushID());
            if (_pushID != null) {
                Browser _browser = getBrowser(_pushID.getBrowserID());
                if (_browser != null) {
                    _browser.startConfirmationTimeout(notificationEntry.getGroupName());
                }
            }
        }
    }

    public boolean startConfirmationTimeout(
        final String browserID, final String groupName) {

        return startConfirmationTimeout(browserID, groupName, getBrowser(browserID).getSequenceNumber());
    }

    public boolean startConfirmationTimeout(
        final String browserID, final String groupName, final long sequenceNumber) {

        Browser browser = getBrowser(browserID);
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
        return false;
    }

    public boolean startConfirmationTimeout(
        final String browserID, final String groupName, final long sequenceNumber, final long timeout) {

        Browser browser = getBrowser(browserID);
        NotifyBackURI notifyBackURI = browser.getNotifyBackURI();
        if (notifyBackURI != null &&
            notifyBackURI.getTimestamp() + browser.getMinCloudPushInterval() <= System.currentTimeMillis() + timeout &&
            browser.getPushConfiguration() != null) {

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

                    ((Timer)PushInternalContext.getInstance().getAttribute(Timer.class.getName() + "$confirmation")).
                        schedule(confirmationTimeout = newConfirmationTimeout(browserID, groupName, timeout), timeout);
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
        return false;
    }

    public boolean startExpiryTimeout(final String pushID) {
        PushID _pushID = getPushID(pushID);
        if (_pushID != null) {
            String browserID = getPushID(pushID).getBrowserID();
            return startExpiryTimeout(pushID, null, browserID != null ? getBrowser(browserID).getSequenceNumber() : -1);
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

        boolean _modified = false;
        PushID _pushID;
        if (pushIDMap.containsKey(pushID)) {
            _pushID = pushIDMap.get(pushID);
        } else {
            _pushID = newPushID(pushID);
            pushIDMap.put(pushID, _pushID);
            _modified = true;
        }
        _modified |= _pushID.addToGroup(groupName);
        _modified |= addToGroup(groupMap, groupName, pushID);
        memberAdded(groupName, pushID);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Added PushID '" + pushID + "' to Push Group '" + groupName + "'.");
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
        final Set<NotificationEntry> pendingNotifiedPushIDSet, final Set<String> pushIDSet) {

        Iterator<NotificationEntry> pendingNotifiedPushIDIterator =
            new HashSet<NotificationEntry>(pendingNotifiedPushIDSet).iterator();
        while (pendingNotifiedPushIDIterator.hasNext()) {
            NotificationEntry _pendingNotifiedPushID = pendingNotifiedPushIDIterator.next();
            if (pushIDSet.contains(_pendingNotifiedPushID.getPushID())) {
                pendingNotifiedPushIDSet.remove(_pendingNotifiedPushID);
            }
        }
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

    protected Lock getPendingNotifiedPushIDSetLock() {
        return pendingNotifiedPushIDSetLock;
    }

    protected long getPushIDTimeout() {
        return pushIDTimeout;
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

    protected PushID newPushID(final String id) {
        PushID pushID = new PushID(id, getPushIDTimeout(), getCloudPushIDTimeout());
        pushID.startExpiryTimeout();
        return pushID;
    }

    protected boolean removeBrowser(final Map<String, org.icepush.Browser> browserMap, final Browser browser) {
        return browserMap.remove(browser.getID()) != null;
    }

    protected boolean removeGroup(final Map<String, Group> groupMap, final String groupName) {
        return groupMap.remove(groupName) != null;
    }

    protected boolean removeMember(final Map<String, Group> groupMap, final String groupName, final String pushID) {
        boolean _modified = false;
        if (groupName != null && pushID != null) {
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

    private class Notification implements Runnable {
        private final PushConfiguration DEFAULT_CONFIGURATION = new PushConfiguration();

        protected final String groupName;
        protected final Set<String> exemptPushIDSet = new HashSet<String>();
        protected PushConfiguration configuration;

        public Notification(String groupName) {
            this.groupName = groupName;
            this.configuration = DEFAULT_CONFIGURATION;
        }

        public Notification(final String groupName, final PushConfiguration config) {
            this.groupName = groupName;
            this.configuration = config;
            Set<String> pushIDSet = (Set<String>)config.getAttributes().get("pushIDSet");
            if (pushIDSet != null) {
                this.exemptPushIDSet.addAll(pushIDSet);
            }
        }

        public void run() {
            try {
                Group group = getModifiableGroupMap().get(groupName);
                if (group != null) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, "Push Notification triggered for Push Group '" + groupName + "'.");
                    }
                    Set<String> pushIDSet = new HashSet<String>(Arrays.asList(group.getPushIDs()));
                    pushIDSet.removeAll(exemptPushIDSet);
                    Set<NotificationEntry> notificationSet = new HashSet<NotificationEntry>();
                    for (final String pushID : pushIDSet) {
                        notificationSet.add(new NotificationEntry(pushID, groupName));
                    }
                    getPendingNotifiedPushIDSetLock().lock();
                    try {
                        getModifiablePendingNotifiedPushIDSet().addAll(notificationSet);
                    } finally {
                        getPendingNotifiedPushIDSetLock().unlock();
                    }
                    startConfirmationTimeout(notificationSet);
                    outboundNotifier.broadcast(notificationSet, configuration.getDuration());
                    pushed(groupName);
                }
            } finally {
                scanForExpiry();
            }
        }

        public void coalesceWith(Notification nextNotification) {
            Group group = getModifiableGroupMap().get(groupName);
            if (group != null) {
                nextNotification.exemptPushIDSet.addAll(Arrays.asList(group.getPushIDs()));
            }
        }
    }

    private class OutOfBandNotification extends Notification {
        private final PushConfiguration config;

        public OutOfBandNotification(String groupName, PushConfiguration config) {
            super(groupName, config);
            this.config = config;
        }

        public void run() {
            Group group = getModifiableGroupMap().get(groupName);
            String[] pushIDs = STRINGS;
            if (group != null) {
                pushIDs = group.getPushIDs();
            }
            for (final String pushID : pushIDs) {
                Browser browser = getBrowser(getPushID(pushID).getBrowserID());
                if (browser != null) {
                    browser.setPushConfiguration(config);
                }
            }
            super.run();
        }
    }

    private class QueueConsumerTask extends TimerTask {
        private boolean running = true;

        public void run() {
            try {
                //take tasks from the queue and execute them serially
                while (running) {
                    try {
                        long currentTime = System.currentTimeMillis();
                        //block until notifications are available
                        Notification notification = queue.take();
                        //put back notification, need to extract the next scheduled notification
                        queue.offer(notification);
                        //search what notification needs to be fired now
                        TreeSet<Notification> notifications = new TreeSet(ScheduledAtComparator);
                        notifications.addAll(queue);
                        Notification scheduledNotification = notifications.first();
                        long scheduledAt = scheduledNotification.configuration.getScheduledAt();
                        if (scheduledAt < currentTime) {
                            //ready to send
                            queue.remove(scheduledNotification);

                            long duration = scheduledNotification.configuration.getDuration();
                            long endOfScheduledNotification = scheduledAt + duration;

                            for (Notification nextScheduledNotification: notifications) {
                                //skip first notification
                                if (nextScheduledNotification == scheduledNotification) continue;
                                //test if it overlaps
                                if (endOfScheduledNotification > nextScheduledNotification.configuration.getScheduledAt()) {
                                    //coalesce current notification with next overlapping notification
                                    scheduledNotification.coalesceWith(nextScheduledNotification);
                                } else {
                                    //stop when notification windows (durations) do not overlap anymore
                                    break;
                                }
                            }

                            scheduledNotification.run();
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
            queue.offer(NOOP);//add noop to unblock the queue
            return super.cancel();
        }
    }
}
