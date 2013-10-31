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
import java.util.concurrent.ConcurrentLinkedQueue;
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
                super.respond(pushResponse);
                browser.getStatus().revertConnectionRecreationTimeout();
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
    private Browser browser;
    private long responseTimeoutTime;
    private PushServer activeServer;
    private ConcurrentLinkedQueue<NotificationEntry> notifiedPushIDs = new ConcurrentLinkedQueue<NotificationEntry>();
    private Timer monitoringScheduler;

    private String lastWindow = "";
    private Set<NotificationEntry> lastNotifications = new HashSet<NotificationEntry>();
    private long defaultConnectionRecreationTimeout;
    private long responseTimestamp = System.currentTimeMillis();
    private long requestTimestamp = System.currentTimeMillis();
    private long backOffDelay = 0;

    public BlockingConnectionServer(
        final String browserID, final Timer monitoringScheduler, final Slot heartbeat,
        final boolean terminateBlockingConnectionOnShutdown, final Configuration configuration) {

        this.minCloudPushInterval = configuration.getAttributeAsLong("minCloudPushInterval", 10 * 1000);
        this.browser = newBrowser(browserID, getMinCloudPushInterval());
        this.pushGroupManager.addBlockingConnectionServer(browser.getID(), this);
        this.monitoringScheduler = monitoringScheduler;
        this.heartbeatInterval = heartbeat;
        this.defaultConnectionRecreationTimeout = configuration.getAttributeAsLong("connectionRecreationTimeout", 5000);
        //add monitor
        this.monitoringScheduler.scheduleAtFixedRate(this, 0, 1000);
        this.pushGroupManager.addNotificationReceiver(this);

        //define blocking server
        activeServer = new RunningServer(terminateBlockingConnectionOnShutdown);
    }

    public synchronized void backOff(final long delay) {
        if (delay > 0) {
            backOffDelay = delay;
            respondIfBackOffRequested();
        }
    }

    public Browser getBrowser() {
        return browser;
    }

    public boolean isInterested(Set<NotificationEntry> notificationSet) {
        Iterator<NotificationEntry> notificationEntryIterator = notificationSet.iterator();
        while (notificationEntryIterator.hasNext()) {
            if (browser.getPushIDSet().contains(notificationEntryIterator.next().getPushID())) {
                return true;
            }
        }
        return false;
    }

    public void service(final PushRequest pushRequest)
    throws Exception {
        activeServer.service(pushRequest);
    }

    public void shutdown() {
        cancel();
        pushGroupManager.deleteNotificationReceiver(this);
        pushGroupManager.removeBlockingConnectionServer(browser.getID());
        activeServer.shutdown();
    }

    public void run() {
        try {
            if ((System.currentTimeMillis() > responseTimeoutTime) && (!pendingRequest.isEmpty())) {
                respondIfPendingRequest(NOOP_TIMEOUT);
            }
        } catch (Exception exception) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(
                    Level.WARNING, "Exception caught on " + this.getClass().getName() + " TimerTask.", exception);
            }
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

    private synchronized boolean sendNotifications(final Set<NotificationEntry> notificationSet) {
        //stop sending notifications if pushID are not used anymore by the browser
        Set<NotificationEntry> matchingSet = new HashSet<NotificationEntry>();
        Iterator<NotificationEntry> notificationEntryIterator = notificationSet.iterator();
        while (notificationEntryIterator.hasNext()) {
            NotificationEntry notificationEntry = notificationEntryIterator.next();
            if (browser.getPushIDSet().contains(notificationEntry.getPushID())) {
                matchingSet.add(notificationEntry);
            }
        }
        boolean anyNotifications = !matchingSet.isEmpty();
        if (anyNotifications) {
            notifiedPushIDs.addAll(matchingSet);
            notifiedPushIDs.retainAll(pushGroupManager.getPendingNotificationSet());
            resetTimeout();
            respondIfNotificationsAvailable();
        }
        return anyNotifications;
    }

    private void resendLastNotifications() {
        sendNotifications(lastNotifications);
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
        if (!notifiedPushIDs.isEmpty()) {
            //save notifications, maybe they will need to be resent when blocking connection switches to another window
            lastNotifications = new HashSet<NotificationEntry>(notifiedPushIDs);
            respondIfPendingRequest(
                new NotifiedPushIDs(lastNotifications) {
                    @Override
                    public void writeTo(final Writer writer)
                    throws IOException {
                        super.writeTo(writer);
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.log(
                                Level.FINE,
                                "Push Notifications available for PushIDs '" + getPushIDSet() + "', trying to respond.");
                        }
                        pushGroupManager.clearPendingNotifications(browser.getPushIDSet());
                        notifiedPushIDs.removeAll(lastNotifications);
                        Set<String> groupNameSet = new HashSet<String>();
                        for (final NotificationEntry notificationEntry : lastNotifications) {
                            String groupName = notificationEntry.getGroupName();
                            if (groupNameSet.add(groupName)) {
                                notificationSent(
                                    new NotificationEvent(
                                        TargetType.BROWSER_ID, browser.getID(), groupName,
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

    private void resetTimeout() {
        responseTimeoutTime = System.currentTimeMillis() + heartbeatInterval.getLongValue();
    }

    private boolean respondIfPendingRequest(final PushResponseHandler handler) {
        PushRequest previousRequest = pendingRequest.poll();
        if (previousRequest != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE, "Pending request for PushIDs '" + browser.getPushIDSet() + "', trying to respond.");
            }
            try {
                recordResponseTime();
                previousRequest.respondWith(handler);
                return true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    public void receive(final Set<NotificationEntry> notificationSet) {
        sendNotifications(notificationSet);
    }

    private class RunningServer
    implements PushServer {
        private final boolean terminateBlockingConnectionOnShutdown;

        public RunningServer(final boolean terminateBlockingConnectionOnShutdown) {
            this.terminateBlockingConnectionOnShutdown = terminateBlockingConnectionOnShutdown;
        }

        public void service(final PushRequest pushRequest) throws Exception {
            resetTimeout();
            try {
                browser.setPushIDSet(pushRequest.getPushIDSet());
                adjustConnectionRecreationTimeout(pushRequest);

                respondIfPendingRequest(closeConnectionDuplicate);
                long sequenceNumber;
                try {
                    sequenceNumber = pushRequest.getSequenceNumber();
                } catch (final RuntimeException exception) {
                    sequenceNumber = 0;
                }
                browser.setSequenceNumber(sequenceNumber);
                //resend notifications if the window owning the blocking connection has changed
                String currentWindow = pushRequest.getWindowID();
                currentWindow = currentWindow == null ? "" : currentWindow;
                boolean resend = !lastWindow.equals(currentWindow);
                lastWindow = currentWindow;

                pendingRequest.put(pushRequest);
                setNotifyBackURI(pushRequest);
                pushGroupManager.scan(browser.getPushIDSet().toArray(STRINGS));
                browser.cancelConfirmationTimeout();
                pushGroupManager.cancelExpiryTimeout(browser);
                pushGroupManager.startExpiryTimeout(browser);
                if (null != browser.getNotifyBackURI())  {
                    pushGroupManager.pruneParkedIDs(browser.getNotifyBackURI(), browser.getPushIDSet());
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

    private void adjustConnectionRecreationTimeout(final PushRequest pushRequest) {
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

    private void setNotifyBackURI(final PushRequest pushRequest) {
        String notifyBack = pushRequest.getNotifyBackURI();
        if (notifyBack != null && notifyBack.trim().length() != 0) {
            browser.setNotifyBackURI(new NotifyBackURI(notifyBack), true);
        }
    }

    private final Set<NotificationListener>
        listenerSet = new CopyOnWriteArraySet<NotificationListener>();

    public void addNotificationListener(final NotificationListener listener) {
        listenerSet.add(listener);
    }

    public void removeNotificationListener(final NotificationListener listener) {
        listenerSet.remove(listener);
    }

    protected void notificationSent(final NotificationEvent event) {
        for (final NotificationListener listener : listenerSet) {
            listener.notificationSent(event);
        }
    }
}
