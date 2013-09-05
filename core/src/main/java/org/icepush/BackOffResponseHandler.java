package org.icepush;

import org.icepush.http.standard.FixedXMLContentHandler;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BackOffResponseHandler extends FixedXMLContentHandler {
    private static final Logger LOGGER = Logger.getLogger(BackOffResponseHandler.class.getName());
    private long delay;

    public BackOffResponseHandler(long delay) {
        this.delay = delay;
    }

    public void writeTo(Writer writer) throws IOException {
        writer.write("<back-off delay=\"" + delay + "\"/>");
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Sending back-off - " + delay + "ms.");
        }
    }
}
