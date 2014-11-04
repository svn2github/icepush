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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icepush.Configuration;
import org.icepush.http.PushServer;
import org.icepush.util.Slot;

public class EnvironmentAdaptingServlet
implements PseudoServlet {
    private static final Logger LOGGER = Logger.getLogger(EnvironmentAdaptingServlet.class.getName());

    private static final AtomicBoolean LOGGING_ADAPTING = new AtomicBoolean(false);
    private static final AtomicBoolean LOGGING_REVERTING = new AtomicBoolean(false);

    private final Configuration configuration;
    private final Slot heartbeatInterval;
    private final PushServer pushServer;

    private PseudoServlet servlet;

    public EnvironmentAdaptingServlet(
        final PushServer pushServer, final Slot heartbeatInterval, final Configuration configuration) {

        this.pushServer = pushServer;
        this.heartbeatInterval = heartbeatInterval;
        this.configuration = configuration;
        if (configuration.getAttributeAsBoolean("useAsyncContext", isAsyncARPAvailable())) {
            if (!LOGGING_ADAPTING.getAndSet(true)) {
                LOGGER.log(Level.INFO, "Adapting to Servlet 3.0 AsyncContext environment");
            }
            servlet = new AsyncAdaptingServlet(this.pushServer, this.heartbeatInterval, this.configuration);
        } else {
            if (!LOGGING_ADAPTING.getAndSet(true)) {
                LOGGER.log(Level.INFO, "Adapting to Thread Blocking environment");
            }
            servlet = new ThreadBlockingAdaptingServlet(this.pushServer, this.heartbeatInterval, this.configuration);
        }
    }

    public void service(final HttpServletRequest request, final HttpServletResponse response)
    throws Exception {
        try {
            servlet.service(request, response);
        } catch (EnvironmentAdaptingException exception) {
            if (!LOGGING_REVERTING.getAndSet(true)) {
                LOGGER.log(Level.INFO, "Falling back to Thread Blocking environment");
            }
            servlet = new ThreadBlockingAdaptingServlet(pushServer, heartbeatInterval, configuration);
            servlet.service(request, response);
        }
    }

    public void shutdown() {
        servlet.shutdown();
    }

    private boolean isAsyncARPAvailable() {
        try {
            this.getClass().getClassLoader().loadClass("javax.servlet.AsyncContext");
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }
}
