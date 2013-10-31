package org.icepush.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.icepush.Configuration;
import org.icepush.http.Response;

public class ServletResponse
extends AbstractResponse
implements Response {
    private static final Logger LOGGER = Logger.getLogger(ServletResponse.class.getName());

    private static Pattern HEADER_FIXER = null;
    static {
        HEADER_FIXER = Pattern.compile("[\r\n]");
    }

    private final Configuration configuration;
    private final HttpServletResponse response;

    public ServletResponse(final HttpServletResponse response, final Configuration configuration)
    throws Exception {
        this.response = response;
        this.configuration = configuration;
    }

    public void setHeader(final String name, final Date value) {
        if (ignoreHeader(name, value)) {
            return;
        }
        response.setDateHeader(name, value.getTime());
    }

    public void setHeader(final String name, final int value) {
        response.setIntHeader(name, value);
    }

    public void setHeader(final String name, final long value) {
        response.setHeader(name, String.valueOf(value));
    }

    public void setHeader(final String name, final String value) {
        if (ignoreHeader(name, value)) {
            return;
        }
        //CR and LF embedded in headers can corrupt the HTTP response
        String _value = HEADER_FIXER.matcher(value).replaceAll("");
        if ("Content-Type".equals(name)) {
            response.setContentType(_value);
        } else if ("Content-Length".equals(name)) {
            response.setContentLength(Integer.parseInt(_value));
        } else {
            response.setHeader(name, _value);
        }
    }

    public void setHeader(final String name, final String[] values) {
        if (ignoreHeader(name, values)) {
            return;
        }
        for (final String _value : values) {
            response.addHeader(name, HEADER_FIXER.matcher(_value).replaceAll(""));
        }
    }

    public void setStatus(final int code) {
        response.setStatus(code);
    }

    public OutputStream writeBody()
    throws IOException {
        return response.getOutputStream();
    }

    public void writeBodyFrom(final InputStream in)
    throws IOException {
        OutputStream _out = writeBody();
        try {
            copy(in, _out);
        } finally {
            try {
                in.close();
            } finally {
                _out.close();
            }
        }
    }

    protected Configuration getConfiguration() {
        return configuration;
    }

    protected HttpServletResponse getHttpServletResponse() {
        return response;
    }
}
