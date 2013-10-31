package org.icepush;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.http.PushRequest;
import org.icepush.http.PushResponseHandler;
import org.icepush.http.PushServer;

public class PushStormDetectionServer
implements PushServer {
    private static final Logger LOGGER = Logger.getLogger(PushStormDetectionServer.class.getName());

    private static final long DefaultLoopInterval = 700;
    private static final long DefaultMaxTightLoopRequests = 25;

    private final PushServer pushServer;

    private long backOffInterval;
    private long lastTimeAccess = System.currentTimeMillis();
    private int successiveTightLoopRequests = 0;
    private long loopInterval;
    private long maxTightLoopRequests;

    public PushStormDetectionServer(final PushServer pushServer, final Configuration configuration) {
        this.pushServer = pushServer;
        loopInterval = configuration.getAttributeAsLong("notificationStormLoopInterval", DefaultLoopInterval);
        maxTightLoopRequests = configuration.getAttributeAsLong("notificationStormMaximumRequests", DefaultMaxTightLoopRequests);

        try {
            backOffInterval = configuration.getAttributeAsLong("notificationStormBackOffInterval");
        } catch (ConfigurationException e) {
            backOffInterval = -1;
        }
    }

    public void service(final PushRequest pushRequest)
    throws Exception {
        if (System.currentTimeMillis() - lastTimeAccess < loopInterval) {
            ++successiveTightLoopRequests;
        } else {
            successiveTightLoopRequests = 0;
        }
        lastTimeAccess = System.currentTimeMillis();

        if (successiveTightLoopRequests > maxTightLoopRequests) {
            if (backOffInterval == -1) {
                pushRequest.respondWith(new ConnectionClose("push storm occurred"));
            } else {
                pushRequest.respondWith((PushResponseHandler)new BackOff(backOffInterval));
            }
        } else {
            pushServer.service(pushRequest);
        }
    }

    public void shutdown() {
        pushServer.shutdown();
    }
}
