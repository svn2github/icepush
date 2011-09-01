package org.icepush.client;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class HttpRequest
extends HttpMessage {
    private static final Logger LOGGER = Logger.getLogger(HttpRequest.class.getName());

    protected static final String CR_LF = "\r\n";

    public static enum Method {
        GET,
        POST
    }

    private final Method method;
    private final URI requestURI;

    private HttpResponse response;

    protected HttpRequest(final Method method, final URI requestURI) {
        this(method, requestURI, new byte[0]);
    }

    protected HttpRequest(final Method method, final URI requestURI, final Map<String, List<String>> headerMap) {
        this(method, requestURI, headerMap, new byte[0]);
    }

    protected HttpRequest(
        final Method method, final URI requestURI, final Map<String, List<String>> headerMap, final byte[] entityBody) {

        super(headerMap, entityBody);
        this.method = method;
        this.requestURI = requestURI;
    }

    protected HttpRequest(final Method method, final URI requestURI, final byte[] entityBody) {
        super(entityBody);
        this.method = method;
        this.requestURI = requestURI;
    }

    public Method getMethod() {
        return method;
    }

    public URI getRequestURI() {
        return requestURI;
    }

    public HttpResponse getResponse() {
        return response;
    }

    public void onResponse(final HttpResponse response) {
        this.response = response;
    }

    public String toString() {
        Map<String, List<String>> _headerMap = getHeaders();
        StringBuilder _request = new StringBuilder();
        _request.append("[").append(method).append(' ').append(requestURI).append("]\r\n");
        for (String _fieldName : _headerMap.keySet()) {
            if (_fieldName != null) {
                for (String _fieldValue : _headerMap.get(_fieldName)) {
                    _request.append(_fieldName).append(": ").append(_fieldValue).append("\r\n");
                }
            }
        }
        _request.
            append("\r\n").
            append(getEntityBodyAsString());
        return _request.toString();
    }
}
