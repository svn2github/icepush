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
package org.icepush.http.standard;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Logger;

import org.icepush.http.Response;
import org.icepush.http.ResponseHandler;

public abstract class ContentTypeContentHandler implements ResponseHandler {
    private String mimeType;
    private String characterSet;

    protected ContentTypeContentHandler(String mimeType, String characterSet) {
        this.mimeType = mimeType;
        this.characterSet = characterSet;
    }

    public abstract void writeTo(Writer writer) throws IOException;

    public void respond(Response response) throws Exception {
        StringWriter writer = new StringWriter();
        writeTo(writer);
        writer.write("\n\n");
        writer.flush();

        byte[] content = writer.getBuffer().toString().getBytes(characterSet);
        response.setHeader("Content-Type", mimeType + "; charset=" + characterSet);

        //PUSH-315: setting the Content-Length causes a problem on Liferay 6.2 where
        //short XML responses to various push requests (e.g. <noop/>) are truncated
        //or missing entirely.

        //response.setHeader("Content-Length", content.length);

        OutputStream out = response.writeBody();
        out.write(content);
    }
}
