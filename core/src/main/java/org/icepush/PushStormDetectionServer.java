package org.icepush;

import org.icepush.http.Request;
import org.icepush.http.Response;
import org.icepush.http.ResponseHandler;
import org.icepush.http.Server;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PushStormDetectionServer implements Server {
    private static final Logger log = Logger.getLogger(PushStormDetectionServer.class.getName());
    private static final long DefaultLoopInterval = 700;
    private static final long DefaultMaxTightLoopRequests = 25;
    private static final long DefaultBackOffInterval = -1;
    private static final ResponseHandler PushStormResponse = new PushStormResponseHandler();

    private Server server;
    private long lastTimeAccess = System.currentTimeMillis();
    private int successiveTightLoopRequests = 0;
    private long loopInterval;
    private long maxTightLoopRequests;

    private BackOffResponseHandler backOffResponseHandler;

    public PushStormDetectionServer(Server server, Configuration configuration) {
        this.server = server;
        loopInterval = configuration.getAttributeAsLong("notificationStormLoopInterval", DefaultLoopInterval);
        maxTightLoopRequests = configuration.getAttributeAsLong("notificationStormMaximumRequests", DefaultMaxTightLoopRequests);

        try {
            long backOffInterval = configuration.getAttributeAsLong("notificationStormBackOffInterval");
            backOffResponseHandler = new BackOffResponseHandler(backOffInterval);
        } catch (ConfigurationException e) {
            backOffResponseHandler = null;
        }
    }

    public void service(Request request) throws Exception {
        if (System.currentTimeMillis() - lastTimeAccess < loopInterval) {
            ++successiveTightLoopRequests;
        } else {
            successiveTightLoopRequests = 0;
        }
        lastTimeAccess = System.currentTimeMillis();

        if (successiveTightLoopRequests > maxTightLoopRequests) {
            if (backOffResponseHandler == null) {
                request.respondWith(PushStormResponse);
            } else {
                request.respondWith(backOffResponseHandler);
            }
        } else {
            server.service(request);
        }
    }

    public void shutdown() {
        server.shutdown();
    }

    private static class PushStormResponseHandler implements ResponseHandler {
        public void respond(Response response) throws Exception {
            //let the bridge know that this blocking connection should not be re-initialized
            response.setHeader("X-Connection", "close");
            response.setHeader("X-Connection-reason", "push storm occurred");
            response.setHeader("Content-Length", 0);
            if (log.isLoggable(Level.WARNING)) {
                log.log(Level.WARNING, "Push storm occurred, shutting down blocking connection.");
            }
        }
    }
}
