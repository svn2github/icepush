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
