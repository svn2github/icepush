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

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.http.PushRequest;
import org.icepush.http.PushResponse;
import org.icepush.http.PushResponseHandler;
import org.icepush.http.PushServer;
import org.icepush.http.standard.PushRequestProxy;
import org.icepush.util.Slot;

public class SequenceTaggingServer
implements PushServer {
    private static final Logger LOGGER = Logger.getLogger(SequenceTaggingServer.class.getName());

    private final PushServer pushServer;

    private Slot sequenceNo;
    private Set<String> participatingPushIDList = new HashSet<String>();
    private boolean participatingPushIDsChanged;
    
    public SequenceTaggingServer(final Slot sequenceNo, final PushServer pushServer) {
        this.sequenceNo = sequenceNo;
        this.pushServer = pushServer;
    }

    public void service(final PushRequest pushRequest)
    throws Exception {
        Set<String> currentParticipatingPushIDList = new HashSet<String>(pushRequest.getPushIDSet());
        participatingPushIDsChanged =
            !participatingPushIDList.containsAll(currentParticipatingPushIDList) ||
            !currentParticipatingPushIDList.containsAll(participatingPushIDList);
        if (participatingPushIDsChanged) {
            participatingPushIDList = currentParticipatingPushIDList;
        }
        pushServer.service(new TaggingRequest(pushRequest));
    }

    public void shutdown() {
        pushServer.shutdown();
    }

    public class TaggingRequest
    extends PushRequestProxy {
        public TaggingRequest(final PushRequest pushRequest) {
            super(pushRequest);
        }

        public void respondWith(final PushResponseHandler handler) throws Exception {
            getPushRequest().respondWith(new TaggingResponseHandler(handler));
        }

        private class TaggingResponseHandler
        implements PushResponseHandler {
            private final PushResponseHandler handler;

            public TaggingResponseHandler(PushResponseHandler handler) {
                this.handler = handler;
            }

            public void respond(PushResponse pushResponse)
            throws Exception {
                try {
                    long previousSequenceNo;
                    try {
                        previousSequenceNo = getPushRequest().getSequenceNumber();
                        if (previousSequenceNo >= sequenceNo.getLongValue()) {
                            sequenceNo.setLongValue(previousSequenceNo + 1);
                        } else if (participatingPushIDsChanged) {
                            sequenceNo.setLongValue(sequenceNo.getLongValue() + 1);
                        } else {
                            if (LOGGER.isLoggable(Level.FINE)) {
                                LOGGER.log(
                                    Level.FINE,
                                    "Request's 'ice.push.sequence' [" + previousSequenceNo + "] is less than " +
                                        "the server-side sequence number [" + sequenceNo.getLongValue() + "].");
                            }
                            sequenceNo.setLongValue(sequenceNo.getLongValue() + 1);
                        }
                    } catch (RuntimeException e) {
                        // No sequence number found.
                        if (sequenceNo.getLongValue() == 0) {
                            // Start with sequence number
                            sequenceNo.setLongValue((long)1);
                        } else {
                            if (LOGGER.isLoggable(Level.FINE)) {
                                LOGGER.log(
                                    Level.FINE,
                                    "Request's 'ice.push.sequence' header is missing, " +
                                        "while server-side sequence number is '" + sequenceNo.getLongValue() + "'.");
                            }
                            sequenceNo.setLongValue(sequenceNo.getLongValue() + 1);
                        }
                    }
                    pushResponse.setSequenceNumber(sequenceNo.getLongValue());
                } finally {
                    handler.respond(pushResponse);
                }
            }
        }
    }
}
