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

import static org.icesoft.util.StringUtilities.isNotNullAndIsNotEmpty;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
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
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.icepush.servlet.ServletContextConfiguration;
import org.icepush.util.DatabaseBackedConcurrentMap;
import org.icepush.util.DatabaseBackedQueue;
import org.icesoft.notify.cloud.core.CloudNotificationService;
import org.icesoft.util.servlet.ExtensionRegistry;

import org.mongodb.morphia.Datastore;

public class LocalPushGroupManager
extends AbstractPushGroupManager
implements InternalPushGroupManager, PushGroupManager {
    private static final Logger LOGGER = Logger.getLogger(LocalPushGroupManager.class.getName());

    private final ConcurrentMap<String, Browser> browserMap;
    private final ConcurrentMap<String, Group> groupMap;
    private final ConcurrentMap<String, PushID> pushIDMap;
    private final ConcurrentMap<String, ExpiryTimeout> expiryTimeoutMap;
    private final ConcurrentMap<String, ConfirmationTimeout> confirmationTimeoutMap;
    private final ConcurrentMap<String, NotifyBackURI> notifyBackURIMap;
    private final Set<NotificationEntry> pendingNotificationEntrySet;

    static final int DEFAULT_NOTIFICATIONQUEUE_SIZE = 1000;
    static final int DEFAULT_MIN_CLOUDPUSH_INTERVAL = 10 * 1000;
    static final int DEFAULT_CLOUDPUSHID_TIMEOUT = 30 * 60 * 1000;
    static final int DEFAULT_PUSHID_TIMEOUT = 2 * 60 * 1000;
    static final int DEFAULT_GROUP_TIMEOUT = 2 * 60 * 1000;
    private static final int GROUP_SCANNING_TIME_RESOLUTION = 3000; // ms
    private static final Comparator<Notification> ScheduledAtComparator = new Comparator<Notification>() {
        public int compare(final Notification notification1, final Notification notification2) {
            return (int) (notification1.getScheduledAt() - notification2.getScheduledAt());
        }
    };
    private final Map<String, BlockingConnectionServer> blockingConnectionServerMap =
        new ConcurrentHashMap<String, BlockingConnectionServer>();
    private final ConcurrentMap<String, String> parkedPushIDs = new ConcurrentHashMap<String, String>();

    /*
        There is no ConcurrentSet or ConcurrentHashSet.  As of JDK 1.6 there is a static method in the Collections class
        <E> Set<E> newSetFromMap(Map<e, Boolean> map) that can be used to create a Set backed by a ConcurrentMap.  But
        ICEpush needs to be JDK 1.5 compatible.  Therefor, a ReentrantLock is used for this Set.
     */
    private final ReentrantLock pendingNotifiedPushIDSetLock = new ReentrantLock();
    private final LocalNotificationBroadcaster outboundNotifier = new LocalNotificationBroadcaster();
    private final Timer timer = new Timer("Notification queue consumer.", true);
    private final TimerTask queueConsumer;
    private final Lock notificationQueueLock = new ReentrantLock();
    private final Condition notificationAvailableCondition = getNotificationQueueLock().newCondition();
    private final Queue<Notification> notificationQueue;
    private final long browserTimeout;
    private final long groupTimeout;
    private final long cloudPushIDTimeout;
    private final long pushIDTimeout;
    private final long minCloudPushInterval;
    private final ServletContext servletContext;

    private long lastTouchScan = System.currentTimeMillis();
    private long lastExpiryScan = System.currentTimeMillis();

    public LocalPushGroupManager(final ServletContext servletContext) {
        this.servletContext = servletContext;
        getServletContext().setAttribute(
            CloudNotificationService.class.getName(), new CloudNotificationService(getServletContext())
        );
        Configuration configuration = new ServletContextConfiguration("org.icepush", getServletContext());
        this.browserTimeout = configuration.getAttributeAsLong("browserTimeout", 10 * 60 * 1000);
        this.groupTimeout = configuration.getAttributeAsLong("groupTimeout", DEFAULT_GROUP_TIMEOUT);
        this.pushIDTimeout = configuration.getAttributeAsLong("pushIdTimeout", DEFAULT_PUSHID_TIMEOUT);
        this.cloudPushIDTimeout = configuration.getAttributeAsLong("cloudPushIdTimeout", DEFAULT_CLOUDPUSHID_TIMEOUT);
        this.minCloudPushInterval = configuration.getAttributeAsLong("minCloudPushInterval", DEFAULT_MIN_CLOUDPUSH_INTERVAL);
        PushInternalContext.getInstance().
            setAttribute(Timer.class.getName() + "$expiry", new Timer("Expiry Timeout timer", true));
        PushInternalContext.getInstance().
            setAttribute(Timer.class.getName() + "$confirmation", new Timer("Confirmation Timeout timer", true));
        // The Pending Notification Entry set must be initiated before the potential database-backed collections.
        this.pendingNotificationEntrySet = new HashSet<NotificationEntry>();
        Datastore datastore = (Datastore)PushInternalContext.getInstance().getAttribute(Datastore.class.getName());
        if (datastore != null) {
            this.browserMap =
                new DatabaseBackedConcurrentMap<Browser>(Browser.class, datastore);
            this.groupMap =
                new DatabaseBackedConcurrentMap<Group>(Group.class, datastore);
            this.pushIDMap =
                new DatabaseBackedConcurrentMap<PushID>(PushID.class, datastore);
            this.expiryTimeoutMap =
                new DatabaseBackedConcurrentMap<ExpiryTimeout>(ExpiryTimeout.class, datastore);
            this.confirmationTimeoutMap =
                new DatabaseBackedConcurrentMap<ConfirmationTimeout>(ConfirmationTimeout.class, datastore);
            this.notifyBackURIMap =
                new DatabaseBackedConcurrentMap<NotifyBackURI>(NotifyBackURI.class, datastore);
            this.notificationQueue =
                new DatabaseBackedQueue<Notification>(
                    configuration.getAttributeAsInteger("notificationQueueSize", DEFAULT_NOTIFICATIONQUEUE_SIZE),
                    Notification.class,
                    datastore
                );
//            this.pendingNotificationEntrySet =
//                new DatabaseBackedSetCollection<NotificationEntry>(NotificationEntry.class, datastore);
        } else {
            this.browserMap =
                new ConcurrentHashMap<String, Browser>();
            this.groupMap =
                new ConcurrentHashMap<String, Group>();
            this.pushIDMap =
                new ConcurrentHashMap<String, PushID>();
            this.expiryTimeoutMap =
                new ConcurrentHashMap<String, ExpiryTimeout>();
            this.confirmationTimeoutMap =
                new ConcurrentHashMap<String, ConfirmationTimeout>();
            this.notifyBackURIMap =
                new ConcurrentHashMap<String, NotifyBackURI>();
            this.notificationQueue =
                new LinkedBlockingQueue<Notification>(
                    configuration.getAttributeAsInteger("notificationQueueSize", DEFAULT_NOTIFICATIONQUEUE_SIZE)
                );
//            this.pendingNotificationEntrySet =
//                new HashSet<NotificationEntry>();
        }
        PushInternalContext.getInstance().setAttribute("browserMap", this.browserMap);
        PushInternalContext.getInstance().setAttribute("groupMap", this.groupMap);
        PushInternalContext.getInstance().setAttribute("pushIDMap", this.pushIDMap);
        PushInternalContext.getInstance().setAttribute("expiryTimeoutMap", this.expiryTimeoutMap);
        PushInternalContext.getInstance().setAttribute("confirmationTimeoutMap", this.confirmationTimeoutMap);
        PushInternalContext.getInstance().setAttribute("notifyBackURIMap", this.notifyBackURIMap);
        PushInternalContext.getInstance().setAttribute("notificationQueue", this.notificationQueue);
        if (datastore != null) {
            for (final String _browserID : this.browserMap.keySet()) {
                if (this.browserMap.get(_browserID).getLastAccessTimestamp() + getBrowserTimeout() <
                        System.currentTimeMillis()) {

                    this.browserMap.remove(_browserID);
                }
            }
            for (final String _pushID : this.expiryTimeoutMap.keySet()) {
                this.expiryTimeoutMap.get(_pushID).scheduleOrExecute(this);
            }
            for (final String _browserID : this.confirmationTimeoutMap.keySet()) {
                this.confirmationTimeoutMap.get(_browserID).scheduleExecuteOrCancel(this);
            }
            for (final Notification _notification : this.notificationQueue) {
                if (_notification instanceof NoopNotification) {
                    this.notificationQueue.remove(_notification);
                }
            }
        }
//        PushInternalContext.getInstance().setAttribute("pendingNotificationEntrySet", this.pendingNotificationEntrySet);
        this.queueConsumer = newQueueConsumerTask();
        this.timer.schedule(queueConsumer, 0);
    }
    
    public void addAllNotificationEntries(final Set<NotificationEntry> notificationEntrySet) {
        getPendingNotifiedPushIDSetLock().lock();
        try {
            getModifiablePendingNotificationEntrySet().addAll(notificationEntrySet);
        } finally {
            getPendingNotifiedPushIDSetLock().unlock();
        }
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

    public boolean addNotifyBackURI(final NotifyBackURI notifyBackURI) {
        return addNotifyBackURI(getModifiableNotifyBackURIMap(), notifyBackURI);
    }

    public void backOff(final String browserID, final long delay) {
        BlockingConnectionServer server = blockingConnectionServerMap.get(browserID);
        if (server != null) {
            server.backOff(delay);
        }
    }

    public void broadcastNotificationEntries(
        final Set<NotificationEntry> notificationEntrySet, final long duration, final String groupName) {

        outboundNotifier.broadcast(notificationEntrySet, duration);
        pushed(groupName);
    }

    public boolean cancelConfirmationTimeout(final String browserID) {
        ConfirmationTimeout _confirmationTimeout = getConfirmationTimeoutMap().remove(browserID);
        if (_confirmationTimeout != null) {
            _confirmationTimeout.cancel(this);
            return true;
        }
        return false;
    }

    public boolean cancelExpiryTimeout(final String pushID) {
        ExpiryTimeout _expiryTimeout = getExpiryTimeoutMap().remove(pushID);
        if (_expiryTimeout != null) {
            _expiryTimeout.cancel(this);
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
            clearPendingNotifications(getModifiablePendingNotificationEntrySet(), pushIDSet);
        } finally {
            getPendingNotifiedPushIDSetLock().unlock();
        }
    }

    public void clearPendingNotification(final String pushID) {
        getPendingNotifiedPushIDSetLock().lock();
        try {
            clearPendingNotifications(getModifiablePendingNotificationEntrySet(), pushID);
        } finally {
            getPendingNotifiedPushIDSetLock().unlock();
        }
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

    public CloudNotificationService getCloudNotificationService() {
        return (CloudNotificationService)getServletContext().getAttribute(CloudNotificationService.class.getName());
    }

    public ConfirmationTimeout getConfirmationTimeout(final String browserID) {
        if (browserID == null) {
            return null;
        }
        return getConfirmationTimeoutMap().get(browserID);
    }

    public ExpiryTimeout getExpiryTimeout(final String pushID) {
        if (pushID == null) {
            return null;
        }
        return getExpiryTimeoutMap().get(pushID);
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

    public synchronized static PushGroupManager getInstance(final ServletContext servletContext) {
        PushGroupManager _pushGroupManager =
            (PushGroupManager)servletContext.getAttribute(PushGroupManager.class.getName());
        if (_pushGroupManager == null)  {
            _pushGroupManager = new LocalPushGroupManager(servletContext);
            servletContext.setAttribute(PushGroupManager.class.getName(), _pushGroupManager);
        }
        return _pushGroupManager;
    }

    public NotifyBackURI getNotifyBackURI(final String notifyBackURI) {
        if (notifyBackURI == null) {
            return null;
        }
        return getNotifyBackURIMap().get(notifyBackURI);
    }

    public Map<String, NotifyBackURI> getNotifyBackURIMap() {
        return Collections.unmodifiableMap(getModifiableNotifyBackURIMap());
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
        return Collections.unmodifiableSet(getModifiablePendingNotificationEntrySet());
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

    public void park(final String pushID, final String notifyBackURI) {
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

    public void pruneParkedIDs(final String notifyBackURI, final Set<String> listenedPushIDSet) {
        for (final Map.Entry<String, String> parkedPushIDEntry : parkedPushIDs.entrySet()) {
            String parkedPushID = parkedPushIDEntry.getKey();
            if (parkedPushIDEntry.getValue().equals(notifyBackURI) &&
                !listenedPushIDSet.contains(parkedPushID)) {

                parkedPushIDs.remove(parkedPushID);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Removed unlistened parked Push-ID '" + parkedPushID + "' for " +
                            "NotifyBackURI '" + notifyBackURI + "'.");
                }
            }
        }
    }

    public void push(final String groupName) {
        push(groupName, (String) null);
    }

    public void push(final String groupName, final String payload) {
        push(getModifiableNotificationQueue(), groupName, payload);
    }

    public void push(final String groupName, final PushConfiguration pushConfiguration) {
        push(groupName, (String)null, pushConfiguration);
    }

    public void push(final String groupName, final String payload, final PushConfiguration pushConfiguration) {
        push(getModifiableNotificationQueue(), groupName, payload, pushConfiguration);
    }

    public void removeBlockingConnectionServer(final String browserID) {
        blockingConnectionServerMap.remove(browserID);
    }

    public boolean removeBrowser(final String browserID) {
        return removeBrowser(getModifiableBrowserMap(), browserID);
    }

    public boolean removeConfirmationTimeout(final ConfirmationTimeout confirmationTimeout) {
        return removeConfirmationTimeout(getModifiableConfirmationTimeoutMap(), confirmationTimeout);
    }

    public boolean removeExpiryTimeout(final ExpiryTimeout expiryTimeout) {
        return removeExpiryTimeout(getModifiableExpiryTimeoutMap(), expiryTimeout);
    }

    public boolean removeGroup(final String groupName) {
        return removeGroup(getModifiableGroupMap(), groupName);
    }

    public boolean removeMember(final String groupName, final String pushID) {
        return removeMember(getModifiableGroupMap(), getModifiablePushIDMap(), groupName, pushID);
    }

    public void removeNotificationReceiver(final NotificationBroadcaster.Receiver observer) {
        outboundNotifier.removeReceiver(observer);
    }

    public void removePendingNotification(final String pushID) {
        getPendingNotifiedPushIDSetLock().lock();
        try {
            clearPendingNotification(pushID);
        } finally {
            getPendingNotifiedPushIDSetLock().unlock();
        }
    }

    public void removePendingNotifications(final Set<String> pushIDSet) {
        getPendingNotifiedPushIDSetLock().lock();
        try {
            clearPendingNotifications(pushIDSet);
        } finally {
            getPendingNotifiedPushIDSetLock().unlock();
        }
    }

    public boolean removePushID(final String pushID) {
        return removePushID(getModifiablePushIDMap(), pushID);
    }

    public void scan(final Set<String> confirmedPushIDSet) {
        scan(getModifiableGroupMap(), confirmedPushIDSet);
    }

    public void scanForExpiry() {
        scanForExpiry(getModifiableGroupMap());
    }

    public void shutdown() {
        outboundNotifier.shutdown();
        ((Timer)PushInternalContext.getInstance().getAttribute(Timer.class.getName() + "$confirmation")).cancel();
        PushInternalContext.getInstance().removeAttribute(Timer.class.getName() + "$confirmation");
        ((Timer)PushInternalContext.getInstance().getAttribute(Timer.class.getName() + "$expiry")).cancel();
        PushInternalContext.getInstance().removeAttribute(Timer.class.getName() + "$expiry");
        queueConsumer.cancel();
        timer.cancel();
    }

    public boolean startConfirmationTimeout(
        final String browserID, final String groupName, final Map<String, String> propertyMap) {

        return
            startConfirmationTimeout(
                browserID, groupName, propertyMap, getBrowser(browserID).getSequenceNumber()
            );
    }

    public boolean startConfirmationTimeout(
        final String browserID, final String groupName, final Map<String, String> propertyMap,
        final long sequenceNumber) {

        Browser browser = getBrowser(browserID);
        if (browser.isCloudPushEnabled()) {
            NotifyBackURI notifyBackURI = getNotifyBackURI(browser.getNotifyBackURI());
            if (notifyBackURI != null) {
                long now = System.currentTimeMillis();
                long timeout = browser.getStatus().getConnectionRecreationTimeout() * 2;
                LOGGER.log(Level.FINE, "Calculated confirmation timeout: '" + timeout + "'");
                if (notifyBackURI.getTimestamp() + browser.getMinCloudPushInterval() <= now + timeout) {
                    return startConfirmationTimeout(browserID, groupName, propertyMap, sequenceNumber, timeout);
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
        final String browserID, final String groupName, final Map<String, String> propertyMap,
        final long sequenceNumber, final long timeout) {

        Browser browser = getBrowser(browserID);
        if (browser.isCloudPushEnabled()) {
            NotifyBackURI notifyBackURI = getNotifyBackURI(browser.getNotifyBackURI());
            if (notifyBackURI != null &&
                notifyBackURI.getTimestamp() + browser.getMinCloudPushInterval() <=
                    System.currentTimeMillis() + timeout &&
                isOutOfBandNotification(propertyMap)) {

                ConfirmationTimeout _confirmationTimeout = getConfirmationTimeoutMap().get(browserID);
                if (_confirmationTimeout == null) {
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
                        _confirmationTimeout =
                            newConfirmationTimeout(
                                browserID, groupName, propertyMap, timeout,
                                getBrowser(browserID).getMinCloudPushInterval()
                            );
                        _confirmationTimeout.schedule(System.currentTimeMillis() + timeout);
                        getConfirmationTimeoutMap().put(browserID, _confirmationTimeout);
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

    public void startConfirmationTimeouts(final Set<NotificationEntry> notificationSet) {
        for (final NotificationEntry _notificationEntry : notificationSet) {
            startConfirmationTimeout(_notificationEntry);
        }
    }

    public boolean startExpiryTimeout(final String pushID) {
        PushID _pushID = getPushID(pushID);
        if (_pushID != null) {
            String _browserID = _pushID.getBrowserID();
            return
                startExpiryTimeout(
                    pushID, (String)null, _browserID != null ? getBrowser(_browserID).getSequenceNumber() : -1
                );
        } else {
            return
                startExpiryTimeout(
                    pushID, (String)null, -1
                );
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
                _expiryTimeout.schedule(
                    System.currentTimeMillis() + (!_isCloudPushID ? pushIDTimeout : cloudPushIDTimeout)
                );
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
                pushID.startExpiryTimeout(browserID, browser.getSequenceNumber());
            }
        }
    }

    protected boolean addBrowser(final ConcurrentMap<String, Browser> browserMap, final Browser browser) {
        boolean _modified = false;
        if (!browserMap.containsKey(browser.getID())) {
            browserMap.put(browser.getID(), browser);
            _modified = true;
        }
        return _modified;
    }

    protected boolean addMember(
        final ConcurrentMap<String, Group> groupMap, final ConcurrentMap<String, PushID> pushIDMap,
        final String groupName, final String pushID) {

        return addMember(groupMap, pushIDMap, groupName, pushID, (PushConfiguration)null);
    }

    protected boolean addMember(
        final ConcurrentMap<String, Group> groupMap, final ConcurrentMap<String, PushID> pushIDMap,
        final String groupName, final String pushID, final PushConfiguration pushConfiguration) {

        boolean _modified = false;
        if (groupMap != null && pushIDMap != null &&
            isNotNullAndIsNotEmpty(groupName) && isNotNullAndIsNotEmpty(pushID)) {

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
                    "Added Push-ID '" + pushID + "' to Group '" + groupName + "' with " +
                        "Push Configuration '" + pushConfiguration + "'.");
            }
        }
        return _modified;
    }

    protected boolean addNotifyBackURI(
        final ConcurrentMap<String, NotifyBackURI> notifyBackURIMap, final NotifyBackURI notifyBackURI) {

        boolean _modified;
        if (!notifyBackURIMap.containsKey(notifyBackURI.getURI())) {
            notifyBackURIMap.put(notifyBackURI.getURI(), notifyBackURI);
            _modified = true;
        } else {
            _modified = false;
        }
        return _modified;
    }

    protected boolean addToGroup(
        final String groupName, final String pushID) {

        return addToGroup(getModifiableGroupMap(), groupName, pushID);
    }

    protected boolean addToGroup(
        final ConcurrentMap<String, Group> groupMap, final String groupName, final String pushID) {

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

    protected final long getBrowserTimeout() {
        return browserTimeout;
    }

    protected long getCloudPushIDTimeout() {
        return cloudPushIDTimeout;
    }

    protected Map<String, ConfirmationTimeout> getConfirmationTimeoutMap() {
        return
            new ConcurrentMap<String, ConfirmationTimeout>() {
                public void clear() {
                    getModifiableConfirmationTimeoutMap().clear();
                }

                public boolean containsKey(final Object objectKey) {
                    return getModifiableConfirmationTimeoutMap().containsKey(objectKey);
                }

                public boolean containsValue(final Object objectValue) {
                    return getModifiableConfirmationTimeoutMap().containsValue(objectValue);
                }

                public Set<Entry<String, ConfirmationTimeout>> entrySet()
                throws UnsupportedOperationException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean equals(final Object object) {
                    return getModifiableConfirmationTimeoutMap().equals(object);
                }

                public ConfirmationTimeout get(final Object objectKey) {
                    return getModifiableConfirmationTimeoutMap().get(objectKey);
                }

                @Override
                public int hashCode() {
                    return getModifiableConfirmationTimeoutMap().hashCode();
                }

                public boolean isEmpty() {
                    return getModifiableConfirmationTimeoutMap().isEmpty();
                }

                public Set<String> keySet() {
                    return getModifiableConfirmationTimeoutMap().keySet();
                }

                public ConfirmationTimeout put(final String key, final ConfirmationTimeout confirmationTimeout) {
                    return getModifiableConfirmationTimeoutMap().put(key, confirmationTimeout);
                }

                public void putAll(final Map<? extends String, ? extends ConfirmationTimeout> map)
                throws UnsupportedOperationException {
                    throw new UnsupportedOperationException();
                }

                public ConfirmationTimeout putIfAbsent(
                    final String key, final ConfirmationTimeout confirmationTimeout) {

                    return getModifiableConfirmationTimeoutMap().putIfAbsent(key, confirmationTimeout);
                }

                public ConfirmationTimeout remove(final Object objectKey) {
                    return getModifiableConfirmationTimeoutMap().remove(objectKey);
                }

                public boolean remove(final Object objectKey, final Object objectValue) {
                    return getModifiableConfirmationTimeoutMap().remove(objectKey, objectValue);
                }

                public ConfirmationTimeout replace(
                    final String key, final ConfirmationTimeout confirmationTimeout) {

                    return getModifiableConfirmationTimeoutMap().replace(key, confirmationTimeout);
                }

                public boolean replace(
                    final String key, final ConfirmationTimeout oldConfirmationTimeout,
                    final ConfirmationTimeout newConfirmationTimeout) {

                    return getModifiableConfirmationTimeoutMap().replace(key, oldConfirmationTimeout, newConfirmationTimeout);
                }

                public int size() {
                    return getModifiableConfirmationTimeoutMap().size();
                }

                public Collection<ConfirmationTimeout> values()
                throws UnsupportedOperationException {
                    throw new UnsupportedOperationException();
                }
            };
    }

    protected Map<String, ExpiryTimeout> getExpiryTimeoutMap() {
        return
            new ConcurrentMap<String, ExpiryTimeout>() {
                public void clear() {
                    getModifiableExpiryTimeoutMap().clear();
                }

                public boolean containsKey(final Object objectKey) {
                    return getModifiableExpiryTimeoutMap().containsKey(objectKey);
                }

                public boolean containsValue(final Object objectValue) {
                    return getModifiableExpiryTimeoutMap().containsValue(objectValue);
                }

                public Set<Entry<String, ExpiryTimeout>> entrySet()
                throws UnsupportedOperationException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean equals(final Object object) {
                    return getModifiableExpiryTimeoutMap().equals(object);
                }

                public ExpiryTimeout get(final Object objectKey) {
                    return getModifiableExpiryTimeoutMap().get(objectKey);
                }

                @Override
                public int hashCode() {
                    return getModifiableExpiryTimeoutMap().hashCode();
                }

                public boolean isEmpty() {
                    return getModifiableExpiryTimeoutMap().isEmpty();
                }

                public Set<String> keySet() {
                    return getModifiableExpiryTimeoutMap().keySet();
                }

                public ExpiryTimeout put(final String key, final ExpiryTimeout expiryTimeout) {
                    return getModifiableExpiryTimeoutMap().put(key, expiryTimeout);
                }

                public void putAll(final Map<? extends String, ? extends ExpiryTimeout> map)
                throws UnsupportedOperationException {
                    throw new UnsupportedOperationException();
                }

                public ExpiryTimeout putIfAbsent(final String key, final ExpiryTimeout expiryTimeout) {
                    return getModifiableExpiryTimeoutMap().putIfAbsent(key, expiryTimeout);
                }

                public ExpiryTimeout remove(final Object objectKey) {
                    return getModifiableExpiryTimeoutMap().remove(objectKey);
                }

                public boolean remove(final Object objectKey, final Object objectValue) {
                    return getModifiableExpiryTimeoutMap().remove(objectKey, objectValue);
                }

                public ExpiryTimeout replace(
                    final String key, final ExpiryTimeout expiryTimeout) {

                    return getModifiableExpiryTimeoutMap().replace(key, expiryTimeout);
                }

                public boolean replace(
                    final String key, final ExpiryTimeout oldExpiryTimeout, final ExpiryTimeout newExpiryTimeout) {

                    return getModifiableExpiryTimeoutMap().replace(key, oldExpiryTimeout, newExpiryTimeout);
                }

                public int size() {
                    return getModifiableExpiryTimeoutMap().size();
                }

                public Collection<ExpiryTimeout> values()
                throws UnsupportedOperationException {
                    throw new UnsupportedOperationException();
                }
            };
    }

    protected Group getGroup(final ConcurrentMap<String, Group> groupMap, final String groupName) {
        return groupMap.get(groupName);
    }

    protected Map<String, String[]> getGroupPushIDsMap(final ConcurrentMap<String, Group> groupMap) {
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
        return
            new ConcurrentMap<String, Browser>() {
                public void clear() {
                    browserMap.clear();
                }

                public boolean containsKey(final Object objectKey) {
                    return browserMap.containsKey(objectKey);
                }

                public boolean containsValue(final Object objectValue) {
                    return browserMap.containsValue(objectValue);
                }

                public Set<Entry<String, Browser>> entrySet()
                throws UnsupportedOperationException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean equals(final Object object) {
                    return browserMap.equals(object);
                }

                public Browser get(final Object objectKey) {
                    return browserMap.get(objectKey);
                }

                @Override
                public int hashCode() {
                    return browserMap.hashCode();
                }

                public boolean isEmpty() {
                    return browserMap.isEmpty();
                }

                public Set<String> keySet() {
                    return browserMap.keySet();
                }

                public Browser put(final String key, final Browser browser) {
                    return browserMap.put(key, browser);
                }

                public void putAll(final Map<? extends String, ? extends Browser> map)
                throws UnsupportedOperationException {
                    throw new UnsupportedOperationException();
                }

                public Browser putIfAbsent(final String key, final Browser browser) {
                    return browserMap.putIfAbsent(key, browser);
                }

                public Browser remove(final Object objectKey) {
                    return browserMap.remove(objectKey);
                }

                public boolean remove(final Object objectKey, final Object objectValue) {
                    return browserMap.remove(objectKey, objectValue);
                }

                public Browser replace(final String key, final Browser browser) {
                    return browserMap.replace(key, browser);
                }

                public boolean replace(final String key, final Browser oldBrowser, final Browser newBrowser) {
                    return browserMap.replace(key, oldBrowser, newBrowser);
                }

                public int size() {
                    return browserMap.size();
                }

                public Collection<Browser> values()
                throws UnsupportedOperationException {
                    throw new UnsupportedOperationException();
                }
            };
    }

    protected ConcurrentMap<String, ConfirmationTimeout> getModifiableConfirmationTimeoutMap() {
        return confirmationTimeoutMap;
    }

    protected ConcurrentMap<String, ExpiryTimeout> getModifiableExpiryTimeoutMap() {
        return expiryTimeoutMap;
    }

    protected ConcurrentMap<String, Group> getModifiableGroupMap() {
        return
            new ConcurrentMap<String, Group>() {
                public void clear() {
                    groupMap.clear();
                }

                public boolean containsKey(final Object objectKey) {
                    return groupMap.containsKey(objectKey);
                }

                public boolean containsValue(final Object objectValue) {
                    return groupMap.containsValue(objectValue);
                }

                public Set<Entry<String, Group>> entrySet()
                throws UnsupportedOperationException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean equals(final Object object) {
                    return groupMap.equals(object);
                }

                public Group get(final Object objectKey) {
                    return groupMap.get(objectKey);
                }

                @Override
                public int hashCode() {
                    return groupMap.hashCode();
                }

                public boolean isEmpty() {
                    return groupMap.isEmpty();
                }

                public Set<String> keySet() {
                    return groupMap.keySet();
                }

                public Group put(final String key, final Group group) {
                    return groupMap.put(key, group);
                }

                public void putAll(final Map<? extends String, ? extends Group> map)
                throws UnsupportedOperationException {
                    throw new UnsupportedOperationException();
                }

                public Group putIfAbsent(final String key, final Group group) {
                    return groupMap.putIfAbsent(key, group);
                }

                public Group remove(final Object objectKey) {
                    return groupMap.remove(objectKey);
                }

                public boolean remove(final Object objectKey, final Object objectValue) {
                    return groupMap.remove(objectKey, objectValue);
                }

                public Group replace(final String key, final Group group) {
                    return groupMap.replace(key, group);
                }

                public boolean replace(final String key, final Group oldGroup, final Group newGroup) {
                    return groupMap.replace(key, oldGroup, newGroup);
                }

                public int size() {
                    return groupMap.size();
                }

                public Collection<Group> values() {
                    return new Values(groupMap.values());
                }

                final class Values
                extends AbstractCollection<Group> {
                    private final Collection<? extends Group> values;

                    Values(final Collection<? extends Group> values) {
                        this.values = values;
                    }

                    @Override
                    public void clear() {
                        groupMap.clear();
                    }

                    @Override
                    public boolean contains(final Object object) {
                        return groupMap.containsValue(object);
                    }

                    @Override
                    public boolean isEmpty() {
                        return groupMap.isEmpty();
                    }

                    @Override
                    public Iterator<Group> iterator() {
                        return new ValueIterator(getValues().iterator());
                    }

                    @Override
                    public int size() {
                        return groupMap.size();
                    }

                    protected Collection<? extends Group> getValues() {
                        return values;
                    }

                    final class ValueIterator
                    implements Iterator<Group> {
                        private final Iterator<? extends Group> valueIterator;

                        ValueIterator(final Iterator<? extends Group> valueIterator) {
                            this.valueIterator = valueIterator;
                        }

                        public boolean hasNext() {
                            return getValueIterator().hasNext();
                        }

                        public Group next() {
                            return getValueIterator().next();
                        }

                        public void remove() {
                            getValueIterator().remove();
                        }

                        protected Iterator<? extends Group> getValueIterator() {
                            return valueIterator;
                        }
                    }
                }
            };
    }

    protected Queue<Notification> getModifiableNotificationQueue() {
        return notificationQueue;
    }

    protected ConcurrentMap<String, NotifyBackURI> getModifiableNotifyBackURIMap() {
        return notifyBackURIMap;
    }

    protected Set<NotificationEntry> getModifiablePendingNotificationEntrySet() {
        return pendingNotificationEntrySet;
    }

    protected ConcurrentMap<String, PushID> getModifiablePushIDMap() {
        return
            new ConcurrentMap<String, PushID>() {
                public void clear() {
                    pushIDMap.clear();
                }

                public boolean containsKey(final Object objectKey) {
                    return pushIDMap.containsKey(objectKey);
                }

                public boolean containsValue(final Object objectValue) {
                    return pushIDMap.containsValue(objectValue);
                }

                public Set<Entry<String, PushID>> entrySet()
                throws UnsupportedOperationException {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean equals(final Object object) {
                    return pushIDMap.equals(object);
                }

                public PushID get(final Object objectKey) {
                    return pushIDMap.get(objectKey);
                }

                @Override
                public int hashCode() {
                    return pushIDMap.hashCode();
                }

                public boolean isEmpty() {
                    return pushIDMap.isEmpty();
                }

                public Set<String> keySet() {
                    return pushIDMap.keySet();
                }

                public PushID put(final String key, final PushID pushID) {
                    return pushIDMap.put(key, pushID);
                }

                public void putAll(final Map<? extends String, ? extends PushID> map)
                throws UnsupportedOperationException {
                    throw new UnsupportedOperationException();
                }

                public PushID putIfAbsent(final String key, final PushID pushID) {
                    return pushIDMap.putIfAbsent(key, pushID);
                }

                public PushID remove(final Object objectKey) {
                    return pushIDMap.remove(objectKey);
                }

                public boolean remove(final Object objectKey, final Object objectValue) {
                    return pushIDMap.remove(objectKey, objectValue);
                }

                public PushID replace(final String key, final PushID pushID) {
                    return pushIDMap.replace(key, pushID);
                }

                public boolean replace(final String key, final PushID oldPushID, final PushID newPushID) {
                    return pushIDMap.replace(key, oldPushID, newPushID);
                }

                public int size() {
                    return pushIDMap.size();
                }

                public Collection<PushID> values()
                throws UnsupportedOperationException {
                    throw new UnsupportedOperationException();
                }
            };
    }

    protected Condition getNotificationAvailableCondition() {
        return notificationAvailableCondition;
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

    protected ServletContext getServletContext() {
        return servletContext;
    }

    protected boolean isOutOfBandNotification(final Map<String, String> propertyMap) {
        return propertyMap != null && propertyMap.containsKey("subject");
    }

    protected boolean isOutOfBandNotification(final PushConfiguration pushConfiguration) {
        return pushConfiguration != null && pushConfiguration.containsAttributeKey("subject");
    }

    protected Browser newBrowser(final String browserID, final long minCloudPushInterval) {
        return new Browser(browserID, minCloudPushInterval);
    }

    protected ConfirmationTimeout newConfirmationTimeout(
        final String browserID, final String groupName, final Map<String, String> propertyMap, final long timeout,
        final long minCloudPushInterval) {

        return new ConfirmationTimeout(browserID, groupName, propertyMap, timeout, minCloudPushInterval);
    }

    protected ExpiryTimeout newExpiryTimeout(final String pushID, final boolean isCloudPushID) {
        return new ExpiryTimeout(pushID, isCloudPushID);
    }

    protected Group newGroup(final String name) {
        return new Group(name, getGroupTimeout());
    }

    protected Notification newNotification(
        final String groupName, final String payload) {

        return new Notification(groupName, payload, newPushConfiguration());
    }

    protected Notification newNotification(
        final String groupName, final String payload, final PushConfiguration pushConfiguration) {

        return new Notification(groupName, payload, pushConfiguration);
    }

    protected OutOfBandNotification newOutOfBandNotification(
        final String groupName, final String payload, final PushConfiguration pushConfiguration) {

        return new OutOfBandNotification(groupName, payload, pushConfiguration);
    }

    protected PushConfiguration newPushConfiguration() {
        return new PushConfiguration();
    }

    protected PushID newPushID(final String id) {
        return new PushID(id, getPushIDTimeout(), getCloudPushIDTimeout());
    }

    protected QueueConsumerTask newQueueConsumerTask() {
        return new QueueConsumerTask();
    }

    protected void push(
        final Queue<Notification> notificationQueue, final String groupName, final String payload) {

        if (LOGGER.isLoggable(Level.FINE)) {
            if (isNotNullAndIsNotEmpty(payload)) {
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
            if (!notificationQueue.contains(_notification)) {
                if (notificationQueue.offer(_notification)) {
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
                        "Request for a Push Notification for Group '" + groupName + "' was ignored, " +
                            "duplicate detected."
                    );
                }
            }
        } finally {
            getNotificationQueueLock().unlock();
        }
    }

    protected void push(
        final Queue<Notification> notificationQueue, final String groupName, final String payload,
        final PushConfiguration pushConfiguration) {

        if (LOGGER.isLoggable(Level.FINE)) {
            if (isNotNullAndIsNotEmpty(payload)) {
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
            if (!notificationQueue.contains(_notification)) {
                //add this notification to a blocking queue
                notificationQueue.add(_notification);
                getNotificationAvailableCondition().signalAll();
            } else {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Request for a Push Notification for Group '" + groupName + "' was ignored, " +
                            "duplicate detected."
                    );
                }
            }
        } finally {
            getNotificationQueueLock().unlock();
        }
    }

    protected boolean removeBrowser(final ConcurrentMap<String, Browser> browserMap, final String browserID) {
        return browserMap.remove(browserID) != null;
    }

    protected boolean removeConfirmationTimeout(
        final ConcurrentMap<String, ConfirmationTimeout> confirmationTimeoutMap,
        final ConfirmationTimeout confirmationTimeout) {

        return confirmationTimeoutMap.remove(confirmationTimeout.getBrowserID()) != null;
    }

    protected boolean removeExpiryTimeout(
        final ConcurrentMap<String, ExpiryTimeout> expiryTimeoutMap,
        final ExpiryTimeout expiryTimeout) {

        return expiryTimeoutMap.remove(expiryTimeout.getPushID()) != null;
    }

    protected boolean removeGroup(final ConcurrentMap<String, Group> groupMap, final String groupName) {
        return groupMap.remove(groupName) != null;
    }

    protected boolean removeMember(
        final ConcurrentMap<String, Group> groupMap, final ConcurrentMap<String, PushID> pushIDMap,
        final String groupName, final String pushID) {

        boolean _modified = false;
        if (groupMap != null && pushIDMap != null &&
            isNotNullAndIsNotEmpty(groupName) && isNotNullAndIsNotEmpty(pushID)) {

            Group group = groupMap.get(groupName);
            if (group != null) {
                _modified = group.removePushID(pushID);
                PushID id = pushIDMap.get(pushID);
                if (id != null) {
                    _modified |= id.removeFromGroup(groupName);
                }
                memberRemoved(groupName, pushID);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Removed Push-ID '" + pushID + "' from Group '" + groupName + "'.");
                }
            }
        }
        return _modified;
    }

    protected boolean removePushID(final ConcurrentMap<String, PushID> pushIDMap, final String pushID) {
        return pushIDMap.remove(pushID) != null;
    }

    protected void scan(final ConcurrentMap<String, Group> groupMap, final Set<String> confirmedPushIDSet) {
        Set<String> pushIDSet = new HashSet<String>(confirmedPushIDSet);
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

    protected void scanForExpiry(final ConcurrentMap<String, Group> groupMap) {
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
                _browser.startConfirmationTimeout(
                    notificationEntry.getGroupName(), notificationEntry.getPropertyMap()
                );
            }
        }
    }

    public static class ExtensionRegistration
    implements ServletContextListener {
        private static final Logger LOGGER = Logger.getLogger(ExtensionRegistration.class.getName());

        public void contextDestroyed(final ServletContextEvent event) {
            // Do nothing.
        }

        public void contextInitialized(final ServletContextEvent event) {
            ExtensionRegistry.registerExtension(
                PushGroupManager.class.getName(), LocalPushGroupManager.class, 1, event.getServletContext()
            );
        }
    }

    protected class QueueConsumerTask
    extends TimerTask
    implements Runnable {
        private boolean running = true;

        public QueueConsumerTask() {
            // Do nothing.
        }

        public void run() {
            try {
                //take tasks from the queue and execute them serially
                while (running) {
                    takeAndExecute();
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
                // Offering noop to unblock the queue
                getModifiableNotificationQueue().offer(new NoopNotification("---", null, newPushConfiguration()));
                getNotificationAvailableCondition().signalAll();
                return super.cancel();
            } finally {
                getNotificationQueueLock().unlock();
            }
        }

        protected void takeAndExecute() {
            try {
                long _currentTime = System.currentTimeMillis();
                Notification _scheduledNotification;
                getNotificationQueueLock().lock();
                try {
                    if (getModifiableNotificationQueue().isEmpty()) {
                        try {
                            // Await until signalled when notifications are available.
                            getNotificationAvailableCondition().await();
                        } catch (final InterruptedException exception) {
                            LOGGER.log(Level.FINE, "Notification queue draining interrupted.");
                        }
                    }
                    TreeSet<Notification> _notificationTreeSet = new TreeSet<Notification>(ScheduledAtComparator);
                    _notificationTreeSet.addAll(getModifiableNotificationQueue());
                    _scheduledNotification = _notificationTreeSet.first();
                    long _scheduledAt = _scheduledNotification.getScheduledAt();
                    if (_scheduledAt <= _currentTime) {
                        //ready to send
                        getModifiableNotificationQueue().remove(_scheduledNotification);
                        long _duration = _scheduledNotification.getDuration();
                        long _endOfScheduledNotification = _scheduledAt + _duration;

                        for (final Notification _nextScheduledNotification : _notificationTreeSet) {
                            //skip first notification
                            if (_nextScheduledNotification == _scheduledNotification) {
                                continue;
                            }
                            //test if it overlaps
                            if (_endOfScheduledNotification >
                                    _nextScheduledNotification.getScheduledAt()) {

                                //coalesce current notification with next overlapping notification
                                _scheduledNotification.coalesceWith(_nextScheduledNotification);
                            } else {
                                //stop when notification windows (durations) do not overlap anymore
                                break;
                            }
                        }
                    } else {
                        _scheduledNotification = null;
                    }
                } finally {
                    getNotificationQueueLock().unlock();
                }
                if (_scheduledNotification != null) {
                    _scheduledNotification.run();
                }
            } catch (final NoClassDefFoundError exception) {
                //ignore the application WAR was removed from the file system
            } catch (final Throwable throwable)  {
                LOGGER.log(Level.WARNING, "Notification queue encountered ", throwable);
            }
        }
    }
}
