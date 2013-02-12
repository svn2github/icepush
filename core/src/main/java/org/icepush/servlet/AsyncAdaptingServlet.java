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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icepush.Configuration;
import org.icepush.ConfigurationServer;
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

            //PUSH-218: temporarily disabling modification of the context parameter
            long heartbeatTimeout = configuration.getAttributeAsLong("heartbeatTimeout", ConfigurationServer.DefaultHeartbeatTimeout);
            asyncContext.setTimeout(heartbeatTimeout * 2);
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
