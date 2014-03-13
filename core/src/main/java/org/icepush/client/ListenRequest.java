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
package org.icepush.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Listen Request
 *
 * Request-URI:
 *     /<servlet-context-path>/listen.icepush
 *
 * Entity Body:
 *     ice.pushid=<push-id>*(&ice.pushid=<push-id>)
 */
public class ListenRequest
extends HttpRequest {
    private static final Logger LOGGER = Logger.getLogger(ListenRequest.class.getName());

    public ListenRequest(final Set<String> pushIdSet, final String contextURI)
    throws URISyntaxException {
        super(
            Method.POST,                                                                                       // Method
            // throws URISyntaxException
            new URI(contextURI + "/listen.icepush").normalize(),                                          // Request-URI
            toForm(pushIdSet).getBytes());                                                                // Entity-Body
    }

    private static String toForm(final Set<String> pushIdSet) {
        StringBuilder _form = new StringBuilder();
        boolean _first = true;
        for (String _pushId : pushIdSet) {
            if (_first) {
                _form.append("ice.pushid=").append(_pushId);
                _first = false;
            } else {
                _form.append('&').append("ice.pushid=").append(_pushId);
            }
        }
        return _form.toString();
    }
}
