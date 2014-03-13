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

import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Cookie {
    private static final Logger LOGGER = Logger.getLogger(Cookie.class.getName());

    private String name;
    private String value;
    private String path;

    public Cookie(final String cookie) {
        StringTokenizer _tokens = new StringTokenizer(cookie, ";");
        int _tokenCount = _tokens.countTokens();
        for (int i = 0; i < _tokenCount; i++) {
            String _token = _tokens.nextToken().trim();
            int _index = _token.indexOf("=");
            if (i == 0) {
                // NAME=VALUE
                name = _token.substring(0, _index);
                value = _token.substring(_index + 1);
            } else if (_token.substring(0, _index).equalsIgnoreCase("Path")) {
                path = _token.substring(_index + 1);
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getValue() {
        return value;
    }
}
