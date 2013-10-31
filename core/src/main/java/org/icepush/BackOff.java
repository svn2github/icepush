package org.icepush;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.http.PushResponse;
import org.icepush.http.PushResponseHandler;
import org.icepush.http.standard.FixedXMLContentHandler;

public class BackOff
extends FixedXMLContentHandler
implements PushResponseHandler {
    private static final Logger LOGGER = Logger.getLogger(BackOff.class.getName());

    private final long delay;

    public BackOff(final long delay) {
        this.delay = delay;
    }

    public void respond(final PushResponse pushResponse)
    throws Exception {
        super.respond(pushResponse);
    }

    public void writeTo(final Writer writer)
    throws IOException {
        writer.write("<back-off delay=\"" + delay + "\"/>");
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Sending back-off - " + delay + "ms.");
        }
    }
}
