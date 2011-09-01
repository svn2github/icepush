package org.icepush.client;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpConnection {
    private static final Logger LOGGER = Logger.getLogger(HttpConnection.class.getName());

    private final CookieHandler cookieHandler;

    private HttpURLConnection connection;

    protected HttpConnection(final CookieHandler cookieHandler) {
        this.cookieHandler = cookieHandler;
    }

    public void close() {
        if (connection != null) {
            connection.disconnect();
            connection = null;
        }
    }

    public synchronized void send(final HttpRequest request)
    throws IOException, MalformedURLException {
        // throws MalformedURLException, IOException
        connection = (HttpURLConnection)request.getRequestURI().toURL().openConnection();
        try {
            Map<String, List<String>> _cookieHeaderMap =
                cookieHandler.get(request.getRequestURI(), connection.getRequestProperties());
            for (String _cookie : _cookieHeaderMap.get("Cookie")) {
                connection.addRequestProperty("Cookie", _cookie);
            }
            if (request.getMethod() == HttpRequest.Method.POST) {
                connection.setDoOutput(true);
                // throws IOException
                OutputStream _out = connection.getOutputStream();
                try {
                    // throws IOException
                    _out.write(request.getEntityBody());
                    // throws IOException
                    _out.flush();
                } finally {
                    // throws IOException
                    _out.close();
                }
            }
            // throws IOException
            InputStream _in = connection.getInputStream();
            cookieHandler.put(request.getRequestURI(), connection.getHeaderFields());
            try {
                ByteArrayOutputStream _out = new ByteArrayOutputStream();
                int _byte;
                while ((_byte = _in.read()) != -1) {
                    _out.write(_byte);
                }
                request.onResponse(
                    new HttpResponse(
                        connection.getResponseCode(),                                                     // Status-Code
                        connection.getResponseMessage(),                                                // Reason-Phrase
                        connection.getHeaderFields(),
                        _out.toByteArray()));                                                             // Entity-Body
                // todo: Change logging level to FINE or FINEST.
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.log(
                        Level.INFO,
                        "\r\n\r\n" +
                        "HTTP Request:\r\n\r\n" + request + "\r\n\r\n" +
                        "HTTP Response:\r\n\r\n" + request.getResponse() + "\r\n");
                }
            } catch (RuntimeException exception) {
                LOGGER.log(Level.SEVERE, "", exception);
                throw exception;
            } finally {
                // throws IOException
                _in.close();
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
                connection = null;
            }
            // todo: Remove this logging.
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, "Sending " + request.getRequestURI() + ":: Done!");
            }
        }
    }
}
