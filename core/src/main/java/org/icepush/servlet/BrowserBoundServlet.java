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

import org.icepush.*;
import org.icepush.http.Server;
import org.icepush.util.Slot;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Timer;
import java.util.logging.Logger;

public class BrowserBoundServlet extends PathDispatcher {
    private final static Logger log = Logger.getLogger(BrowserBoundServlet.class.getName());
    protected PushContext pushContext;
    protected ServletContext context;
    protected PushGroupManager pushGroupManager;
    protected Timer monitoringScheduler;
    protected Configuration configuration;
    protected boolean terminateBlockingConnectionOnShutdown;

    public BrowserBoundServlet(PushContext pushContext, ServletContext context, final PushGroupManager pushGroupManager, final Timer monitoringScheduler, Configuration configuration, boolean terminateBlockingConnectionOnShutdown) {
        this.pushContext = pushContext;
        this.context = context;
        this.pushGroupManager = pushGroupManager;
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
        Slot heartbeatInterval = new Slot(configuration.getAttributeAsLong("heartbeatTimeout", ConfigurationServer.DefaultHeartbeatTimeout));
        Slot sequenceNo = new Slot(0);
        return new ConfigurationServer(pushContext, context, configuration,
                new SequenceTaggingServer(sequenceNo,
                        new BlockingConnectionServer(pushGroupManager, monitoringScheduler, heartbeatInterval, terminateBlockingConnectionOnShutdown, configuration)));
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
            pushContext.push(group);
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
