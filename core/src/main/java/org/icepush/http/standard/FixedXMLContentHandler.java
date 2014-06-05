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

import org.icepush.http.Response;

public abstract class FixedXMLContentHandler extends ContentTypeContentHandler {

    protected FixedXMLContentHandler() {
        super("text/xml", "UTF-8");
    }

    public void respond(Response response) throws Exception {
        response.setHeader("Cache-Control", new String[]{"no-cache", "no-store", "must-revalidate"});//HTTP 1.1
        response.setHeader("Pragma", "no-cache");//HTTP 1.0
        response.setHeader("Expires", 0);//prevents proxy caching
        super.respond(response);
    }
}
