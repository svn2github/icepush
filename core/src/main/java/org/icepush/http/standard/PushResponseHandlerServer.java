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

package org.icepush.http.standard;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.http.PushRequest;
import org.icepush.http.PushResponseHandler;
import org.icepush.http.PushServer;

public class PushResponseHandlerServer
implements PushServer {
    private static final Logger LOGGER = Logger.getLogger(PushResponseHandlerServer.class.getName());

    private final PushResponseHandler handler;

    public PushResponseHandlerServer(final PushResponseHandler handler) {
        this.handler = handler;
    }

    public void service(final PushRequest pushRequest)
    throws Exception {
        pushRequest.respondWith(handler);
    }

    public void shutdown() {
        // Do nothing.
    }
}
