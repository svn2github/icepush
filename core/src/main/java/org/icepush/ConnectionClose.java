package org.icepush;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.http.PushResponse;
import org.icepush.http.PushResponseHandler;

public class ConnectionClose
implements PushResponseHandler {
    private static final Logger LOGGER = Logger.getLogger(ConnectionClose.class.getName());

    private final String reason;

    public ConnectionClose(final String reason) {
        this.reason = reason;
    }

    public void respond(final PushResponse pushResponse)
    throws Exception {
        //let the bridge know that this blocking connection should not be re-initialized
        pushResponse.setHeader("X-Connection", "close");
        pushResponse.setHeader("X-Connection-reason", reason);
        pushResponse.setHeader("Content-Length", 0);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Close current blocking connection.");
        }
        pushResponse.writeBody().close();
    }
}
