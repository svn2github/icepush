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

import static org.icepush.util.RequestUtilities.Patterns.NOTIFY_REQUEST;

import java.lang.reflect.InvocationTargetException;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Timer;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icepush.CheckBrowserIDServlet;
import org.icepush.CodeServer;
import org.icepush.Configuration;
import org.icepush.ProductInfo;
import org.icepush.PushContext;
import org.icepush.PushGroupManager;
import org.icepush.PushGroupManagerFactory;
import org.icepush.PushInternalContext;
import org.icepush.RemoveParameterPrefix;
import org.icepush.http.standard.CacheControlledServer;
import org.icepush.http.standard.CompressingServer;
import org.icesoft.util.servlet.ExtensionRegistry;

public class MainServlet implements PseudoServlet {
    private static final Logger LOGGER = Logger.getLogger(MainServlet.class.getName());

    private final PathDispatcher pathDispatcher;
    private final Timer monitoringScheduler;
    private final PushContext pushContext;
    private final ServletContext servletContext;
    private final Configuration configuration;
    private final boolean terminateConnectionOnShutdown;

    public synchronized static MainServlet getInstance(final ServletContext servletContext) {
        return getInstance(servletContext, true);
    }

    public synchronized static MainServlet getInstance(final ServletContext servletContext, final boolean create) {
        MainServlet _mainServlet = (MainServlet)servletContext.getAttribute(MainServlet.class.getName());
        if (_mainServlet == null && create)  {
            _mainServlet = new MainServlet(servletContext);
            servletContext.setAttribute(MainServlet.class.getName(), _mainServlet);
        }
        return _mainServlet;
    }

    public MainServlet(
        final ServletContext context) {

        this(
            context, true
        );
    }

    public MainServlet(
        final ServletContext servletContext, final boolean terminateBlockingConnectionOnShutdown) {

        this(
            servletContext,terminateBlockingConnectionOnShutdown,true
        );
    }

    public MainServlet(
        final ServletContext servletContext, final boolean terminateBlockingConnectionOnShutdown,
        final boolean printProductInfo) {

        this(
            servletContext, terminateBlockingConnectionOnShutdown, printProductInfo, (ScheduledThreadPoolExecutor)null
        );
    }

    public MainServlet(
        final ServletContext servletContext, final boolean terminateBlockingConnectionOnShutdown,
        final boolean printProductInfo, final ScheduledThreadPoolExecutor executor) {

        //We print the product info unless we are part of EE which will print out it's
        //own version.
        if(printProductInfo){
            LOGGER.info(new ProductInfo().toString());
        }
        this.servletContext = servletContext;
        getServletContext().setAttribute(org.icepush.servlet.MainServlet.class.getName(), this);
        this.configuration = new ServletContextConfiguration("org.icepush", getServletContext());
        terminateConnectionOnShutdown = terminateBlockingConnectionOnShutdown;
        monitoringScheduler = new Timer("Monitoring scheduler", true);
        pushContext = PushContext.getInstance(getServletContext());
        PushGroupManager _pushGroupManager;
        try {
            _pushGroupManager =
                (PushGroupManager)
                    ((Class)ExtensionRegistry.getBestExtension(PushGroupManager.class.getName(), servletContext)).
                        getMethod("getInstance", new Class[]{ServletContext.class}).invoke(null, servletContext);
        } catch (final NoSuchMethodException exception) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, "Unable to get instance of Push Group Manager.", exception);
            }
            _pushGroupManager =
                PushGroupManagerFactory.newPushGroupManager(getServletContext(), executor, getConfiguration());
        } catch (final IllegalAccessException exception) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, "Unable to get instance of Push Group Manager.", exception);
            }
            _pushGroupManager =
                PushGroupManagerFactory.newPushGroupManager(getServletContext(), executor, getConfiguration());
        } catch (final InvocationTargetException exception) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, "Unable to get instance of Push Group Manager.", exception);
            }
            _pushGroupManager =
                PushGroupManagerFactory.newPushGroupManager(getServletContext(), executor, getConfiguration());
        }
        PushInternalContext.getInstance().setAttribute(PushGroupManager.class.getName(), _pushGroupManager);
        pathDispatcher = new PathDispatcher();
        addDispatches();
    }

    public void dispatchOn(final String pattern, final PseudoServlet servlet) {
        getPathDispatcher().dispatchOn(pattern, servlet);
    }

    public void service(final HttpServletRequest request, final HttpServletResponse response)
    throws Exception {
        try {
            getPathDispatcher().service(request, response);
        } catch (SocketException e) {
            if ("Broken pipe".equals(e.getMessage())) {
                // client left the page
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(Level.FINE, "Connection broken by client.", e);
                } else if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Connection broken by client: " + e.getMessage());
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
        getPathDispatcher().shutdown();
        ((PushGroupManager)PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName())).shutdown();
        getMonitoringScheduler().cancel();
    }

    protected void addBrowserBoundDispatch() {
        dispatchOn(".*", createBrowserDispatcher());
    }

    protected void addDispatches() {
        dispatchOn(
            ".*code\\.min\\.icepush",
            new BasicAdaptingServlet(
                new CacheControlledServer(
                    new CompressingServer(
                        new CodeServer(getCompressedCodeResources())
                    )
                ),
                getConfiguration()
            )
        );
        dispatchOn(
            ".*code\\.icepush",
            new BasicAdaptingServlet(
                new CacheControlledServer(
                    new CompressingServer(
                        new CodeServer(getCodeResources())
                    )
                ),
                getConfiguration()
            )
        );
        dispatchOn(
            NOTIFY_REQUEST,
            new RemoveParameterPrefix(newNotifyPushID())
        );
        addBrowserBoundDispatch();
    }

    protected PseudoServlet createBrowserBoundServlet(final String browserID) {
        BrowserBoundServlet browserBoundServlet =
            new BrowserBoundServlet(
                browserID,
                getPushContext(),
                getServletContext(),
                getMonitoringScheduler(),
                getConfiguration(),
                getTerminateConnectionOnShutdown()
            );
        browserBoundServlet.setUp();
        return browserBoundServlet;
    }

    protected PseudoServlet createBrowserDispatcher() {
        return
            new RemoveParameterPrefix(
                new CheckBrowserIDServlet(
                    newBrowserDispatcher()
                )
            );
    }

    protected String[] getCodeResources() {
        return new String[] {"ice.core/bridge-support.uncompressed.js", "ice.push/icepush.uncompressed.js"};
    }

    protected String[] getCompressedCodeResources() {
        return new String[] {"ice.core/bridge-support.js", "ice.push/icepush.js"};
    }

    protected final Configuration getConfiguration() {
        return configuration;
    }

    protected final Timer getMonitoringScheduler() {
        return monitoringScheduler;
    }

    protected final PathDispatcher getPathDispatcher() {
        return pathDispatcher;
    }

    protected final PushContext getPushContext() {
        return pushContext;
    }

    protected final ServletContext getServletContext() {
        return servletContext;
    }

    protected final boolean getTerminateConnectionOnShutdown() {
        return terminateConnectionOnShutdown;
    }

    protected BrowserDispatcher newBrowserDispatcher() {
        return
            new BrowserDispatcher(getConfiguration()) {
                protected PseudoServlet newServer(final String browserID) {
                    return createBrowserBoundServlet(browserID);
                }
            };
    }

    protected PseudoServlet newNotifyPushID() {
        return new NotifyPushID(getPushContext());
    }

    public static class ExtensionRegistration
    implements ServletContextListener {
        public void contextInitialized(final ServletContextEvent servletContextEvent) {
            org.icepush.util.ExtensionRegistry.addExtension(
                servletContextEvent.getServletContext(), 1, "org.icepush.MainServlet", MainServlet.class
            );
        }

        public void contextDestroyed(final ServletContextEvent servletContextEvent) {
        }
    }
}
