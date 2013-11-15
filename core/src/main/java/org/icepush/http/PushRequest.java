package org.icepush.http;

import java.util.Set;

public interface PushRequest
extends Request {
    String getBrowserID();

    long getHeartbeatInterval()
    throws NumberFormatException;

    long getHeartbeatTimestamp()
    throws NumberFormatException;

    String getNotifyBackURI();

    Set<String> getPushIDSet();

    long getSequenceNumber()
    throws NumberFormatException;

    String getWindowID();

    void respondWith(PushResponseHandler handler)
    throws Exception;
}
