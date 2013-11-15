package org.icepush.http.standard;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.http.PushRequest;
import org.icepush.http.PushResponseHandler;
import org.icepush.http.Request;

public class PushRequestProxy
extends RequestProxy
implements PushRequest, Request {
    private static final Logger LOGGER = Logger.getLogger(PushRequestProxy.class.getName());

    private final PushRequest pushRequest;

    public PushRequestProxy(final PushRequest pushRequest) {
        super(pushRequest);
        this.pushRequest = pushRequest;
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

    public void respondWith(final PushResponseHandler handler)
    throws Exception {
        pushRequest.respondWith(handler);
    }

    protected PushRequest getPushRequest() {
        return pushRequest;
    }
}
