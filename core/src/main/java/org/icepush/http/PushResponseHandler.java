package org.icepush.http;

public interface PushResponseHandler {
    void respond(PushResponse pushResponse)
    throws Exception;
}
