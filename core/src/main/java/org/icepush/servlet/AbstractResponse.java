package org.icepush.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.http.Response;

public abstract class AbstractResponse
implements Response {
    private static final Logger LOGGER = Logger.getLogger(AbstractResponse.class.getName());

    protected static void copy(final InputStream in, final OutputStream out)
    throws IOException {
        byte[] buf = new byte[4096];
        int len;
        while ((len = in.read(buf)) > -1) {
            out.write(buf, 0, len);
        }
    }

    protected static boolean ignoreHeader(final String name, final Object value) {
        return name == null || value == null;
    }
}
