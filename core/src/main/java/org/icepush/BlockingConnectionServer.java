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
import java.util.Arrays;
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

import org.icepush.http.Request;
import org.icepush.http.Response;
import org.icepush.http.ResponseHandler;
import org.icepush.http.Server;
import org.icepush.http.standard.FixedXMLContentHandler;
import org.icepush.http.standard.ResponseHandlerServer;
import org.icepush.util.Slot;

public class BlockingConnectionServer extends TimerTask implements Server, NotificationBroadcaster.Receiver {
    private static final Logger LOGGER = Logger.getLogger(BlockingConnectionServer.class.getName());
    private static final String[] STRINGS = new String[0];
    //Define here to avoid classloading problems after application exit
    private static final ResponseHandler ShutdownNoopResponse = new NoopResponseHandler("shutdown");
    private static final ResponseHandler TimeoutNoopResponse = new NoopResponseHandler("response timeout");
    private final ResponseHandler CloseResponseDup = new CloseConnectionResponseHandler("duplicate") {
        public void respond(Response response) throws Exception {
            super.respond(response);
            //revert timeout to previous value, duplicate requests can extend excessively the calculated delay
            //the duplicate requests occur during page reload or navigating away from the page and then returning back
            //before the server decides to sever the connection
            browser.getStatus().revertConnectionRecreationTimeout();
        }
    };
    private final ResponseHandler CloseResponseDown = new CloseConnectionResponseHandler("shutdown");
    private final Server AfterShutdown = new ResponseHandlerServer(CloseResponseDown);

    private final BlockingQueue<Request> pendingRequest = new LinkedBlockingQueue<Request>(1);
    private final Slot heartbeatInterval;
    // This is either a LocalPushGroupManager or a DynamicPushGroupManager
    private final PushGroupManager pushGroupManager;
    private final long minCloudPushInterval;
    private Browser browser;
    private long responseTimeoutTime;
    private Server activeServer;
    private ConcurrentLinkedQueue<NotificationEntry> notifiedPushIDs = new ConcurrentLinkedQueue<NotificationEntry>();
    private Timer monitoringScheduler;

    private String lastWindow = "";
    private Set<NotificationEntry> lastNotifications = new HashSet<NotificationEntry>();
    private long defaultConnectionRecreationTimeout;
    private long responseTimestamp = System.currentTimeMillis();
    private long requestTimestamp = System.currentTimeMillis();
    private long backOffDelay = 0;

    public BlockingConnectionServer(
        final String browserID, final PushGroupManager pushGroupManager, final Timer monitoringScheduler, final Slot heartbeat,
        final boolean terminateBlockingConnectionOnShutdown, final Configuration configuration) {

        this.browser = newBrowser(browserID);
        this.pushGroupManager = pushGroupManager;
        this.pushGroupManager.addBlockingConnectionServer(browser.getID(), this);
        this.monitoringScheduler = monitoringScheduler;
        this.heartbeatInterval = heartbeat;
        this.defaultConnectionRecreationTimeout = configuration.getAttributeAsLong("connectionRecreationTimeout", 5000);
        this.minCloudPushInterval = configuration.getAttributeAsLong("minCloudPushInterval", 10 * 1000);
        //add monitor
        this.monitoringScheduler.scheduleAtFixedRate(this, 0, 1000);
        this.pushGroupManager.addNotificationReceiver(this);

        //define blocking server
        activeServer = new RunningServer(pushGroupManager, terminateBlockingConnectionOnShutdown);
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

    public void service(final Request request) throws Exception {
        activeServer.service(request);
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
                respondIfPendingRequest(TimeoutNoopResponse);
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

    protected Browser newBrowser(final String browserID) {
        return new Browser(browserID, getMinCloudPushInterval(), getPushGroupManager());
    }

    private boolean sendNotifications(final Set<NotificationEntry> notificationSet) {
        //stop sending notifications if pushID are not used anymore by the browser
        Iterator<NotificationEntry> notificationEntryIterator = notificationSet.iterator();
        while (notificationEntryIterator.hasNext()) {
            if (!browser.getPushIDSet().contains(notificationEntryIterator.next().getPushID())) {
                notificationEntryIterator.remove();
            }
        }
        boolean anyNotifications = !notificationSet.isEmpty();
        if (anyNotifications) {
            notifiedPushIDs.addAll(notificationSet);
            resetTimeout();
            respondIfNotificationsAvailable();
        }
        return anyNotifications;
    }

    private void resendLastNotifications() {
        sendNotifications(lastNotifications);
    }

    private synchronized boolean respondIfBackOffRequested() {
        boolean result = false;
        if (backOffDelay > 0) {
            if (result = respondIfPendingRequest(new BackOffResponseHandler(backOffDelay))) {
                backOffDelay = 0;
            }
        }
        return result;
    }

    private synchronized void respondIfNotificationsAvailable() {
        if (!notifiedPushIDs.isEmpty()) {
            //save notifications, maybe they will need to be resent when blocking connection switches to another window
            lastNotifications = new HashSet<NotificationEntry>(notifiedPushIDs);
            respondIfPendingRequest(
                new NotificationHandler(lastNotifications) {
                    public void writeTo(Writer writer) throws IOException {
                        super.writeTo(writer);

                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.log(
                                Level.FINE,
                                "Push Notifications available for PushIDs '" + pushIDSet + "', trying to respond.");
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

    private boolean respondIfPendingRequest(ResponseHandler handler) {
        Request previousRequest = pendingRequest.poll();
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

    private static class ServerErrorResponseHandler extends FixedXMLContentHandler {
        private String message;

        private ServerErrorResponseHandler(Throwable exception) {
            this.message = exception.getMessage();
        }

        public void respond(Response response) throws Exception {
            response.setStatus(503);
            super.respond(response);
        }

        public void writeTo(Writer writer) throws IOException {
            writer.write("<server-error message=\"");
            writer.write(message);
            writer.write("\"/>");
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Sending server-error - " + message);
            }
        }
    }

    private static class NoopResponseHandler extends FixedXMLContentHandler {
        private final String reason;

        private NoopResponseHandler(final String reason) {
            this.reason = reason;
        }

        @Override
        public void respond(final Response response)
        throws Exception {
            response.setHeader("X-Connection-reason", reason);
            super.respond(response);
        }

        public void writeTo(Writer writer) throws IOException {
            writer.write("<noop/>");
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Sending NoOp.");
            }
        }
    }

    private static class BackOffResponseHandler extends FixedXMLContentHandler {
        private long delay;

        private BackOffResponseHandler(long delay) {
            this.delay = delay;
        }

        public void writeTo(Writer writer) throws IOException {
            writer.write("<back-off delay=\"" + delay + "\"/>");
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Sending back-off - " + delay + "ms.");
            }
        }
    }

    private class CloseConnectionResponseHandler implements ResponseHandler {
        private String reason = "undefined";

        public CloseConnectionResponseHandler(String reason) {
            this.reason = reason;
        }

        public void respond(Response response) throws Exception {
            //let the bridge know that this blocking connection should not be re-initialized
            response.setHeader("X-Connection", "close");
            response.setHeader("X-Connection-reason", reason);
            response.setHeader("Content-Length", 0);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Close current blocking connection.");
            }
        }
    }

    private class NotificationHandler extends FixedXMLContentHandler {
        protected Set<String> pushIDSet = new HashSet<String>();

        private NotificationHandler(final Set<NotificationEntry> notificationSet) {
            for (final NotificationEntry notificationEntry : notificationSet) {
                pushIDSet.add(notificationEntry.getPushID());
            }
        }

        public void writeTo(Writer writer) throws IOException {
            writer.write("<notified-pushids>");
            boolean first = true;
            for (final String pushID : pushIDSet) {
                if (!first) {
                    writer.write(' ');
                }
                writer.write(pushID);
                first = false;
            }
            writer.write("</notified-pushids>");
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Sending Notified PushIDs '" + pushIDSet + "'.");
            }
        }
    }

    public void receive(final Set<NotificationEntry> notificationSet) {
        Set<NotificationEntry> _copyOfNotificationSet = new HashSet<NotificationEntry>();
        _copyOfNotificationSet.addAll(notificationSet);
        sendNotifications((_copyOfNotificationSet));
    }

    private class RunningServer implements Server {
        private final PushGroupManager pushGroupManager;
        private final boolean terminateBlockingConnectionOnShutdown;

        public RunningServer(PushGroupManager pushGroupManager, boolean terminateBlockingConnectionOnShutdown) {
            this.pushGroupManager = pushGroupManager;
            this.terminateBlockingConnectionOnShutdown = terminateBlockingConnectionOnShutdown;
        }

        public void service(final Request request) throws Exception {
            resetTimeout();
            try {
                browser.setPushIDSet(new HashSet<String>(Arrays.asList(request.getParameterAsStrings("ice.pushid"))));
                adjustConnectionRecreationTimeout(request);

                respondIfPendingRequest(CloseResponseDup);
                long sequenceNumber;
                try {
                    sequenceNumber = request.getHeaderAsLong("ice.push.sequence");
                } catch (final RuntimeException exception) {
                    sequenceNumber = 0;
                }
                browser.setSequenceNumber(sequenceNumber);
                //resend notifications if the window owning the blocking connection has changed
                String currentWindow = request.getHeader("ice.push.window");
                currentWindow = currentWindow == null ? "" : currentWindow;
                boolean resend = !lastWindow.equals(currentWindow);
                lastWindow = currentWindow;

                pendingRequest.put(request);
                setNotifyBackURI(request);
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
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "Failed to respond to request", t);
                respondIfPendingRequest(new ServerErrorResponseHandler(t));
            }
        }

        public void shutdown() {
            //avoid creating new blocking connections after shutdown
            activeServer = AfterShutdown;
            respondIfPendingRequest(terminateBlockingConnectionOnShutdown ? CloseResponseDown : ShutdownNoopResponse);
        }
    }

    private void adjustConnectionRecreationTimeout(Request request) {
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
            setNotifyBackURI(request);
            LOGGER.log(
                Level.FINE,
                "ICEpush metric:" +
                    " IP: " + request.getRemoteAddr() +
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

    private void setNotifyBackURI(final Request request) {
        String notifyBack = request.getHeader("ice.notifyBack");
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
