/*
 * Copyright 2004-2012 ICEsoft Technologies Canada Corp.
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
package org.icepush.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CreatePushIdRequest
extends HttpRequest {
    private static final Logger LOGGER = Logger.getLogger(CreatePushIdRequest.class.getName());

    public CreatePushIdRequest(final String contextURI)
    throws URISyntaxException {
        super(
            Method.GET,                                                                                        // Method
            // throws URISyntaxException
            new URI(contextURI + "/create-push-id.icepush").normalize());                                 // Request-URI
    }
}
