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

package org.icepush.servlet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.icepush.Configuration;
import org.icepush.http.PushRequest;
import org.icepush.http.PushResponseHandler;
import org.icepush.http.Request;
import org.icepush.http.ResponseHandler;

public class ServletPushRequest
extends ServletRequest
implements PushRequest, Request {
    private static final Logger LOGGER = Logger.getLogger(ServletPushRequest.class.getName());

    private String cachedBrowserID;
    private Long cachedHeartbeatInterval;
    private Long cachedHeartbeatTimestamp;
    private String cachedNotifyBackURI;
    private Set<String> cachedPushIDSet;
    private Long cachedSequenceNumber;
    private String cachedWindowID;

    public ServletPushRequest(final HttpServletRequest request, final Configuration configuration)
    throws Exception {
        super(request, configuration);
    }

    public String getBrowserID() {
        if (cachedBrowserID == null) {
            cachedBrowserID = getParameter("ice.push.browser");
        }
        return cachedBrowserID;
    }

    public long getHeartbeatInterval()
    throws NumberFormatException {
        if (cachedHeartbeatInterval == null) {
            cachedHeartbeatInterval = getParameterAsLong("ice.push.heartbeat");
        }
        return cachedHeartbeatInterval;
    }

    public long getHeartbeatTimestamp()
    throws NumberFormatException {
        if (cachedHeartbeatTimestamp == null) {
            cachedHeartbeatTimestamp = getParameterAsLong("ice.push.heartbeatTimestamp", 0);
        }
        return cachedHeartbeatTimestamp;
    }

    public String getNotifyBackURI() {
        if (cachedNotifyBackURI == null) {
            cachedNotifyBackURI = getParameter("ice.notifyBack", null);
        }
        return cachedNotifyBackURI;
    }

    public Set<String> getPushIDSet() {
        if (cachedPushIDSet == null) {
            cachedPushIDSet = new HashSet<String>(Arrays.asList(getParameterAsStrings("ice.pushid")));
        }
        return cachedPushIDSet;
    }

    public long getSequenceNumber()
    throws NumberFormatException {
        if (cachedSequenceNumber == null) {
            cachedSequenceNumber = getParameterAsLong("ice.push.sequence", 0);
        }
        return cachedSequenceNumber;
    }

    public String getWindowID() {
        if (cachedWindowID == null) {
            cachedWindowID = getParameter("ice.push.window", null);
        }
        return cachedWindowID;
    }

    @Override
    public void respondWith(final ResponseHandler handler)
    throws Exception {
        throw new UnsupportedOperationException();
    }

    public void respondWith(final PushResponseHandler handler)
    throws Exception {
        throw new UnsupportedOperationException();
    }
}
