package org.icepush.http;

public interface PushResponse
extends Response {
    void setHeartbeatInterval(long heartbeatInterval);

    void setHeartbeatTimestamp(long heartbeatTimestamp);

    void setSequenceNumber(long sequenceNumber);
}
