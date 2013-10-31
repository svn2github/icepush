package org.icepush;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.http.PushResponse;
import org.icepush.http.PushResponseHandler;
import org.icepush.http.standard.FixedXMLContentHandler;

public class Noop
extends FixedXMLContentHandler
implements PushResponseHandler {
    private static final Logger LOGGER = Logger.getLogger(Noop.class.getName());

    private final String reason;

    public Noop(final String reason) {
        this.reason = reason;
    }

    public void respond(final PushResponse pushResponse)
    throws Exception {
        pushResponse.setHeader("X-Connection-reason", reason);
        super.respond(pushResponse);
    }

    @Override
    public void writeTo(final Writer writer)
    throws IOException {
        writer.write("<noop/>");
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Sending NoOp.");
        }
    }
}
