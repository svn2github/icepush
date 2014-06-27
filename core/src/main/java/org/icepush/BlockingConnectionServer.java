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

import static org.icepush.NotificationEvent.NotificationType;
import static org.icepush.NotificationEvent.TargetType;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.http.PushRequest;
import org.icepush.http.PushResponse;
import org.icepush.http.PushResponseHandler;
import org.icepush.http.PushServer;
import org.icepush.http.standard.PushResponseHandlerServer;
import org.icepush.util.Slot;

public class BlockingConnectionServer
extends TimerTask
implements NotificationBroadcaster.Receiver, PushServer {
    private static final Logger LOGGER = Logger.getLogger(BlockingConnectionServer.class.getName());
    private static final String[] STRINGS = new String[0];
    //Define here to avoid classloading problems after application exit
    private static final PushResponseHandler NOOP_SHUTDOWN = new Noop("shutdown");
    private static final PushResponseHandler NOOP_TIMEOUT = new Noop("response timeout");
    private final PushResponseHandler closeConnectionDuplicate =
        new ConnectionClose("duplicate") {
            @Override
            public void respond(final PushResponse pushResponse)
            throws Exception {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Received duplicate listen.icepush request for Browser-ID '" + getBrowserID() + "'.");
                }
                super.respond(pushResponse);
                pushGroupManager.getBrowser(getBrowserID()).getStatus().revertConnectionRecreationTimeout();
            }
        };
    private final PushResponseHandler closeConnectionShutdown = new ConnectionClose("shutdown");
    private final PushServer AfterShutdown = new PushResponseHandlerServer(closeConnectionShutdown);

    private final BlockingQueue<PushRequest> pendingRequest = new LinkedBlockingQueue<PushRequest>(1);
    private final Slot heartbeatInterval;
    // This is either a LocalPushGroupManager or a DynamicPushGroupManager
    private final PushGroupManager pushGroupManager =
        (PushGroupManager)PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName());
    private final long minCloudPushInterval;
    private final long maxHeartbeatInterval;
    private final long minHeartbeatInterval;

    private final Set<NotificationListener>listenerSet = new CopyOnWriteArraySet<NotificationListener>();

    private String browserID;
    private long responseTimeoutTime;
    private PushServer activeServer;
    private Timer monitoringScheduler;

    private String lastWindow = "";
    private long defaultConnectionRecreationTimeout;
    private long responseTimestamp = System.currentTimeMillis();
    private long requestTimestamp = System.currentTimeMillis();
    private long backOffDelay = 0;

    private boolean setUp = false;

    public BlockingConnectionServer(
        final String browserID, final Timer monitoringScheduler, final Slot heartbeat,
        final boolean terminateBlockingConnectionOnShutdown, final Configuration configuration) {

        this.minCloudPushInterval = configuration.getAttributeAsLong("minCloudPushInterval", 10 * 1000);
        this.browserID = browserID;
        this.monitoringScheduler = monitoringScheduler;
        this.heartbeatInterval = heartbeat;
        this.defaultConnectionRecreationTimeout = configuration.getAttributeAsLong("connectionRecreationTimeout", 5000);
        this.pushGroupManager.addNotificationReceiver(this);
        this.maxHeartbeatInterval =
            configuration.getAttributeAsLong("maxHeartbeatInterval", Math.round(3 * heartbeat.getLongValue()));
        this.minHeartbeatInterval =
            configuration.getAttributeAsLong("minHeartbeatInterval", heartbeat.getLongValue() / 3);
        //define blocking server
        this.activeServer = new RunningServer(terminateBlockingConnectionOnShutdown);
    }

    public void addNotificationListener(final NotificationListener listener) {
        listenerSet.add(listener);
    }

    public synchronized void backOff(final long delay)
    throws IllegalStateException {
        checkSetUp();
        if (delay > 0) {
            backOffDelay = delay;
            respondIfBackOffRequested();
        }
    }

    public String getBrowserID() {
        return browserID;
    }

    public boolean isInterested(final Set<NotificationEntry> notificationEntrySet) {
        for (final NotificationEntry _notificationEntry : notificationEntrySet) {
            if (getPushGroupManager().
                    getBrowser(getBrowserID()).getPushIDSet().contains(_notificationEntry.getPushID())) {

                return true;
            }
        }
        return false;
    }

    public void receive(final Set<NotificationEntry> notificationSet)
    throws IllegalStateException {
        checkSetUp();
        sendNotifications(notificationSet);
    }

    public void removeNotificationListener(final NotificationListener listener) {
        listenerSet.remove(listener);
    }

    public void run()
    throws IllegalStateException {
        checkSetUp();
        try {
            if (System.currentTimeMillis() > responseTimeoutTime) {
                respondIfPendingRequest(NOOP_TIMEOUT);
            }
        } catch (Exception exception) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(
                    Level.WARNING, "Exception caught on " + this.getClass().getName() + " TimerTask.", exception);
            }
        }
    }

    public void service(final PushRequest pushRequest)
    throws Exception, IllegalStateException {
        checkSetUp();
        activeServer.service(pushRequest);
    }

    public void setUp() {
        pushGroupManager.addBrowser(newBrowser(getBrowserID(), getMinCloudPushInterval()));
        pushGroupManager.addBlockingConnectionServer(getBrowserID(), this);
        setUp = true;
        //add monitor
        monitoringScheduler.scheduleAtFixedRate(this, 0, 1000);
    }

    public void shutdown()
    throws IllegalStateException {
        checkSetUp();
        cancel();
        pushGroupManager.deleteNotificationReceiver(this);
        pushGroupManager.removeBlockingConnectionServer(getBrowserID());
        pushGroupManager.removeBrowser(pushGroupManager.getBrowser(getBrowserID()));
        activeServer.shutdown();
    }

    protected void checkSetUp()
    throws IllegalStateException {
        if (!setUp) {
            throw new IllegalStateException("Blocking Connection Server has not been set-up.");
        }
    }

    protected long getMinCloudPushInterval() {
        return minCloudPushInterval;
    }

    protected PushGroupManager getPushGroupManager() {
        return pushGroupManager;
    }

    protected Browser newBrowser(final String browserID, final long minCloudPushInterval) {
        return new Browser(browserID, minCloudPushInterval);
    }

    protected void notificationSent(final NotificationEvent event) {
        for (final NotificationListener listener : listenerSet) {
            listener.notificationSent(event);
        }
    }

    private void adjustConnectionRecreationTimeout(final PushRequest pushRequest) {
        Browser browser = pushGroupManager.getBrowser(getBrowserID());
        for (final String pushIDString : browser.getPushIDSet()) {
            PushID pushID = pushGroupManager.getPushID(pushIDString);
            if (pushID != null) {
                if (browser.getStatus().getConnectionRecreationTimeout() == -1) {
                    browser.getStatus().setConnectionRecreationTimeout(defaultConnectionRecreationTimeout);
                }
                browser.getStatus().backUpConnectionRecreationTimeout();
            }
        }
        long now = System.currentTimeMillis();
        long elapsed = now - requestTimestamp;
        requestTimestamp = now;
        long currentResponseDelay = requestTimestamp - responseTimestamp;
        //adaptive timeout -- see algorithm described in PUSH-164
        long responseDelay = currentResponseDelay;
        for (final String pushIDString : browser.getPushIDSet()) {
            PushID pushID = pushGroupManager.getPushID(pushIDString);
            if (pushID != null) {
                responseDelay = Math.max(responseDelay, (browser.getStatus().getConnectionRecreationTimeout() * 4) / 5);
                responseDelay = Math.min(responseDelay, (browser.getStatus().getConnectionRecreationTimeout() * 3) / 2);
                responseDelay = Math.max(responseDelay, 500);
                browser.getStatus().setConnectionRecreationTimeout(
                    (responseDelay + (browser.getStatus().getConnectionRecreationTimeout() * 4)) / 5);
            }
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            setNotifyBackURI(pushRequest);
            LOGGER.log(
                Level.FINE,
                "ICEpush metric:" +
                    " IP: " + pushRequest.getRemoteAddr() +
                    " pushIds: " + browser.getPushIDSet() +
                    " Cloud Push ID: " + browser.getNotifyBackURI() +
                    " Browser: " + browser.getID() +
                    " last request: " + elapsed +
                    " Latency: " + currentResponseDelay);
        }
    }

    private void recordResponseTime() {
        responseTimestamp = System.currentTimeMillis();
    }

    private void resendLastNotifications() {
        sendNotifications(pushGroupManager.getBrowser(getBrowserID()).getLastNotifiedPushIDSet());
    }

    private void resetTimeout(final PushRequest pushRequest) {
        long clientSideHeartbeatInterval;
        if (pushRequest != null) {
            try {
                clientSideHeartbeatInterval = pushRequest.getHeartbeatInterval();
            } catch (final NumberFormatException exception) {
                clientSideHeartbeatInterval = Long.MAX_VALUE;
            }
        } else {
            clientSideHeartbeatInterval = Long.MAX_VALUE;
        }
        long serverSideHeartbeatInterval = heartbeatInterval.getLongValue();
        long heartbeatInterval =
            Math.min(
                Math.max(
                    Math.min(clientSideHeartbeatInterval, serverSideHeartbeatInterval),
                    minHeartbeatInterval),
                maxHeartbeatInterval);
        responseTimeoutTime = System.currentTimeMillis() +heartbeatInterval;
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE,
                "Heartbeat Interval: " +
                    "client-side '" + clientSideHeartbeatInterval + "', " +
                    "server-side '" + serverSideHeartbeatInterval + "', " +
                    "used '" + heartbeatInterval + "'.");
        }
    }

    private synchronized boolean respondIfBackOffRequested() {
        boolean _result = false;
        if (backOffDelay > 0) {
            _result = respondIfPendingRequest(new BackOff(backOffDelay));
            if (_result) {
                backOffDelay = 0;
            }
        }
        return _result;
    }

    private synchronized void respondIfNotificationsAvailable() {
        if (pushGroupManager.getBrowser(getBrowserID()).hasNotifiedPushIDs()) {
            //save notifications, maybe they will need to be resent when blocking connection switches to another window
            pushGroupManager.getBrowser(getBrowserID()).
                setLastNotifiedPushIDSet(pushGroupManager.getBrowser(getBrowserID()).getNotifiedPushIDSet());
            respondIfPendingRequest(
                new NotifiedPushIDs(pushGroupManager.getBrowser(getBrowserID()).getLastNotifiedPushIDSet()) {
                    @Override
                    public void writeTo(final Writer writer)
                    throws IOException {
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.log(
                                Level.FINE,
                                "Send Notifications for Browser-ID '" + getBrowserID() + "' " +
                                    "with Push-IDs '" + getPushIDSet() + "'.");
                        }
                        super.writeTo(writer);
                        pushGroupManager.
                            clearPendingNotifications(
                                pushGroupManager.getBrowser(getBrowserID()).getPushIDSet());
                        pushGroupManager.getBrowser(getBrowserID()).
                            removeNotifiedPushIDs(
                                pushGroupManager.getBrowser(getBrowserID()).getLastNotifiedPushIDSet());
                        Set<String> groupNameSet = new HashSet<String>();
                        for (final NotificationEntry notificationEntry :
                                pushGroupManager.getBrowser(getBrowserID()).getLastNotifiedPushIDSet()) {

                            String groupName = notificationEntry.getGroupName();
                            if (groupNameSet.add(groupName)) {
                                notificationSent(
                                    new NotificationEvent(
                                        TargetType.BROWSER_ID, getBrowserID(), groupName,
                                        NotificationEvent.NotificationType.PUSH, this));
                            }
                            notificationSent(
                                new NotificationEvent(
                                    TargetType.PUSH_ID, notificationEntry.getPushID(), groupName, NotificationType.PUSH,
                                    this));
                        }
                    }
                });
        }
    }

    private boolean respondIfPendingRequest(final PushResponseHandler handler) {
        PushRequest previousRequest = pendingRequest.poll();
        if (previousRequest != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Pending request for PushIDs '" + pushGroupManager.getBrowser(getBrowserID()).getPushIDSet() + "', " +
                        "trying to respond.");
            }
            try {
                recordResponseTime();
                previousRequest.respondWith(handler);
                return true;
            } catch (IOException e) {
                LOGGER.fine("Possible communication issue encountered while responding: " + e.getMessage());
                return true;
            } catch (Exception e) {
                LOGGER.severe("Failed to respond to pending request: " + e.getMessage());
                return true;
            }
        }
        return false;
    }

    private synchronized boolean sendNotifications(final Set<NotificationEntry> notificationSet) {
        //stop sending notifications if pushID are not used anymore by the browser
        Set<NotificationEntry> matchingSet = new HashSet<NotificationEntry>();
        Iterator<NotificationEntry> notificationEntryIterator = notificationSet.iterator();
        while (notificationEntryIterator.hasNext()) {
            NotificationEntry notificationEntry = notificationEntryIterator.next();
            if (pushGroupManager.getBrowser(getBrowserID()).getPushIDSet().contains(notificationEntry.getPushID())) {
                matchingSet.add(notificationEntry);
            }
        }
        boolean anyNotifications = !matchingSet.isEmpty();
        if (anyNotifications) {
            pushGroupManager.getBrowser(getBrowserID()).
                addNotifiedPushIDs(matchingSet);
            pushGroupManager.getBrowser(getBrowserID()).
                retainNotifiedPushIDs(pushGroupManager.getPendingNotificationSet());
            resetTimeout(pendingRequest.peek());
            respondIfNotificationsAvailable();
        }
        return anyNotifications;
    }

    private void setNotifyBackURI(final PushRequest pushRequest) {
        String notifyBack = pushRequest.getNotifyBackURI();
        if (notifyBack != null && notifyBack.trim().length() != 0) {
            pushGroupManager.getBrowser(getBrowserID()).
                setNotifyBackURI(pushGroupManager.newNotifyBackURI(notifyBack), true);
        }
    }

    private class RunningServer
    implements PushServer {
        private final boolean terminateBlockingConnectionOnShutdown;

        public RunningServer(final boolean terminateBlockingConnectionOnShutdown) {
            this.terminateBlockingConnectionOnShutdown = terminateBlockingConnectionOnShutdown;
        }

        public void service(final PushRequest pushRequest) throws Exception {
            resetTimeout(pushRequest);
            try {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Received listen.icepush request from Browser-ID '" + pushRequest.getBrowserID() + "' " +
                            "for Push-IDs '" + pushRequest.getPushIDSet() + "'.");
                }
                pushGroupManager.getBrowser(getBrowserID()).setPushIDSet(pushRequest.getPushIDSet());
                adjustConnectionRecreationTimeout(pushRequest);

                respondIfPendingRequest(closeConnectionDuplicate);
                long sequenceNumber;
                try {
                    sequenceNumber = pushRequest.getSequenceNumber();
                } catch (final RuntimeException exception) {
                    sequenceNumber = 0;
                }
                pushGroupManager.getBrowser(getBrowserID()).setSequenceNumber(sequenceNumber);
                //resend notifications if the window owning the blocking connection has changed
                String currentWindow = pushRequest.getWindowID();
                currentWindow = currentWindow == null ? "" : currentWindow;
                boolean resend = !lastWindow.equals(currentWindow);
                lastWindow = currentWindow;

                pendingRequest.put(pushRequest);
                setNotifyBackURI(pushRequest);
                pushGroupManager.scan(pushGroupManager.getBrowser(getBrowserID()).getPushIDSet().toArray(STRINGS));
                pushGroupManager.getBrowser(getBrowserID()).cancelConfirmationTimeout();
                pushGroupManager.cancelExpiryTimeouts(pushGroupManager.getBrowser(getBrowserID()).getID());
                pushGroupManager.startExpiryTimeouts(pushGroupManager.getBrowser(getBrowserID()).getID());
                if (null != pushGroupManager.getBrowser(getBrowserID()).getNotifyBackURI())  {
                    pushGroupManager.
                        pruneParkedIDs(
                            pushGroupManager.getBrowser(getBrowserID()).getNotifyBackURI(),
                            pushGroupManager.getBrowser(getBrowserID()).getPushIDSet());
                }
                if (!respondIfBackOffRequested()) {
                    if (!sendNotifications(pushGroupManager.getPendingNotificationSet())) {
                        if (resend) {
                            resendLastNotifications();
                        } else {
                            respondIfNotificationsAvailable();
                        }
                    }
                }
            } catch (final Throwable throwable) {
                LOGGER.log(Level.WARNING, "Failed to respond to request", throwable);
                respondIfPendingRequest(new ServerError(throwable));
            }
        }

        public void shutdown() {
            //avoid creating new blocking connections after shutdown
            activeServer = AfterShutdown;
            respondIfPendingRequest(
                terminateBlockingConnectionOnShutdown ? closeConnectionShutdown : NOOP_SHUTDOWN);
        }
    }
}
