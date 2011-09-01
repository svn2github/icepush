/*
 * Version: MPL 1.1
 *
 * "The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is ICEfaces 1.5 open source software code, released
 * November 5, 2006. The Initial Developer of the Original Code is ICEsoft
 * Technologies Canada, Corp. Portions created by ICEsoft are Copyright (C)
 * 2004-2010 ICEsoft Technologies Canada, Corp. All Rights Reserved.
 *
 * Contributor(s): _____________________.
 *
 */

package org.icepush;

import org.icepush.http.Request;
import org.icepush.http.Response;
import org.icepush.http.ResponseHandler;
import org.icepush.http.Server;
import org.icepush.http.standard.FixedXMLContentHandler;
import org.icepush.http.standard.ResponseHandlerServer;
import org.icepush.util.Slot;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BlockingConnectionServer extends TimerTask implements Server, Observer {
    private static final Logger log = Logger.getLogger(BlockingConnectionServer.class.getName());
    private static final ResponseHandler CloseResponseDup = new CloseConnectionResponseHandler("duplicate");
    private static final ResponseHandler CloseResponseDown = new CloseConnectionResponseHandler("shutdown");
    //Define here to avoid classloading problems after application exit
    private static final ResponseHandler NoopResponse = new NoopResponseHandler();
    private static final Server AfterShutdown = new ResponseHandlerServer(CloseResponseDown);

    private final BlockingQueue pendingRequest = new LinkedBlockingQueue(1);
    private final Slot heartbeatInterval;
    private final PushGroupManager pushGroupManager;
    private long responseTimeoutTime;
    private Server activeServer;
    private ConcurrentLinkedQueue notifiedPushIDs = new ConcurrentLinkedQueue();
    private List participatingPushIDs = Collections.emptyList();

    private String lastWindow = "";
    private String[] lastNotifications = new String[]{};

    public BlockingConnectionServer(final PushGroupManager pushGroupManager, final Timer monitorRunner, Slot heartbeat, final boolean terminateBlockingConnectionOnShutdown) {
        this.heartbeatInterval = heartbeat;
        this.pushGroupManager = pushGroupManager;
        //add monitor
        monitorRunner.scheduleAtFixedRate(this, 0, 1000);
        this.pushGroupManager.addObserver(this);

        //define blocking server
        activeServer = new RunningServer(pushGroupManager, terminateBlockingConnectionOnShutdown);
    }

    public void update(Observable observable, Object o) {
        sendNotifications((String[]) o);
    }

    public void service(final Request request) throws Exception {
        activeServer.service(request);
    }

    public void shutdown() {
        cancel();
        pushGroupManager.deleteObserver(this);
        activeServer.shutdown();
    }

    public void run() {
        if ((System.currentTimeMillis() > responseTimeoutTime) && (!pendingRequest.isEmpty())) {
            respondIfPendingRequest(NoopResponse);
        }
    }

    private boolean sendNotifications(String[] ids) {
        //stop sending notifications if pushID are not used anymore by the browser
        List pushIDs = new ArrayList(Arrays.asList(ids));
        pushIDs.retainAll(participatingPushIDs);
        boolean anyNotifications = !pushIDs.isEmpty();

        if (anyNotifications) {
            notifiedPushIDs.addAll(pushIDs);
            resetTimeout();
            respondIfNotificationsAvailable();
        }

        return anyNotifications;
    }

    private void resendLastNotifications() {
        sendNotifications(lastNotifications);
    }

    private synchronized void respondIfNotificationsAvailable() {
        if (!notifiedPushIDs.isEmpty()) {
            //save notifications, maybe they will need to be resent when blocking connection switches to another window 
            lastNotifications = (String[]) notifiedPushIDs.toArray(new String[0]);
            respondIfPendingRequest(new NotificationHandler(lastNotifications) {
                public void writeTo(Writer writer) throws IOException {
                    super.writeTo(writer);

                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Sending notifications for " + notifiedPushIDs + ".");
                    }
                    notifiedPushIDs.removeAll(Arrays.asList(lastNotifications));
                }
            });
        }
    }

    private void resetTimeout() {
        responseTimeoutTime = System.currentTimeMillis() + heartbeatInterval.getLongValue();
    }

    private void respondIfPendingRequest(ResponseHandler handler) {
        Request previousRequest = (Request) pendingRequest.poll();
        if (previousRequest != null) {
            try {
                previousRequest.respondWith(handler);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class NoopResponseHandler extends FixedXMLContentHandler {
        public void writeTo(Writer writer) throws IOException {
            writer.write("<noop/>");

            if (log.isLoggable(Level.FINEST)) {
                log.finest("Sending NoOp.");
            }
        }
    }

    private static class CloseConnectionResponseHandler implements ResponseHandler {
        private String reason = "undefined";

        public CloseConnectionResponseHandler(String reason) {
            this.reason = reason;
        }

        public void respond(Response response) throws Exception {
            //let the bridge know that this blocking connection should not be re-initialized
            response.setHeader("X-Connection", "close");
            response.setHeader("X-Connection-reason", reason);
            response.setHeader("Content-Length", 0);

            if (log.isLoggable(Level.FINEST)) {
                log.finest("Close current blocking connection.");
            }
        }
    }

    private class NotificationHandler extends FixedXMLContentHandler {
        private String[] pushIDs;

        private NotificationHandler(String[] pushIDs) {
            this.pushIDs = pushIDs;
        }

        public void writeTo(Writer writer) throws IOException {
            writer.write("<notified-pushids>");
            for (int i = 0; i < pushIDs.length; i++) {
                String id = pushIDs[i];
                if (i > 0) {
                    writer.write(' ');
                }
                writer.write(id);
            }
            writer.write("</notified-pushids>");
        }
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
            respondIfPendingRequest(CloseResponseDup);

            //resend notifications if the window owning the blocking connection has changed
            String currentWindow = request.getHeader("ice.push.window");
            currentWindow = currentWindow == null ? "" : currentWindow;
            boolean resend = !lastWindow.equals(currentWindow);
            lastWindow = currentWindow;

            pendingRequest.put(request);
            try {
                participatingPushIDs = Arrays.asList(request.getParameterAsStrings("ice.pushid"));
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Participating pushIds: " + participatingPushIDs + ".");
                }

                if (!sendNotifications(pushGroupManager.getPendingNotifications())) {
                    if (resend) {
                        resendLastNotifications();
                    } else {
                        respondIfNotificationsAvailable();
                    }
                }
                pushGroupManager.notifyObservers(new ArrayList(participatingPushIDs));
            } catch (RuntimeException e) {
                log.fine("Request does not contain pushIDs.");
                respondIfPendingRequest(NoopResponse);
            }
        }

        public void shutdown() {
            //avoid creating new blocking connections after shutdown
            activeServer = AfterShutdown;
            respondIfPendingRequest(terminateBlockingConnectionOnShutdown ? CloseResponseDown : NoopResponse);
        }
    }
}
