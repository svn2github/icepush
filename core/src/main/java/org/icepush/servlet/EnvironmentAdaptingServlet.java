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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icepush.Configuration;
import org.icepush.http.PushServer;

public class EnvironmentAdaptingServlet implements PseudoServlet {
    private static Logger log = Logger.getLogger(EnvironmentAdaptingServlet.class.getName());
    private PseudoServlet servlet;
    private PushServer pushServer;
    private Configuration configuration;

    public EnvironmentAdaptingServlet(final PushServer pushServer, final Configuration configuration) {
        this.pushServer = pushServer;
        this.configuration = configuration;
        if (configuration.getAttributeAsBoolean("useAsyncContext", isAsyncARPAvailable())) {
            log.log(Level.INFO, "Adapting to Servlet 3.0 AsyncContext environment");
            servlet = new AsyncAdaptingServlet(this.pushServer, this.configuration);
        } else {
            log.log(Level.INFO, "Adapting to Thread Blocking environment");
            servlet = new ThreadBlockingAdaptingServlet(this.pushServer, this.configuration);
        }
    }

    public void service(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        try {
            servlet.service(request, response);
        } catch (EnvironmentAdaptingException exception) {
            log.log(Level.INFO, "Falling back to Thread Blocking environment");
            servlet = new ThreadBlockingAdaptingServlet(pushServer, configuration);
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
