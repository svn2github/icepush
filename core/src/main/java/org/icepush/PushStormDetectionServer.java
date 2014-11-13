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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.http.PushRequest;
import org.icepush.http.PushResponseHandler;
import org.icepush.http.PushServer;

public class PushStormDetectionServer
implements PushServer {
    private static final Logger LOGGER = Logger.getLogger(PushStormDetectionServer.class.getName());

    private static final long DefaultLoopInterval = 275;
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
