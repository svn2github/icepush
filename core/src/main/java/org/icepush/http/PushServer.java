package org.icepush.http;

public interface PushServer {
    void service(PushRequest pushRequest)
    throws Exception;

    void shutdown();
}
