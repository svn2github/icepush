package org.icepush.http.standard;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.http.PushRequest;
import org.icepush.http.PushResponseHandler;
import org.icepush.http.PushServer;

public class PushResponseHandlerServer
implements PushServer {
    private static final Logger LOGGER = Logger.getLogger(PushResponseHandlerServer.class.getName());

    private final PushResponseHandler handler;

    public PushResponseHandlerServer(final PushResponseHandler handler) {
        this.handler = handler;
    }

    public void service(final PushRequest pushRequest)
    throws Exception {
        pushRequest.respondWith(handler);
    }

    public void shutdown() {
        // Do nothing.
    }
}
