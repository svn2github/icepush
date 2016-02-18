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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icepush.Configuration;
import org.icepush.http.PushResponseHandler;
import org.icepush.http.PushServer;
import org.icepush.util.Slot;

public class AsyncAdaptingServlet
implements PseudoServlet {
    private static final Logger LOGGER = Logger.getLogger(AsyncAdaptingServlet.class.getName());

    private static final AtomicBoolean LOGGING_ADAPTED = new AtomicBoolean(false);

    private final Configuration configuration;
    private final Slot heartbeatIntervalSlot;
    private final PushServer pushServer;

    public AsyncAdaptingServlet(
        final PushServer pushServer, final Slot heartbeatIntervalSlot, final Configuration configuration) {

        this.pushServer = pushServer;
        this.heartbeatIntervalSlot = heartbeatIntervalSlot;
        this.configuration = configuration;
        if (!LOGGING_ADAPTED.getAndSet(true)) {
            LOGGER.info("Using Servlet 3.0 AsyncContext");
        }
    }

    public void service(final HttpServletRequest request, final HttpServletResponse response)
    throws Exception {
        if (!request.isAsyncSupported()) {
            throw new EnvironmentAdaptingException();
        }
        getPushServer().service(new AsyncRequestResponse(request, response, getConfiguration()));
    }

    public void shutdown() {
        getPushServer().shutdown();
    }

    protected Configuration getConfiguration() {
        return configuration;
    }

    protected Slot getHeartbeatIntervalSlot() {
        return heartbeatIntervalSlot;
    }

    protected PushServer getPushServer() {
        return pushServer;
    }

    private class AsyncRequestResponse
    extends ServletPushRequestResponse {
        private final AsyncContext asyncContext;

        public AsyncRequestResponse(
            final HttpServletRequest request, final HttpServletResponse response, final Configuration configuration)
        throws Exception {
            super(request, response, configuration);
            if (request.isAsyncStarted()) {
                asyncContext = request.getAsyncContext();
            } else {
                asyncContext = request.startAsync();
            }
            getAsyncContext().setTimeout(getHeartbeatIntervalSlot().getLongValue() * 3);
        }

        public void respondWith(final PushResponseHandler handler)
        throws Exception {
            try {
                super.respondWith(handler);
            } finally {
                getAsyncContext().complete();
            }
        }

        protected AsyncContext getAsyncContext() {
            return asyncContext;
        }
    }
}
