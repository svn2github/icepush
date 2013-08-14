/*
 * Copyright 2004-2013 ICEsoft Technologies Canada Corp.
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
 *
 */
package org.icepush;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.icepush.servlet.ServletContextConfiguration;

public class LocalPushGroupManager extends AbstractPushGroupManager implements PushGroupManager {
    private static final Logger LOGGER = Logger.getLogger(LocalPushGroupManager.class.getName());
    private static final String[] STRINGS = new String[0];
    private static final int GROUP_SCANNING_TIME_RESOLUTION = 3000; // ms
    private static final OutOfBandNotifier NOOPOutOfBandNotifier = new OutOfBandNotifier() {
        public void broadcast(PushNotification notification, String[] uris) {
            System.out.println("message send " + notification + " to " + Arrays.asList(uris));
        }

        public void registerProvider(String protocol, NotificationProvider provider) {
        }
        public void trace(String message)  {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, "NOOPOutOfBandNotifier discarding notification " + message);
            }
        }
    };
    private static final Runnable NOOP = new Runnable() {
        public void run() {
        }
    };
    private final Map<String, BlockingConnectionServer> blockingConnectionServerMap =
        new ConcurrentHashMap<String, BlockingConnectionServer>();
    protected final Map<String, Browser> browserMap = new ConcurrentHashMap<String, Browser>();
    protected final ConcurrentMap<String, PushID> pushIDMap = new ConcurrentHashMap<String, PushID>();
    protected final ConcurrentMap<String, Group> groupMap = new ConcurrentHashMap<String, Group>();
    private final ConcurrentMap<String, NotifyBackURI> parkedPushIDs = new ConcurrentHashMap<String, NotifyBackURI>();
    private final HashSet<String> pendingNotifications = new HashSet();
    private final NotificationBroadcaster outboundNotifier = new LocalNotificationBroadcaster();
    private final Timer timer = new Timer("Notification queue consumer.", true);
    private final TimerTask queueConsumer;
    private final BlockingQueue<Runnable> queue;
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
        this.queue = new LinkedBlockingQueue<Runnable>(notificationQueueSize);
        this.queueConsumer = new QueueConsumerTask();
        this.timer.schedule(queueConsumer, 0);
    }

    public void scan(final String[] confirmedPushIDs) {
        Set<String> pushIDs = new HashSet<String>();
        long now = System.currentTimeMillis();
        //accumulate pushIDs
        pushIDs.addAll(Arrays.asList(confirmedPushIDs));
        //avoid to scan/touch the groups on each notification
        if (lastTouchScan + GROUP_SCANNING_TIME_RESOLUTION < now) {
            try {
                for (Group group : groupMap.values()) {
                    group.touchIfMatching(pushIDs);
                    group.discardIfExpired();
                }
            } finally {
                lastTouchScan = now;
                lastExpiryScan = now;
            }
        }
    }

    public void addBlockingConnectionServer(final String browserID, final BlockingConnectionServer server) {
        blockingConnectionServerMap.put(browserID, server);
    }

    public void addBrowser(final Browser browser) {
        browserMap.put(browser.getID(), browser);
    }

    public void addMember(final String groupName, final String id) {
        PushID pushID = pushIDMap.get(id);
        if (pushID == null) {
            pushIDMap.put(id, newPushID(id, groupName));
        } else {
            pushID.addToGroup(groupName);
        }
        Group group = groupMap.get(groupName);
        if (group == null) {
            groupMap.put(groupName, new Group(groupName, id, groupTimeout, this));
        } else {
            group.addPushID(id);
        }
        memberAdded(groupName, id);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Added PushID '" + id + "' to Push Group '" + groupName + "'.");
        }
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

    public void clearPendingNotifications(final Set<String> pushIDSet) {
        pendingNotifications.removeAll(pushIDSet);
    }

    public void deleteNotificationReceiver(final NotificationBroadcaster.Receiver observer) {
        outboundNotifier.deleteReceiver(observer);
    }

    public Browser getBrowser(final String browserID) {
        return browserMap.get(browserID);
    }

    public Map<String, String[]> getGroupMap() {
        Map<String, String[]> groupMap = new HashMap<String, String[]>();
        for (Group group : new ArrayList<Group>(this.groupMap.values())) {
            groupMap.put(group.getName(), group.getPushIDs());
        }
        return groupMap;
    }

    public String[] getPendingNotifications() {
        return pendingNotifications.toArray(STRINGS);
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

    public void removeBrowser(final Browser browser) {
        browserMap.remove(browser.getID());
    }

    public void removeMember(final String groupName, final String pushId) {
        Group group = groupMap.get(groupName);
        if (group != null) {
            group.removePushID(pushId);
            PushID id = pushIDMap.get(pushId);
            if (id != null) {
                id.removeFromGroup(groupName);
            }
            memberRemoved(groupName, pushId);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Removed PushID '" + pushId + "' from Push Group '" + groupName + "'.");
            }
        }
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

    public void shutdown() {
        queueConsumer.cancel();
        timer.cancel();
    }

    public void cancelExpiryTimeout(final Browser browser) {
        for (final String pushIDString : browser.getPushIDSet()) {
            PushID pushID = pushIDMap.get(pushIDString);
            if (pushID != null) {
                pushID.cancelExpiryTimeout();
            }
        }
    }

    public void startConfirmationTimeout(final Set<String> pushIDSet) {
        for (final String pushIDString : pushIDSet) {
            PushID _pushID = pushIDMap.get(pushIDString);
            if (_pushID != null) {
                Browser _browser = _pushID.getBrowser();
                if (_browser != null) {
                    _browser.startConfirmationTimeout();
                }
            }
        }
    }

    public void startExpiryTimeout(final Browser browser) {
        for (final String pushIDString : browser.getPushIDSet()) {
            PushID pushID = pushIDMap.get(pushIDString);
            if (pushID != null) {
                pushID.startExpiryTimeout(browser, pushID.getBrowser().getSequenceNumber());
            }
        }
    }

    public Map<String, Browser> getBrowserMap() {
        return Collections.unmodifiableMap(browserMap);
    }

    public Map<String, PushID> getPushIDMap() {
        return Collections.unmodifiableMap(pushIDMap);
    }

    private void scanForExpiry() {
        long now = System.currentTimeMillis();
        //avoid to scan/touch the groups on each notification
        if (lastExpiryScan + GROUP_SCANNING_TIME_RESOLUTION < now) {
            try {
                for (Group group : groupMap.values()) {
                    group.discardIfExpired();
                }
            } finally {
                lastExpiryScan = now;
            }
        }
    }

    public OutOfBandNotifier getOutOfBandNotifier() {
        Object attribute = context.getAttribute(OutOfBandNotifier.class.getName());
        return attribute == null ? NOOPOutOfBandNotifier : (OutOfBandNotifier) attribute;
    }

    public PushID getPushID(final String pushID) {
        return pushIDMap.get(pushID);
    }

    protected long getCloudPushIDTimeout() {
        return cloudPushIDTimeout;
    }

    protected long getPushIDTimeout() {
        return pushIDTimeout;
    }

    protected PushID newPushID(final String id, final String groupName) {
        PushID pushID = new PushID(id, groupName, getPushIDTimeout(), getCloudPushIDTimeout(), this);
        pushID.startExpiryTimeout();
        return pushID;
    }

    Group getGroup(final String groupName) {
        return groupMap.get(groupName);
    }

    void groupTouched(final String groupName, final long timestamp) {
        super.groupTouched(groupName, timestamp);
    }

    boolean isParked(final String pushID) {
        return parkedPushIDs.containsKey(pushID);
    }

    void removeGroup(final String groupName) {
        groupMap.remove(groupName);
    }

    void removePendingNotification(final String pushID) {
        pendingNotifications.remove(pushID);
    }

    void removePendingNotifications(final Set<String> pushIDList) {
        pendingNotifications.removeAll(pushIDList);
    }

    void removePushID(final String pushID) {
        pushIDMap.remove(pushID);
    }

    private class Notification implements Runnable {
        protected final String groupName;
        protected final Set<String> exemptPushIDSet = new HashSet<String>();

        public Notification(String groupName) {
            this.groupName = groupName;
        }

        public Notification(final String groupName, final PushConfiguration config) {
            this.groupName = groupName;
            Set pushIDSet = (Set)config.getAttributes().get("pushIDSet");
            if (pushIDSet != null) {
                this.exemptPushIDSet.addAll(pushIDSet);
            }
        }

        public void run() {
            try {
                Group group = groupMap.get(groupName);
                if (group != null) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, "Push Notification triggered for Push Group '" + groupName + "'.");
                    }
                    Set<String> pushIDSet = new HashSet<String>(Arrays.asList(group.getPushIDs()));
                    pushIDSet.removeAll(exemptPushIDSet);
                    pendingNotifications.addAll(pushIDSet);
                    startConfirmationTimeout(pushIDSet);
                    outboundNotifier.broadcast(pushIDSet.toArray(new String[pushIDSet.size()]));
                    pushed(groupName);
                }
            } finally {
                scanForExpiry();
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
            Group group = groupMap.get(groupName);
            String[] pushIDs = STRINGS;
            if (group != null) {
                pushIDs = group.getPushIDs();
            }
            for (final String pushID : pushIDs) {
                pushIDMap.get(pushID).getBrowser().setPushConfiguration(config);
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
                        queue.take().run();
                    } catch (InterruptedException e) {
                        LOGGER.log(Level.FINE, "Notification queue draining interrupted.");
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
