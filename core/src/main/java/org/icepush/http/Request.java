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
package org.icepush.http;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Date;

public interface Request {
    Object getAttribute(String name);

    Cookie[] getCookies();

    String getMethod();

    URI getURI();

    String[] getHeaderNames();

    String getHeader(String name);

    String[] getHeaderAsStrings(String name);

    Date getHeaderAsDate(String name);

    int getHeaderAsInteger(String name)
    throws NumberFormatException;

    long getHeaderAsLong(String name)
    throws NumberFormatException;

    boolean containsParameter(String name);

    String[] getParameterNames();

    String getParameter(String name);

    String[] getParameterAsStrings(String name);

    int getParameterAsInteger(String name)
    throws NumberFormatException;

    long getParameterAsLong(String name)
    throws NumberFormatException;

    long getParameterAsLong(String name, long defaultValue);

    boolean getParameterAsBoolean(String name);

    String getParameter(String name, String defaultValue);

    int getParameterAsInteger(String name, int defaultValue);

    boolean getParameterAsBoolean(String name, boolean defaultValue);

    String getLocalAddr();

    String getLocalName();

    String getRemoteAddr();

    String getRemoteHost();

    String getServerName();

    InputStream readBody() throws IOException;

    void readBodyInto(OutputStream out) throws IOException;

    void respondWith(ResponseHandler handler) throws Exception;

    void detectEnvironment(Environment environment) throws Exception;

    void setAttribute(String name, Object value);

    //avoid runtime dependency on Portlet interfaces,
    //and for the symmetry's sake, same for the Servlet interfaces
    interface Environment {

        void servlet(Object request, Object response) throws Exception;

        void portlet(Object request, Object response, Object config) throws Exception;
    }
}
