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
package org.icepush.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icepush.Configuration;
import org.icepush.http.Request;
import org.icepush.http.Response;
import org.icepush.http.ResponseHandler;

public class ServletRequestResponse
implements Request, Response {
    private static Logger LOGGER = Logger.getLogger(ServletRequestResponse.class.getName());

    private Request request;
    private Response response;

    public ServletRequestResponse(
        final HttpServletRequest request, final HttpServletResponse response, final Configuration configuration)
    throws Exception {
        this(new ServletRequest(request, configuration), new ServletResponse(response, configuration));
    }

    public ServletRequestResponse(
        final ServletRequest request, final ServletResponse response)
    throws Exception {
        this.request = request;
        this.response = response;
    }

    public boolean containsParameter(final String name) {
        return request.containsParameter(name);
    }

    public void detectEnvironment(final Environment environment)
    throws Exception {
        request.detectEnvironment(environment);
    }

    public Object getAttribute(final String name) {
        return request.getAttribute(name);
    }

    public Cookie[] getCookies() {
        return request.getCookies();
    }

    public String getHeader(final String name) {
        return request.getHeader(name);
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

    public String[] getHeaderAsStrings(final String name) {
        return request.getHeaderAsStrings(name);
    }

    public String[] getHeaderNames() {
        return request.getHeaderNames();
    }

    public String getLocalAddr() {
        return request.getLocalAddr();
    }

    public String getLocalName() {
        return request.getLocalName();
    }

    public String getMethod() {
        return request.getMethod();
    }

    public String getParameter(final String name) {
        return request.getParameter(name);
    }

    public String getParameter(final String name, final String defaultValue) {
        return request.getParameter(name, defaultValue);
    }

    public boolean getParameterAsBoolean(final String name) {
        return request.getParameterAsBoolean(name);
    }

    public boolean getParameterAsBoolean(final String name, final boolean defaultValue) {
        return request.getParameterAsBoolean(name, defaultValue);
    }

    public int getParameterAsInteger(final String name) {
        return request.getParameterAsInteger(name);
    }

    public int getParameterAsInteger(final String name, final int defaultValue) {
        return request.getParameterAsInteger(name, defaultValue);
    }

    public long getParameterAsLong(final String name)
    throws NumberFormatException {
        return request.getParameterAsLong(name);
    }

    public long getParameterAsLong(final String name, final long defaultValue) {
        return request.getParameterAsLong(name, defaultValue);
    }

    public String[] getParameterAsStrings(final String name) {
        return request.getParameterAsStrings(name);
    }

    public String[] getParameterNames() {
        return request.getParameterNames();
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

    public URI getURI() {
        return request.getURI();
    }

    public InputStream readBody()
    throws IOException {
        return request.readBody();
    }

    public void readBodyInto(final OutputStream out)
    throws IOException {
        request.readBodyInto(out);
    }

    public void respondWith(final ResponseHandler handler)
    throws Exception {
        handler.respond(this);
    }

    public void setAttribute(final String name, final Object value) {
        request.setAttribute(name, value);
    }

    public void setHeader(final String name, final Date value) {
        response.setHeader(name, value);
    }

    public void setHeader(final String name, final int value) {
        response.setHeader(name, value);
    }

    public void setHeader(final String name, final long value) {
        response.setHeader(name, value);
    }

    public void setHeader(final String name, final String value) {
        response.setHeader(name, value);
    }

    public void setHeader(final String name, final String[] values) {
        response.setHeader(name, values);
    }

    public void setStatus(final int code) {
        response.setStatus(code);
    }

    public OutputStream writeBody()
    throws IOException {
        return response.writeBody();
    }

    public void writeBodyFrom(final InputStream in)
    throws IOException {
        response.writeBodyFrom(in);
    }

    protected Request getRequest() {
        return request;
    }

    protected Response getResponse() {
        return response;
    }
}
