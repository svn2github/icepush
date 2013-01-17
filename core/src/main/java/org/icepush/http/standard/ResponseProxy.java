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

import org.icepush.http.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;


public class ResponseProxy implements Response {
    protected Response response;

    public ResponseProxy(Response response) {
        this.response = response;
    }

    public void setStatus(int code) {
        response.setStatus(code);
    }

    public void setHeader(String name, String value) {
        response.setHeader(name, value);
    }

    public void setHeader(String name, String[] values) {
        response.setHeader(name, values);
    }

    public void setHeader(String name, Date value) {
        response.setHeader(name, value);
    }

    public void setHeader(String name, int value) {
        response.setHeader(name, value);
    }

    public void setHeader(String name, long value) {
        response.setHeader(name, value);
    }

    public OutputStream writeBody() throws IOException {
        return response.writeBody();
    }

    public void writeBodyFrom(InputStream in) throws IOException {
        response.writeBodyFrom(in);
    }
}
