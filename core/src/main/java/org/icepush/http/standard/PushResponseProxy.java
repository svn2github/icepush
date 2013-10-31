package org.icepush.http.standard;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.http.PushResponse;
import org.icepush.http.Response;

public class PushResponseProxy
extends ResponseProxy
implements PushResponse, Response {
    private static final Logger LOGGER = Logger.getLogger(PushResponseProxy.class.getName());

    private final PushResponse pushResponse;

    public PushResponseProxy(final PushResponse pushResponse) {
        super(pushResponse);
        this.pushResponse = pushResponse;
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

    protected PushResponse getPushResponse() {
        return pushResponse;
    }
}
