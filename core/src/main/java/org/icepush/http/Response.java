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
package org.icepush.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

public interface Response {

    void setStatus(int code);

    void setHeader(String name, String value);

    void setHeader(String name, String[] values);

    void setHeader(String name, Date value);

    void setHeader(String name, int value);

    void setHeader(String name, long value);

    OutputStream writeBody() throws IOException;

    void writeBodyFrom(InputStream in) throws IOException;
}
