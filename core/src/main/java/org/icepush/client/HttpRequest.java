/*
 * Copyright 2004-2014 ICEsoft Technologies Canada Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS
 * IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
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
