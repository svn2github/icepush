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
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.http.Response;

public class ResponseProxy
implements Response {
    private static final Logger LOGGER = Logger.getLogger(ResponseProxy.class.getName());

    private final Response response;

    public ResponseProxy(final Response response) {
        this.response = response;
    }

    public void setStatus(final int code) {
        response.setStatus(code);
    }

    public void setHeader(final String name, final String value) {
        response.setHeader(name, value);
    }

    public void setHeader(final String name, final String[] values) {
        response.setHeader(name, values);
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

    public OutputStream writeBody()
    throws IOException {
        return response.writeBody();
    }

    public void writeBodyFrom(final InputStream in)
    throws IOException {
        response.writeBodyFrom(in);
    }

    protected Response getResponse() {
        return response;
    }
}
