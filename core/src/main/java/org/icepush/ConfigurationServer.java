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
import static org.icepush.LocalPushGroupManager.DEFAULT_CLOUDPUSHID_TIMEOUT;

import org.icepush.http.*;
import org.icepush.http.standard.FixedXMLContentHandler;
import org.icepush.util.Slot;

public class ConfigurationServer implements PushServer {
    private static final Logger log = Logger.getLogger(ConfigurationServer.class.getName());
    private static final String defaultServerErrorRetries = "1000 2000 4000";
    private static final String defaultNetworkErrorRetries = "1 1 1 2 2 3";
    private static final int defaultEmptyResponseRetries = 3;
    public static final int DefaultHeartbeatTimeout = 15000;
    private static boolean pushConfigLogged = false;
    private Slot heartbeatInterval;
    private Configuration configuration;
    private PushServer server;
    private boolean nonDefaultConfiguration;

    private boolean redirect;
    private final String contextPath;
    private final String serverErrorRetries;
    private final String networkErrorRetries;
    private final int emptyResponseRetries;

    public ConfigurationServer(Slot heartbeatInterval, final ServletContext servletContext, final Configuration configuration, final PushServer server) {
        this.heartbeatInterval = heartbeatInterval;
        this.server = server;
        this.configuration = configuration;
        contextPath = normalizeContextPath(this.configuration.getAttribute("contextPath", (String)servletContext.getAttribute("contextPath")));
        //PUSH-218: temporarily disabling modification of the context parameter
        serverErrorRetries = this.configuration.getAttribute("serverErrorRetryTimeouts", defaultServerErrorRetries);
        networkErrorRetries = this.configuration.getAttribute("networkErrorRetryTimeouts", defaultNetworkErrorRetries);
        emptyResponseRetries = this.configuration.getAttributeAsInteger("emptyResponseRetries", defaultEmptyResponseRetries);
    
        //Log ICEpush Configuration
        if (!pushConfigLogged) {
				StringBuilder info = new StringBuilder();
			configuration.logLong("org.icepush.cloudPushIdTimeout", configuration.getAttributeAsLong("cloudPushIdTimeout", LocalPushGroupManager.DEFAULT_CLOUDPUSHID_TIMEOUT), LocalPushGroupManager.DEFAULT_CLOUDPUSHID_TIMEOUT, info);
				configuration.logString("org.icepush.contextPath", contextPath, null, info);
				configuration.logBoolean("org.icepush.disableRemoteHostLookup", configuration.getAttributeAsBoolean("disableRemoteHostLookup", false), false, info);
				configuration.logLong("org.icepush.emptyResponseRetries", emptyResponseRetries, defaultEmptyResponseRetries, info);
				configuration.logLong("org.icepush.groupTimeout", configuration.getAttributeAsLong("groupTimeout", LocalPushGroupManager.DEFAULT_GROUP_TIMEOUT), LocalPushGroupManager.DEFAULT_GROUP_TIMEOUT, info);
				configuration.logString("org.icepush.networkErrorRetryTimeouts", networkErrorRetries, defaultNetworkErrorRetries, info);
				configuration.logLong("org.icepush.heartbeatTimeout", configuration.getAttributeAsLong("heartbeatTimeout", 15000), 15000, info);
				configuration.logLong("org.icepush.notificationQueueSize", configuration.getAttributeAsLong("notificationQueueSize", LocalPushGroupManager.DEFAULT_NOTIFICATIONQUEUE_SIZE), LocalPushGroupManager.DEFAULT_NOTIFICATIONQUEUE_SIZE, info);
			configuration.logLong("org.icepush.notificationStormLoopInterval", configuration.getAttributeAsLong("notificationStormLoopInterval", PushStormDetectionServer.DefaultLoopInterval), PushStormDetectionServer.DefaultLoopInterval, info);
			configuration.logLong("org.icepush.notificationStormMaximumRequests", configuration.getAttributeAsLong("notificationStormMaximumRequests", PushStormDetectionServer.DefaultMaxTightLoopRequests), PushStormDetectionServer.DefaultMaxTightLoopRequests, info);
			configuration.logLong("org.icepush.notificationStormBackOffInterval", configuration.getAttributeAsLong("notificationStormBackOffInterval", PushStormDetectionServer.DefaultBackoffInterval), PushStormDetectionServer.DefaultBackoffInterval, info);
				configuration.logLong("org.icepush.pushIdTimeout", configuration.getAttributeAsLong("pushIdTimeout", LocalPushGroupManager.DEFAULT_PUSHID_TIMEOUT), LocalPushGroupManager.DEFAULT_PUSHID_TIMEOUT, info);
				configuration.logString("org.icepush.serverErrorRetryTimeouts", serverErrorRetries, defaultServerErrorRetries, info);
				final boolean isARPEnabled = isAsyncARPAvailable();
						configuration.logBoolean("org.icepush.useAsyncContext", configuration.getAttributeAsBoolean("useAsyncContext", isARPEnabled), isARPEnabled, info);
				log.info("ICEpush Configuration: \n" + info); 
				pushConfigLogged = true;
		}
	
        //always redirect if the request comes to this context path
        redirect = contextPath != null && !servletContext.getContextPath().equals(contextPath);
        nonDefaultConfiguration =
                emptyResponseRetries != defaultEmptyResponseRetries ||
                !serverErrorRetries.equals(defaultServerErrorRetries) ||
                !networkErrorRetries.equals(defaultNetworkErrorRetries) ||
                contextPath != null;

    }

    private static String normalizeContextPath(String path) {
        if (path == null) {
            return null;
        } else {
            return path.startsWith("/") ? path : "/" + path;
        }
    }

    private boolean isAsyncARPAvailable() {
        try {
            this.getClass().getClassLoader().loadClass("javax.servlet.AsyncContext");
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
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
                    (!networkErrorRetries.equals(defaultNetworkErrorRetries) ?
                            " networkErrorRetryTimeouts=\"" + networkErrorRetries + "\"" : "") +
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
