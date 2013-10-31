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

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icepush.Configuration;
import org.icepush.PushContext;
import org.icepush.http.standard.FixedXMLContentHandler;

public class ConfigurationServlet implements PseudoServlet {
    private static final Logger log = Logger.getLogger(ConfigurationServlet.class.getName());
    private static final String defaultServerErrorRetries = "1000 2000 4000";
    private static final int defaultEmptyResponseRetries = 3;
    public static final int DefaultHeartbeatTimeout = 15000;

    private Configuration configuration;
    private PseudoServlet pseudoServlet;
    private boolean nonDefaultConfiguration;

    private FixedXMLContentHandler configureBridge;
    private boolean redirect;

    public ConfigurationServlet(final PushContext context, final ServletContext servletContext, final Configuration configuration, final PseudoServlet pseudoServlet) {
        this.pseudoServlet = pseudoServlet;
        this.configuration = configuration;
        String contextPath = normalizeContextPath(this.configuration.getAttribute("contextPath", (String)servletContext.getAttribute("contextPath")));
        //PUSH-218: temporarily disabling modification of the context parameter
        long heartbeatTimeout = this.configuration.getAttributeAsLong("heartbeatTimeout", DefaultHeartbeatTimeout);
        String serverErrorRetries = this.configuration.getAttribute("serverErrorRetryTimeouts", defaultServerErrorRetries);
        int emptyResponseRetries = this.configuration.getAttributeAsInteger("emptyResponseRetries", defaultEmptyResponseRetries);

        String configurationMessage = "<configuration" +
                (heartbeatTimeout != DefaultHeartbeatTimeout ?
                        " heartbeatTimeout=\"" + heartbeatTimeout + "\"" : "") +
                (emptyResponseRetries != defaultEmptyResponseRetries ?
                        " emptyResponseRetries=\"" + emptyResponseRetries + "\"" : "") +
                (!serverErrorRetries.equals(defaultServerErrorRetries) ?
                        " serverErrorRetryTimeouts=\"" + serverErrorRetries + "\"" : "") +
                (contextPath != null ?
                        " blockingConnectionURI=\"" + contextPath + "/listen.icepush\"" : "") +
                (contextPath != null ?
                        " contextPath=\"" + contextPath + "\"" : "") +
                "/>";
        //always redirect if the request comes to this context path
        redirect = contextPath != null && !servletContext.getContextPath().equals(contextPath);
        nonDefaultConfiguration = configurationMessage.length() != "<configuration/>".length();
        configureBridge = new ConfigureBridge(configurationMessage);
    }

    private static String normalizeContextPath(String path) {
        if (path == null) {
            return null;
        } else {
            return path.startsWith("/") ? path : "/" + path;
        }
    }

    public void service(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        ServletRequestResponse requestResponse = new ServletRequestResponse(request, response, configuration);
        if ((redirect || requestResponse.containsParameter("ice.sendConfiguration")) && nonDefaultConfiguration) {
            requestResponse.respondWith(configureBridge);
        } else {
            pseudoServlet.service(request, response);
        }
    }

    public void shutdown() {
        pseudoServlet.shutdown();
    }

    private static class ConfigureBridge extends FixedXMLContentHandler {
        private String configurationMessage;

        private ConfigureBridge(String configurationMessage) {
            this.configurationMessage = configurationMessage;
        }

        public void writeTo(Writer writer) throws IOException {
            writer.write(configurationMessage);
            log.fine("Re-configured bridge.");
        }
    }
}
