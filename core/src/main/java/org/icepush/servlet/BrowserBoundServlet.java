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

import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icepush.*;
import org.icepush.http.PushServer;
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
        dispatchOn(".*create-push-id\\.icepush", newCreatePushID());
        dispatchOn(".*notify\\.icepush", newNotifyPushID());
        dispatchOn(".*add-group-member\\.icepush", newAddGroupMember());
        dispatchOn(".*remove-group-member\\.icepush", newRemoveGroupMember());
    }

    protected PushServer createBlockingConnectionServer() {
        Slot heartbeatInterval = new Slot(configuration.getAttributeAsLong("heartbeatTimeout", ConfigurationServer.DefaultHeartbeatTimeout));
        Slot sequenceNo = new Slot(0L);
        return new ConfigurationServer(heartbeatInterval, servletContext, configuration,
                    new PushStormDetectionServer(
                        new SequenceTaggingServer(sequenceNo,
                            new BlockingConnectionServer(browserID, monitoringScheduler, heartbeatInterval, terminateBlockingConnectionOnShutdown, configuration)), configuration));
    }

    protected AddGroupMember newAddGroupMember() {
        return new AddGroupMember();
    }

    protected CreatePushID newCreatePushID() {
        return new CreatePushID();
    }

    protected NotifyPushID newNotifyPushID() {
        return new NotifyPushID();
    }

    protected RemoveGroupMember newRemoveGroupMember() {
        return new RemoveGroupMember();
    }

    protected class AddGroupMember extends AbstractPseudoServlet {
        public void service(HttpServletRequest request, HttpServletResponse response) throws Exception {
            String groupName = request.getParameter("group");
            String pushID = request.getParameter("id");
            addGroupMember(groupName, pushID);
            response.setContentType("text/plain");
            response.setContentLength(0);
        }

        protected void addGroupMember(final String groupName, final String pushID) {
            pushContext.addGroupMember(groupName, pushID);
        }
    }

    protected class CreatePushID extends AbstractPseudoServlet {
        public void service(HttpServletRequest request, HttpServletResponse response) throws Exception {
            response.setContentType("text/plain");
            response.getOutputStream().print(createPushID(request, response));
        }

        protected String createPushID(final HttpServletRequest request, final HttpServletResponse response) {
            return pushContext.createPushId(request, response);
        }
    }

    protected class NotifyPushID extends AbstractPseudoServlet {
        public void service(HttpServletRequest request, HttpServletResponse response) throws Exception {
            String groupName = request.getParameter("group");
            PushConfiguration pushConfiguration;

            String[] options = request.getParameterValues("option");
            if (options != null && options.length > 0) {
                pushConfiguration = new PushConfiguration();
                Map<String,Object> attributes = pushConfiguration.getAttributes();
                for (int i = 0; i < options.length; i++) {
                    String option = options[i];
                    String[] nameValue = NAME_VALUE.split(option);
                    attributes.put(nameValue[0], nameValue[1]);
                }
            } else {
                pushConfiguration = null;
            }
            String delay = request.getParameter("delay");
            if (delay != null) {
                String duration = request.getParameter("duration");
                if (pushConfiguration == null) {
                    pushConfiguration = new PushConfiguration();
                }
                pushConfiguration.delayed(Long.parseLong(delay), Long.parseLong(duration));
            }
            String at = request.getParameter("at");
            if (at != null) {
                String duration = request.getParameter("duration");
                if (pushConfiguration == null) {
                    pushConfiguration = new PushConfiguration();
                }
                pushConfiguration.scheduled(new Date(Long.parseLong(at)), Long.parseLong(duration));
            }

            if (pushConfiguration == null) {
                push(groupName);
            } else {
                push(groupName, pushConfiguration);
            }

            response.setContentType("text/plain");
            response.setContentLength(0);
        }

        protected void push(final String groupName) {
            pushContext.push(groupName);
        }

        protected void push(final String groupName, final PushConfiguration pushConfiguration) {
            pushContext.push(groupName, pushConfiguration);
        }
    }

    protected class RemoveGroupMember extends AbstractPseudoServlet {
        public void service(HttpServletRequest request, HttpServletResponse response) throws Exception {
            String group = request.getParameter("group");
            String pushID = request.getParameter("id");
            removeGroupMember(group, pushID);
            response.setContentType("text/plain");
            response.setContentLength(0);
        }

        protected void removeGroupMember(final String groupName, final String pushID) {
            pushContext.removeGroupMember(groupName, pushID);
        }
    }
}
