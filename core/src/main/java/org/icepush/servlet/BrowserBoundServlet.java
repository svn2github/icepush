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

import java.util.Timer;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icepush.BlockingConnectionServer;
import org.icepush.Configuration;
import org.icepush.ConfigurationServer;
import org.icepush.PushContext;
import org.icepush.PushStormDetectionServer;
import org.icepush.SequenceTaggingServer;
import org.icepush.http.PushServer;
import org.icepush.util.Slot;

public class BrowserBoundServlet
extends PathDispatcher
implements PseudoServlet {
    private final static Logger LOGGER = Logger.getLogger(BrowserBoundServlet.class.getName());

    protected final String browserID;
    protected final Configuration configuration;
    protected final Slot heartbeatInterval;
    protected final Timer monitoringScheduler;
    protected final PushContext pushContext;
    protected final ServletContext servletContext;
    protected final boolean terminateBlockingConnectionOnShutdown;

    protected boolean setUp = false;

    public BrowserBoundServlet(
        final String browserID, final PushContext pushContext, final ServletContext servletContext,
        final Timer monitoringScheduler, final Configuration configuration,
        final boolean terminateBlockingConnectionOnShutdown) {

        this.browserID = browserID;
        this.pushContext = pushContext;
        this.servletContext = servletContext;
        this.monitoringScheduler = monitoringScheduler;
        this.configuration = configuration;
        this.terminateBlockingConnectionOnShutdown = terminateBlockingConnectionOnShutdown;
        this.heartbeatInterval =
            new Slot(
                configuration.getAttributeAsLong("heartbeatTimeout", ConfigurationServer.DefaultHeartbeatTimeout));
    }

    @Override
    public void service(final HttpServletRequest request, final HttpServletResponse response)
    throws Exception, IllegalStateException {
        checkSetUp();
        super.service(request, response);
    }

    public void setUp() {
        dispatchOn(".*listen\\.icepush", newListen());
        dispatchOn(".*create-push-id\\.icepush", newCreatePushID());
        dispatchOn(".*add-group-member\\.icepush", newAddGroupMember());
        dispatchOn(".*remove-group-member\\.icepush", newRemoveGroupMember());
        setUp = true;
    }

    @Override
    public void shutdown()
    throws IllegalStateException {
        checkSetUp();
        super.shutdown();
    }

    protected void checkSetUp()
    throws IllegalStateException {
        if (!setUp) {
            throw new IllegalStateException("Browser Bound Servlet is not set-up.");
        }
    }

    protected PushServer createBlockingConnectionServer() {
        Slot sequenceNo = new Slot(0L);
        return
            new ConfigurationServer(
                heartbeatInterval,
                servletContext,
                configuration,
                new PushStormDetectionServer(
                    new SequenceTaggingServer(
                        sequenceNo,
                        newBlockingConnectionServer()
                    ),
                    configuration
                )
            );
    }

    protected PseudoServlet newAddGroupMember() {
        return new AddGroupMember(pushContext);
    }

    protected BlockingConnectionServer newBlockingConnectionServer() {
        BlockingConnectionServer _blockBlockingConnectionServer =
            new BlockingConnectionServer(
                browserID, monitoringScheduler, heartbeatInterval, terminateBlockingConnectionOnShutdown, configuration
            );
        _blockBlockingConnectionServer.setUp();
        return _blockBlockingConnectionServer;
    }

    protected PseudoServlet newCreatePushID() {
        return new CreatePushID(pushContext);
    }

    protected PseudoServlet newListen() {
        return new EnvironmentAdaptingServlet(createBlockingConnectionServer(), heartbeatInterval, configuration);
    }

    protected PseudoServlet newRemoveGroupMember() {
        return new RemoveGroupMember(pushContext);
    }
}
