package org.icepush;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.http.PushResponse;
import org.icepush.http.PushResponseHandler;
import org.icepush.http.standard.FixedXMLContentHandler;

public class ServerError
extends FixedXMLContentHandler
implements PushResponseHandler {
    private static final Logger LOGGER = Logger.getLogger(ServerError.class.getName());

    private final String message;

    public ServerError(final Throwable throwable) {
        this.message = throwable.getMessage();
    }

    public void respond(final PushResponse pushResponse)
    throws Exception {
        pushResponse.setStatus(503);
        super.respond(pushResponse);
    }

    @Override
    public void writeTo(final Writer writer)
    throws IOException {
        writer.write("<server-error message=\"");
        writer.write(message);
        writer.write("\"/>");
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Sending server-error - " + message);
        }
    }
}
