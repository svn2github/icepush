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
package org.icepush;

import org.icepush.http.Response;
import org.icepush.http.ResponseHandler;
import org.icepush.http.standard.ResponseHandlerServer;

import java.io.InputStream;

public class CodeServer extends ResponseHandlerServer {
    public CodeServer(final String resourceName) {
        super(new ResponseHandler() {
            public void respond(Response response) throws Exception {
                response.setHeader("Content-Type", "text/javascript");
                InputStream code = CodeServer.class.getResourceAsStream("/META-INF/resources/" + resourceName);
                response.writeBodyFrom(code);
            }
        });
    }
}