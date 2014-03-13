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

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icepush.Configuration;
import org.icepush.http.PushRequest;
import org.icepush.http.PushResponse;
import org.icepush.http.PushResponseHandler;
import org.icepush.http.Request;
import org.icepush.http.Response;
import org.icepush.http.ResponseHandler;

public class ServletPushRequestResponse
extends ServletRequestResponse
implements PushRequest, PushResponse, Request, Response {
    private static final Logger LOGGER = Logger.getLogger(ServletPushRequestResponse.class.getName());

    private final PushRequest pushRequest;
    private final PushResponse pushResponse;

    public ServletPushRequestResponse(
        final HttpServletRequest request, final HttpServletResponse response, final Configuration configuration)
    throws Exception {
        this(new ServletPushRequest(request, configuration), new ServletPushResponse(response, configuration));
    }

    public ServletPushRequestResponse(
        final ServletPushRequest pushRequest, final ServletPushResponse pushResponse)
    throws Exception {
        super(pushRequest, pushResponse);
        this.pushRequest = pushRequest;
        this.pushResponse = pushResponse;
    }

    public String getBrowserID() {
        return pushRequest.getBrowserID();
    }

    public long getHeartbeatInterval()
    throws NumberFormatException {
        return pushRequest.getHeartbeatInterval();
    }

    public long getHeartbeatTimestamp()
    throws NumberFormatException {
        return pushRequest.getHeartbeatTimestamp();
    }

    public String getNotifyBackURI() {
        return pushRequest.getNotifyBackURI();
    }

    public Set<String> getPushIDSet() {
        return pushRequest.getPushIDSet();
    }

    public long getSequenceNumber()
    throws NumberFormatException {
        return pushRequest.getSequenceNumber();
    }

    public String getWindowID() {
        return pushRequest.getWindowID();
    }

    @Override
    public void respondWith(final ResponseHandler handler)
    throws Exception {
        throw new UnsupportedOperationException();
    }

    public void respondWith(final PushResponseHandler handler)
    throws Exception {
        handler.respond(this);
    }

    public void setHeartbeatInterval(final long heartbeatInterval) {
        pushResponse.setHeartbeatInterval(heartbeatInterval);
    }

    public void setHeartbeatTimestamp(final long heartbeatTimestamp) {
        pushResponse.setHeartbeatTimestamp(heartbeatTimestamp);
    }

    public void setSequenceNumber(final long sequenceNumber) {
        pushResponse.setSequenceNumber(sequenceNumber);
    }

    protected PushRequest getPushRequest() {
        return pushRequest;
    }

    protected PushResponse getPushResponse() {
        return pushResponse;
    }
}
