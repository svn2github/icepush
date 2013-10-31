package org.icepush.http;

import java.util.Set;

public interface PushRequest
extends Request {
    String getBrowserID();

    long getHeartbeatTimestamp();

    String getNotifyBackURI();

    Set<String> getPushIDSet();

    long getSequenceNumber();

    String getWindowID();

    void respondWith(PushResponseHandler handler)
    throws Exception;
}
