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

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.http.PushRequest;
import org.icepush.http.PushResponse;
import org.icepush.http.PushResponseHandler;
import org.icepush.http.PushServer;
import org.icepush.http.Request;
import org.icepush.http.ResponseHandler;
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
    private final Lock backOffLock = new ReentrantLock();
    private final BlockingQueue<PushRequest> pendingPushRequestQueue = new LinkedBlockingQueue<PushRequest>(1);
    private final Slot heartbeatInterval;
    // This is either a LocalPushGroupManager or a DynamicPushGroupManager
    private final PushGroupManager pushGroupManager =
        (PushGroupManager)PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName());
    private final long minCloudPushInterval;
    private final long maxHeartbeatInterval;
    private final long minHeartbeatInterval;

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

    public void backOff(final long delay)
    throws IllegalStateException {
        checkSetUp();
        getBackOffLock().lock();
        try {
            if (delay > 0) {
                backOffDelay = delay;
                respondToIfBackOffRequested((PushRequest)null);
            }
        } finally {
            getBackOffLock().unlock();
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
        sendNotificationsTo((PushRequest)null, notificationSet);
    }

    public void run()
    throws IllegalStateException {
        checkSetUp();
        try {
            if (System.currentTimeMillis() > responseTimeoutTime && !getPendingPushRequestQueue().isEmpty()) {
                // Respond only if there is a pending request as this is done on a non-container thread.
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
        getPushGroupManager().addBrowser(newBrowser(getBrowserID(), getMinCloudPushInterval()));
        getPushGroupManager().addBlockingConnectionServer(getBrowserID(), this);
        setUp = true;
        //add monitor
        monitoringScheduler.scheduleAtFixedRate(this, 0, 1000);
    }

    public void shutdown()
    throws IllegalStateException {
        checkSetUp();
        cancel();
        getPushGroupManager().removeNotificationReceiver(this);
        getPushGroupManager().removeBlockingConnectionServer(getBrowserID());
        getPushGroupManager().removeBrowser(pushGroupManager.getBrowser(getBrowserID()));
        activeServer.shutdown();
    }

    protected void checkSetUp()
    throws IllegalStateException {
        if (!setUp) {
            throw new IllegalStateException("Blocking Connection Server has not been set-up.");
        }
    }

    protected Lock getBackOffLock() {
        return backOffLock;
    }

    protected long getMinCloudPushInterval() {
        return minCloudPushInterval;
    }

    protected BlockingQueue<PushRequest> getPendingPushRequestQueue() {
        return pendingPushRequestQueue;
    }

    protected PushGroupManager getPushGroupManager() {
        return pushGroupManager;
    }

    protected Browser newBrowser(final String browserID, final long minCloudPushInterval) {
        return new Browser(browserID, minCloudPushInterval);
    }

    protected org.icepush.NotifiedPushIDs newNotifiedPushIDs(final Set<NotificationEntry> notificationEntrySet) {
        return new NotifiedPushIDs(notificationEntrySet, getBrowserID());
    }

    private void adjustConnectionRecreationTimeout(final PushRequest pushRequest) {
        Browser browser = getPushGroupManager().getBrowser(getBrowserID());
        for (final String pushIDString : browser.getPushIDSet()) {
            PushID pushID = getPushGroupManager().getPushID(pushIDString);
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
            PushID pushID = getPushGroupManager().getPushID(pushIDString);
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

    private boolean resendLastNotificationsTo(final PushRequest pushRequest) {
        return
            sendNotificationsTo(
                pushRequest, getPushGroupManager().getBrowser(getBrowserID()).getLastNotifiedPushIDSet()
            );
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
        responseTimeoutTime = System.currentTimeMillis() + heartbeatInterval;
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE,
                "Heartbeat Interval: " +
                    "client-side '" + clientSideHeartbeatInterval + "', " +
                    "server-side '" + serverSideHeartbeatInterval + "', " +
                    "used '" + heartbeatInterval + "'.");
        }
    }

    private void respondTo(final PushRequest pushRequest, final PushResponseHandler handler) {
        try {
            recordResponseTime();
            pushRequest.respondWith(handler);
        } catch (final IOException exception) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Possible communication issue encountered while responding: " + exception.getMessage(),
                    exception
                );
            }
        } catch (final Exception exception) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Failed to respond to pending request: " + exception.getMessage(),
                    exception
                );
            } else if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(
                    Level.SEVERE,
                    "Failed to respond to pending request: " + exception.getMessage());
            }
        }
    }

    private boolean respondToIfBackOffRequested(final PushRequest pushRequest) {
        getBackOffLock().lock();
        try {
            boolean _result = false;
            if (backOffDelay > 0) {
                BackOff _backOff = new BackOff(backOffDelay);
                if (pushRequest != null) {
                    respondTo(pushRequest, _backOff);
                    _result = true;
                } else {
                    _result = respondIfPendingRequest(_backOff);
                }
                if (_result) {
                    backOffDelay = 0;
                }
            }
            return _result;
        } finally {
            getBackOffLock().unlock();
        }
    }

    private boolean respondToIfNotificationsAvailable(final PushRequest pushRequest) {
        getPushGroupManager().getBrowser(getBrowserID()).lockNotifiedPushIDQueue();
        try {
            boolean _result = false;
            if (getPushGroupManager().getBrowser(getBrowserID()).hasNotifiedPushIDs()) {
                //save notifications, maybe they will need to be resent when blocking connection switches to another window
                getPushGroupManager().getBrowser(getBrowserID()).
                    setLastNotifiedPushIDSet(getPushGroupManager().getBrowser(getBrowserID()).getNotifiedPushIDSet());
                org.icepush.NotifiedPushIDs _notifiedPushIDs =
                    newNotifiedPushIDs(getPushGroupManager().getBrowser(getBrowserID()).getLastNotifiedPushIDSet());
                if (pushRequest != null) {
                    respondTo(pushRequest, _notifiedPushIDs);
                    _result = true;
                } else {
                    _result = respondIfPendingRequest(_notifiedPushIDs);
                }
            }
            return _result;
        } finally {
            getPushGroupManager().getBrowser(getBrowserID()).unlockNotifiedPushIDQueue();
        }
    }

    private boolean respondIfPendingRequest(final PushResponseHandler handler) {
        PushRequest _previousPushRequest = getPendingPushRequestQueue().poll();
        if (_previousPushRequest != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Pending request for PushIDs '" + getPushGroupManager().getBrowser(getBrowserID()).getPushIDSet() + "', " +
                        "trying to respond.");
            }
            respondTo(_previousPushRequest, handler);
            return true;
        }
        return false;
    }

    private boolean sendNotificationsTo(
        final PushRequest pushRequest, final Set<NotificationEntry> notificationSet) {

        //stop sending notifications if pushID are not used anymore by the browser
        Set<NotificationEntry> matchingSet = new HashSet<NotificationEntry>();
        Iterator<NotificationEntry> notificationEntryIterator = notificationSet.iterator();
        while (notificationEntryIterator.hasNext()) {
            NotificationEntry notificationEntry = notificationEntryIterator.next();
            if (getPushGroupManager().getBrowser(getBrowserID()).getPushIDSet().contains(notificationEntry.getPushID())) {
                matchingSet.add(notificationEntry);
            }
        }
        boolean anyNotifications = !matchingSet.isEmpty();
        if (anyNotifications) {
            getPushGroupManager().getBrowser(getBrowserID()).lockNotifiedPushIDQueue();
            try {
                getPushGroupManager().getBrowser(getBrowserID()).
                    addNotifiedPushIDs(matchingSet);
                getPushGroupManager().getBrowser(getBrowserID()).
                    retainNotifiedPushIDs(getPushGroupManager().getPendingNotificationSet());
            } finally {
                getPushGroupManager().getBrowser(getBrowserID()).unlockNotifiedPushIDQueue();
            }
            if (pushRequest != null) {
                resetTimeout(pushRequest);
            } else {
                resetTimeout(getPendingPushRequestQueue().peek());
            }
            respondToIfNotificationsAvailable(pushRequest);
        }
        return anyNotifications;
    }

    private void setNotifyBackURI(final PushRequest pushRequest) {
        String notifyBack = pushRequest.getNotifyBackURI();
        if (notifyBack != null && notifyBack.trim().length() != 0) {
            getPushGroupManager().getBrowser(getBrowserID()).
                setNotifyBackURI(getPushGroupManager().newNotifyBackURI(notifyBack), true);
        }
    }

    protected static class NotifiedPushIDs
    extends org.icepush.NotifiedPushIDs
    implements PushResponseHandler, ResponseHandler {
        private static final Logger LOGGER = Logger.getLogger(NotifiedPushIDs.class.getName());

        private final PushGroupManager pushGroupManager =
            (PushGroupManager)PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName());

        protected NotifiedPushIDs(final Set<NotificationEntry> notificationEntrySet, final String browserID) {
            super(notificationEntrySet, browserID);
        }

        @Override
        public void writeTo(final Writer writer)
        throws IOException {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Send Notifications to Browser-ID '" + getBrowserID() + "'.");
            }
            super.writeTo(writer);
            getPushGroupManager().
                clearPendingNotifications(
                    getPushGroupManager().getBrowser(getBrowserID()).getPushIDSet()
                );
            getPushGroupManager().getBrowser(getBrowserID()).lockNotifiedPushIDQueue();
            try {
                getPushGroupManager().getBrowser(getBrowserID()).
                    removeNotifiedPushIDs(
                        getPushGroupManager().getBrowser(getBrowserID()).getLastNotifiedPushIDSet()
                    );
            } finally {
                getPushGroupManager().getBrowser(getBrowserID()).unlockNotifiedPushIDQueue();
            }
        }

        protected PushGroupManager getPushGroupManager() {
            return pushGroupManager;
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
                getPushGroupManager().getBrowser(getBrowserID()).setPushIDSet(pushRequest.getPushIDSet());
                adjustConnectionRecreationTimeout(pushRequest);
                // Respond only if there is a pending request as it would be considered a duplicate.
                respondIfPendingRequest(closeConnectionDuplicate);
                long sequenceNumber;
                try {
                    sequenceNumber = pushRequest.getSequenceNumber();
                } catch (final RuntimeException exception) {
                    sequenceNumber = 0;
                }
                getPushGroupManager().getBrowser(getBrowserID()).setSequenceNumber(sequenceNumber);
                //resend notifications if the window owning the blocking connection has changed
                String currentWindow = pushRequest.getWindowID();
                currentWindow = currentWindow == null ? "" : currentWindow;
                boolean resend = !lastWindow.equals(currentWindow);
                lastWindow = currentWindow;
                setNotifyBackURI(pushRequest);
                getPushGroupManager().scan(getPushGroupManager().getBrowser(getBrowserID()).getPushIDSet().toArray(STRINGS));
                getPushGroupManager().getBrowser(getBrowserID()).cancelConfirmationTimeout();
                getPushGroupManager().cancelExpiryTimeouts(getPushGroupManager().getBrowser(getBrowserID()).getID());
                getPushGroupManager().startExpiryTimeouts(getPushGroupManager().getBrowser(getBrowserID()).getID());
                if (null != getPushGroupManager().getBrowser(getBrowserID()).getNotifyBackURI())  {
                    getPushGroupManager().
                        pruneParkedIDs(
                            getPushGroupManager().getBrowser(getBrowserID()).getNotifyBackURI(),
                            getPushGroupManager().getBrowser(getBrowserID()).getPushIDSet());
                }
                if (!respondToIfBackOffRequested(pushRequest)) {
                    // No response has been sent to the request.
                    if (!sendNotificationsTo(pushRequest, getPushGroupManager().getPendingNotificationSet())) {
                        // No response has been sent to the request.
                        if (!resend || !resendLastNotificationsTo(pushRequest)) {
                            // No response has been sent to the request.
                            if (respondToIfNotificationsAvailable(pushRequest)) {
                                // No response has been sent to the request.
                                getPendingPushRequestQueue().put(pushRequest);
                            }
                        }
                    }
                }
            } catch (final Throwable throwable) {
                LOGGER.log(Level.WARNING, "Failed to respond to request", throwable);
                respondTo(pushRequest, new ServerError(throwable));
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
