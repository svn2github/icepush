package org.icepush.servlet;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.icepush.Configuration;
import org.icepush.http.PushResponse;
import org.icepush.http.Response;

public class ServletPushResponse
extends ServletResponse
implements PushResponse, Response {
    private static final Logger LOGGER = Logger.getLogger(ServletPushResponse.class.getName());

    public ServletPushResponse(final HttpServletResponse response, final Configuration configuration)
    throws Exception {
        super(response, configuration);
    }

    public void setHeartbeatInterval(final long heartbeatInterval) {
        setHeader("ice.push.heartbeat", heartbeatInterval);
    }

    public void setHeartbeatTimestamp(final long heartbeatTimestamp) {
        setHeader("ice.push.heartbeatTimestamp", heartbeatTimestamp);
    }

    public void setSequenceNumber(final long sequenceNumber) {
        setHeader("ice.push.sequence", sequenceNumber);
    }
}
