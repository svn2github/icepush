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

import java.util.Map;
import java.util.Timer;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icepush.BlockingConnectionServer;
import org.icepush.Configuration;
import org.icepush.PushConfiguration;
import org.icepush.PushContext;
import org.icepush.PushGroupManager;
import org.icepush.PushStormDetectionServer;
import org.icepush.SequenceTaggingServer;
import org.icepush.http.Server;
import org.icepush.util.Slot;

public class BrowserBoundServlet extends PathDispatcher {
    private final static Logger log = Logger.getLogger(BrowserBoundServlet.class.getName());

    private static final Pattern NAME_VALUE = Pattern.compile("\\=");

    protected String browserID;
    protected PushContext pushContext;
    protected ServletContext servletContext;
    protected Timer monitoringScheduler;
    protected Configuration configuration;
    protected boolean terminateBlockingConnectionOnShutdown;

    public BrowserBoundServlet(
        final String browserID, final PushContext pushContext, final ServletContext servletContext,
        final Timer monitoringScheduler, final Configuration configuration,
        boolean terminateBlockingConnectionOnShutdown) {

        this.browserID = browserID;
        this.pushContext = pushContext;
        this.servletContext = servletContext;
        this.monitoringScheduler = monitoringScheduler;
        this.configuration = configuration;
        this.terminateBlockingConnectionOnShutdown = terminateBlockingConnectionOnShutdown;

        dispatchOn(".*listen\\.icepush", new EnvironmentAdaptingServlet(createBlockingConnectionServer(), configuration));
        dispatchOn(".*create-push-id\\.icepush", new CreatePushID());
        dispatchOn(".*notify\\.icepush", new NotifyPushID());
        dispatchOn(".*add-group-member\\.icepush", new AddGroupMember());
        dispatchOn(".*remove-group-member\\.icepush", new RemoveGroupMember());
    }

    protected Server createBlockingConnectionServer() {
        Slot heartbeatInterval =
            new Slot(
                configuration.getAttributeAsLong("heartbeatTimeout", ConfigurationServlet.DefaultHeartbeatTimeout));
        Slot sequenceNo = new Slot(0L);
        return
            new PushStormDetectionServer(
                new SequenceTaggingServer(sequenceNo,
                    new BlockingConnectionServer(browserID, monitoringScheduler, heartbeatInterval, terminateBlockingConnectionOnShutdown, configuration)), configuration);
    }

    private class CreatePushID extends AbstractPseudoServlet {
        public void service(HttpServletRequest request, HttpServletResponse response) throws Exception {
            response.setContentType("text/plain");
            response.getOutputStream().print(pushContext.createPushId(request, response));
        }
    }

    private class NotifyPushID extends AbstractPseudoServlet {
        public void service(HttpServletRequest request, HttpServletResponse response) throws Exception {
            String group = request.getParameter("group");
            String[] options = request.getParameterValues("option");
            if (options != null && options.length > 0) {
                PushConfiguration configuration = new PushConfiguration();
                Map<String,Object> attributes = configuration.getAttributes();
                for (int i = 0; i < options.length; i++) {
                    String option = options[i];
                    String[] nameValue = NAME_VALUE.split(option);
                    attributes.put(nameValue[0], nameValue[1]);
                }
                pushContext.push(group, configuration);
            } else {
                pushContext.push(group);
            }
            response.setContentType("text/plain");
            response.setContentLength(0);
        }
    }

    private class AddGroupMember extends AbstractPseudoServlet {
        public void service(HttpServletRequest request, HttpServletResponse response) throws Exception {
            String group = request.getParameter("group");
            String pushID = request.getParameter("id");
            pushContext.addGroupMember(group, pushID);
            response.setContentType("text/plain");
            response.setContentLength(0);
        }
    }

    private class RemoveGroupMember extends AbstractPseudoServlet {
        public void service(HttpServletRequest request, HttpServletResponse response) throws Exception {
            String group = request.getParameter("group");
            String pushID = request.getParameter("id");
            pushContext.removeGroupMember(group, pushID);
            response.setContentType("text/plain");
            response.setContentLength(0);
        }
    }
}
