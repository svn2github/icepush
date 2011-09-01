/*
 * Version: MPL 1.1
 *
 * "The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is ICEfaces 1.5 open source software code, released
 * November 5, 2006. The Initial Developer of the Original Code is ICEsoft
 * Technologies Canada, Corp. Portions created by ICEsoft are Copyright (C)
 * 2004-2010 ICEsoft Technologies Canada, Corp. All Rights Reserved.
 *
 * Contributor(s): _____________________.
 *
 */

package org.icepush.servlet;

import org.icepush.*;
import org.icepush.http.standard.CacheControlledServer;
import org.icepush.http.standard.CompressingServer;
import org.icepush.util.ExtensionRegistry;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.SocketException;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainServlet implements PseudoServlet {
    private static final Logger log = Logger.getLogger(MainServlet.class.getName());
    protected PushGroupManager pushGroupManager;
    protected PathDispatcher dispatcher;
    protected Timer timer;
    protected PushContext pushContext;
    protected ServletContext context;
    protected Configuration configuration;
    protected boolean terminateConnectionOnShutdown;

    public MainServlet(final ServletContext context) {
        this(context, true);
    }

    public MainServlet(final ServletContext servletContext, final boolean terminateBlockingConnectionOnShutdown) {
        log.info(new ProductInfo().toString());

        context = servletContext;
        terminateConnectionOnShutdown = terminateBlockingConnectionOnShutdown;
        timer = new Timer(true);
        configuration = new ServletContextConfiguration("org.icepush", context);
        pushContext = new PushContext(context);
        pushGroupManager = PushGroupManagerFactory.newPushGroupManager(context);
        pushContext.setPushGroupManager(pushGroupManager);
        dispatcher = new PathDispatcher();

        addDispatches();
    }

    protected void addDispatches() {
        dispatchOn(".*code\\.icepush", new BasicAdaptingServlet(new CacheControlledServer(new CompressingServer(new CodeServer("icepush.js")))));
        dispatchOn(".*", new BrowserDispatcher(configuration) {
            protected PseudoServlet newServer(String browserID) {
                return createBrowserBoundServlet(browserID);
            }
        });
    }

    protected PseudoServlet createBrowserBoundServlet(String browserID) {
        return new BrowserBoundServlet(pushContext, context, pushGroupManager, timer, configuration, terminateConnectionOnShutdown);
    }

    public void dispatchOn(String pattern, PseudoServlet servlet) {
        dispatcher.dispatchOn(pattern, servlet);
    }

    public PushGroupManager getPushGroupManager() {
        return pushGroupManager;
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            dispatcher.service(request, response);
        } catch (SocketException e) {
            if ("Broken pipe".equals(e.getMessage())) {
                // client left the page
                if (log.isLoggable(Level.FINEST)) {
                    log.log(Level.FINEST, "Connection broken by client.", e);
                } else if (log.isLoggable(Level.FINE)) {
                    log.log(Level.FINE, "Connection broken by client: " + e.getMessage());
                }
            } else {
                throw new ServletException(e);
            }
        } catch (RuntimeException e) {
            //Tomcat won't properly redirect to the configured error-page.
            //So we need a new RuntimeException that actually includes a message.
            if (e.getMessage() == null) {
                throw new RuntimeException("wrapped Exception: " + e, e);
            } else {
                throw e;
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    public void shutdown() {
        dispatcher.shutdown();
        timer.cancel();
    }

    public static class ExtensionRegistration implements ServletContextListener {
        public void contextInitialized(ServletContextEvent servletContextEvent) {
            ExtensionRegistry.addExtension(servletContextEvent.getServletContext(), 1, "org.icepush.MainServlet", MainServlet.class);
        }

        public void contextDestroyed(ServletContextEvent servletContextEvent) {
        }
    }
}
