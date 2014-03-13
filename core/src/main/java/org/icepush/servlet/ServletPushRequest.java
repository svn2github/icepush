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

    public ServletPushRequest(final HttpServletRequest request, final Configuration configuration)
    throws Exception {
        super(request, configuration);
    }

    public String getBrowserID() {
        String _browserID = getHeader("ice.push.browser");
        if (_browserID == null) {
            _browserID = getParameter("ice.push.browser");
        }
        return _browserID;
    }

    public long getHeartbeatInterval()
    throws NumberFormatException {
        return getParameterAsLong("ice.push.heartbeat");
    }

    public long getHeartbeatTimestamp()
    throws NumberFormatException {
        return getHeaderAsLong("ice.push.heartbeatTimestamp");
    }

    public String getNotifyBackURI() {
        return getHeader("ice.notifyBack");
    }

    public Set<String> getPushIDSet() {
        return new HashSet<String>(Arrays.asList(getParameterAsStrings("ice.pushid")));
    }

    public long getSequenceNumber()
    throws NumberFormatException {
        return getHeaderAsLong("ice.push.sequence");
    }

    public String getWindowID() {
        return getHeader("ice.push.window");
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
