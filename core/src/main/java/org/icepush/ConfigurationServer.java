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
package org.icepush;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.icepush.http.*;
import org.icepush.http.standard.FixedXMLContentHandler;
import org.icepush.util.Slot;

public class ConfigurationServer implements PushServer {
    private static final Logger log = Logger.getLogger(ConfigurationServer.class.getName());
    private static final String defaultServerErrorRetries = "1000 2000 4000";
    private static final int defaultEmptyResponseRetries = 3;
    public static final int DefaultHeartbeatTimeout = 15000;
    private Slot heartbeatInterval;
    private Configuration configuration;
    private PushServer server;
    private boolean nonDefaultConfiguration;

    private boolean redirect;
    private final String contextPath;
    private final String serverErrorRetries;
    private final int emptyResponseRetries;

    public ConfigurationServer(Slot heartbeatInterval, final ServletContext servletContext, final Configuration configuration, final PushServer server) {
        this.heartbeatInterval = heartbeatInterval;
        this.server = server;
        this.configuration = configuration;
        contextPath = normalizeContextPath(this.configuration.getAttribute("contextPath", (String)servletContext.getAttribute("contextPath")));
        //PUSH-218: temporarily disabling modification of the context parameter
        serverErrorRetries = this.configuration.getAttribute("serverErrorRetryTimeouts", defaultServerErrorRetries);
        emptyResponseRetries = this.configuration.getAttributeAsInteger("emptyResponseRetries", defaultEmptyResponseRetries);

        //always redirect if the request comes to this context path
        redirect = contextPath != null && !servletContext.getContextPath().equals(contextPath);
        nonDefaultConfiguration =
                emptyResponseRetries != defaultEmptyResponseRetries ||
                !serverErrorRetries.equals(defaultServerErrorRetries) ||
                contextPath != null;

    }

    private static String normalizeContextPath(String path) {
        if (path == null) {
            return null;
        } else {
            return path.startsWith("/") ? path : "/" + path;
        }
    }

    public void service(PushRequest request) throws Exception {
        if ((redirect || request.containsParameter("ice.sendConfiguration")) &&
                (nonDefaultConfiguration || heartbeatInterval.getLongValue() != DefaultHeartbeatTimeout)) {
            String configurationMessage = "<configuration" +
                    (heartbeatInterval.getLongValue() != DefaultHeartbeatTimeout ?
                            " heartbeatTimeout=\"" + heartbeatInterval.getLongValue() + "\"" : "") +
                    (emptyResponseRetries != defaultEmptyResponseRetries ?
                            " emptyResponseRetries=\"" + emptyResponseRetries + "\"" : "") +
                    (!serverErrorRetries.equals(defaultServerErrorRetries) ?
                            " serverErrorRetryTimeouts=\"" + serverErrorRetries + "\"" : "") +
                    (contextPath != null ?
                            " blockingConnectionURI=\"" + contextPath + "/listen.icepush\"" : "") +
                    (contextPath != null ?
                            " contextPath=\"" + contextPath + "\"" : "") +
                    "/>";


            request.respondWith((PushResponseHandler) new ConfigureBridge(configurationMessage));
        } else {
            server.service(request);
        }
    }

    public void shutdown() {
        server.shutdown();
    }

    private static class ConfigureBridge extends FixedXMLContentHandler implements PushResponseHandler {
        private String configurationMessage;

        private ConfigureBridge(String configurationMessage) {
            this.configurationMessage = configurationMessage;
        }

        public void writeTo(Writer writer) throws IOException {
            writer.write(configurationMessage);
            log.fine("Re-configured bridge.");
        }

        public void respond(PushResponse pushResponse) throws Exception {
            super.respond(pushResponse);
        }
    }
}
