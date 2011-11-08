/*
 * Version: MPL 1.1
 *
 * "The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is ICEfaces 1.5 open source software code, released
 * November 5, 2006. The Initial Developer of the Original Code is ICEsoft
 * Technologies Canada, Corp. Portions created by ICEsoft are Copyright (C)
 * 2004-2010 ICEsoft Technologies Canada, Corp. All Rights Reserved.
 *
 * Contributor(s): _____________________.
 *
 */

package org.icepush.servlet;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icepush.Configuration;
import org.icepush.http.ResponseHandler;
import org.icepush.http.Server;

public class AsyncAdaptingServlet implements PseudoServlet {
    private final static Logger log = Logger.getLogger(AsyncAdaptingServlet.class.getName());
    private Server server;
    private Configuration configuration;

    public AsyncAdaptingServlet(final Server server, final Configuration configuration) {
        this.server = server;
        this.configuration = configuration;
        log.info("Using Servlet 3.0 AsyncContext");
    }

    public void service(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        if (!request.isAsyncSupported()) {
            throw new EnvironmentAdaptingException();
        }
        AsyncRequestResponse requestResponse = new AsyncRequestResponse(request, response, configuration);
        server.service(requestResponse);
    }

    public void shutdown() {
        server.shutdown();
    }

    private class AsyncRequestResponse extends ServletRequestResponse {
        private AsyncContext asyncContext;

        public AsyncRequestResponse(final HttpServletRequest request, final HttpServletResponse response, final Configuration configuration) throws Exception {
            super(request, response, configuration);
            asyncContext = request.isAsyncStarted() ? request.getAsyncContext() : request.startAsync();
            asyncContext.setTimeout(configuration.getAttributeAsLong("heartbeatTimeout", 15000) * 2);
        }

        public void respondWith(final ResponseHandler handler) throws Exception {
            try {
                super.respondWith(handler);
            } finally {
                asyncContext.complete();
            }
        }
    }
}
