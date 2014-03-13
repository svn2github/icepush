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

package org.icepush.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.icepush.Configuration;
import org.icepush.http.Request;
import org.icepush.http.ResponseHandler;

public class ServletRequest
extends AbstractRequest
implements Request {
    private static final Logger LOGGER = Logger.getLogger(ServletRequest.class.getName());

    private final static DateFormat DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private final Configuration configuration;
    private final boolean disableRemoteHostLookup;
    private final HttpServletRequest request;
    private final URI requestURI;

    public ServletRequest(final HttpServletRequest request, final Configuration configuration)
    throws Exception {
        this.request = request;
        this.configuration = configuration;
        this.disableRemoteHostLookup = this.configuration.getAttributeAsBoolean("disableRemoteHostLookup", false);
        String query = this.request.getQueryString();
        URI uri = null;
        while (null == uri) {
            try {
                uri = URI.create(this.request.getRequestURL().toString());
            } catch (final NullPointerException exception) {
                //TODO remove this catch block when GlassFish bug is addressed
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Null Protocol Scheme in request", exception);
                }
                uri =
                    URI.create(
                        "http://" + this.request.getServerName() + ":" + this.request.getServerPort() +
                            this.request.getRequestURI());
            }
        }
        requestURI = (query == null ? uri : URI.create(uri + "?" + query));
    }

    public boolean containsParameter(final String name) {
        return request.getParameter(name) != null;
    }

    public void detectEnvironment(final Environment environment)
    throws Exception {
    }

    public Object getAttribute(final String name) {
        return request.getAttribute(name);
    }

    public Cookie[] getCookies(){
        return request.getCookies();
    }

    public String getHeader(final String name) {
        return request.getHeader(name);
    }

    public Date getHeaderAsDate(final String name) {
        try {
            return DATE_FORMAT.parse(request.getHeader(name));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public int getHeaderAsInteger(final String name)
    throws NumberFormatException {
        return Integer.parseInt(request.getHeader(name));
    }

    public long getHeaderAsLong(final String name)
    throws NumberFormatException {
        return Long.parseLong(request.getHeader(name));
    }

    public String[] getHeaderAsStrings(final String name) {
        Enumeration<String> e = request.getHeaders(name);
        List<String> values = new ArrayList<String>();
        while (e.hasMoreElements()) {
            values.add(e.nextElement());
        }
        return values.toArray(new String[values.size()]);
    }

    public String[] getHeaderNames() {
        List<String> headerNames = new ArrayList<String>();
        Enumeration<String> e = request.getHeaderNames();
        while (e.hasMoreElements()) {
            headerNames.add(e.nextElement());
        }
        return headerNames.toArray(new String[headerNames.size()]);
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
        checkExistenceOf(name);
        return request.getParameter(name);
    }

    public String getParameter(final String name, final String defaultValue) {
        try {
            return getParameter(name);
        } catch (final Exception e) {
            return defaultValue;
        }
    }

    public boolean getParameterAsBoolean(final String name) {
        return Boolean.valueOf(getParameter(name));
    }

    public boolean getParameterAsBoolean(final String name, final boolean defaultValue) {
        try {
            return getParameterAsBoolean(name);
        } catch (final Exception e) {
            return defaultValue;
        }
    }

    public int getParameterAsInteger(final String name)
    throws NumberFormatException {
        return Integer.parseInt(getParameter(name));
    }

    public int getParameterAsInteger(final String name, final int defaultValue) {
        try {
            return getParameterAsInteger(name);
        } catch (final Exception e) {
            return defaultValue;
        }
    }

    public long getParameterAsLong(final String name)
    throws NumberFormatException {
        return Long.parseLong(getParameter(name));
    }

    public long getParameterAsLong(final String name, final long defaultValue) {
        try {
            return getParameterAsLong(name);
        } catch (final Exception exception) {
            return defaultValue;
        }
    }

    public String[] getParameterAsStrings(final String name) {
        checkExistenceOf(name);
        return request.getParameterValues(name);
    }

    public String[] getParameterNames() {
        Collection<String> result = request.getParameterMap().keySet();
        return result.toArray(new String[result.size()]);
    }

    public String getRemoteAddr() {
        return request.getRemoteAddr();
    }

    public String getRemoteHost() {
        if (!disableRemoteHostLookup) {
            LOGGER.info("Remote Host: " + request.getRemoteHost());
            return request.getRemoteHost();
        } else {
            LOGGER.info("Remote Host: " + request.getRemoteAddr());
            return request.getRemoteAddr();
        }
    }

    public String getServerName() {
        return request.getServerName();
    }

    public URI getURI() {
        return requestURI;
    }

    public InputStream readBody()
    throws IOException {
        return request.getInputStream();
    }

    public void readBodyInto(final OutputStream out)
    throws IOException {
        copy(readBody(), out);
    }

    public void respondWith(final ResponseHandler handler)
    throws Exception {
        throw new UnsupportedOperationException();
    }

    public void setAttribute(final String name, final Object value) {
        request.setAttribute(name, value);
    }

    protected Configuration getConfiguration() {
        return configuration;
    }

    protected HttpServletRequest getHttpServletRequest() {
        return request;
    }

    private void checkExistenceOf(final String name) {
        if (request.getParameter(name) == null) {

            // This block is removable once we find out why sometimes the request
            // object appears a little corrupted.

            String host = getRemoteHost();
            StringBuffer data = new StringBuffer("+ Request does not contain parameter '" + name + "' host: \n");
            data.append("  Originator: ").append(host).append("\n");
            data.append("  Path: ").append(requestURI.toString()).append("\n");

            Enumeration e = request.getParameterNames();
            String key;
            int i = 0;

            while (e.hasMoreElements()) {
                key = (String) e.nextElement();
                if (i == 0) {
                    data.append("  Available request parameters are: \n");
                }
                data.append("  - parameter name: ").append(key).append(", value: ").append(request.getParameter(key)).append("\n");
                i++;
            }
            if (i == 0) {
                data.append("   Request map is empty!\n");
            }

            data.append("- SRR hashcode: ").append(this.hashCode()).append(" Servlet request hash: ").append(request.hashCode());
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(data.toString());
            }
            // we can't just carry on. We seriously need those paramters ...
            throw new RuntimeException("Query does not contain parameter named: " + name);
        }
    }
}
