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

import org.icepush.http.PushResponse;
import org.icepush.http.PushResponseHandler;

public class ConnectionClose
implements PushResponseHandler {
    private static final Logger LOGGER = Logger.getLogger(ConnectionClose.class.getName());

    private final String reason;

    public ConnectionClose(final String reason) {
        this.reason = reason;
    }

    public void respond(final PushResponse pushResponse)
    throws Exception {
        //let the bridge know that this blocking connection should not be re-initialized
        pushResponse.setHeader("X-Connection", "close");
        pushResponse.setHeader("X-Connection-reason", reason);
        pushResponse.setHeader("Content-Length", 0);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Close current blocking connection.");
        }
        pushResponse.writeBody().close();
    }
}
