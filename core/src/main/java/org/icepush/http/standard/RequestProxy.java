/*
 * Copyright 2004-2013 ICEsoft Technologies Canada Corp.
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
 *
 */
package org.icepush.http.standard;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.http.Request;
import org.icepush.http.ResponseHandler;

public class RequestProxy
implements Request {
    private static final Logger LOGGER = Logger.getLogger(RequestProxy.class.getName());

    private final Request request;

    public RequestProxy(final Request request) {
        this.request = request;
    }

    public javax.servlet.http.Cookie[] getCookies() {
        return request.getCookies();
    }

    public String getMethod() {
        return request.getMethod();
    }

    public URI getURI() {
        return request.getURI();
    }

    public String[] getHeaderNames() {
        return request.getHeaderNames();
    }

    public String getHeader(final String name) {
        return request.getHeader(name);
    }

    public String[] getHeaderAsStrings(final String name) {
        return request.getHeaderAsStrings(name);
    }

    public Date getHeaderAsDate(final String name) {
        return request.getHeaderAsDate(name);
    }

    public int getHeaderAsInteger(final String name) {
        return request.getHeaderAsInteger(name);
    }

    public long getHeaderAsLong(final String name) {
        return request.getHeaderAsLong(name);
    }

    public boolean containsParameter(final String name) {
        return request.containsParameter(name);
    }

    public String[] getParameterNames() {
        return request.getParameterNames();
    }

    public String getParameter(final String name) {
        return request.getParameter(name);
    }

    public String[] getParameterAsStrings(final String name) {
        return request.getParameterAsStrings(name);
    }

    public int getParameterAsInteger(final String name) {
        return request.getParameterAsInteger(name);
    }

    public boolean getParameterAsBoolean(final String name) {
        return request.getParameterAsBoolean(name);
    }

    public String getParameter(final String name, final String defaultValue) {
        return request.getParameter(name, defaultValue);
    }

    public int getParameterAsInteger(final String name, final int defaultValue) {
        return request.getParameterAsInteger(name, defaultValue);
    }

    public boolean getParameterAsBoolean(final String name, final boolean defaultValue) {
        return request.getParameterAsBoolean(name, defaultValue);
    }

    public String getLocalAddr() {
        return request.getLocalAddr();
    }

    public String getLocalName() {
        return request.getLocalName();
    }

    public String getRemoteAddr() {
        return request.getRemoteAddr();
    }

    public String getRemoteHost() {
        return request.getRemoteHost();
    }

    public String getServerName() {
        return request.getServerName();
    }

    public InputStream readBody() throws IOException {
        return request.readBody();
    }

    public void readBodyInto(final OutputStream out)
    throws IOException {
        request.readBodyInto(out);
    }

    public void respondWith(final ResponseHandler handler)
    throws Exception {
        request.respondWith(handler);
    }

    public void detectEnvironment(final Environment environment)
    throws Exception {
        request.detectEnvironment(environment);
    }

    protected Request getRequest() {
        return request;
    }
}
