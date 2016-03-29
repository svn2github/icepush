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

    static HashSet<TraceListener> traceListeners = new HashSet<TraceListener>();
    protected PathDispatcher dispatcher;
    protected Timer monitoringScheduler;
    protected PushContext pushContext;
    protected ServletContext servletContext;
    protected Configuration configuration;
    protected boolean terminateConnectionOnShutdown;

    public synchronized static MainServlet getInstance(final ServletContext servletContext) {
        MainServlet _mainServlet = (MainServlet)servletContext.getAttribute(MainServlet.class.getName());
        if (_mainServlet == null)  {
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
        this.servletContext.setAttribute(org.icepush.servlet.MainServlet.class.getName(), this);
        configuration = new ServletContextConfiguration("org.icepush", this.servletContext);
        terminateConnectionOnShutdown = terminateBlockingConnectionOnShutdown;
        monitoringScheduler = new Timer("Monitoring scheduler", true);
        pushContext = PushContext.getInstance(this.servletContext);
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
                PushGroupManagerFactory.newPushGroupManager(this.servletContext, executor, configuration);
        } catch (final IllegalAccessException exception) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, "Unable to get instance of Push Group Manager.", exception);
            }
            _pushGroupManager =
                PushGroupManagerFactory.newPushGroupManager(this.servletContext, executor, configuration);
        } catch (final InvocationTargetException exception) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, "Unable to get instance of Push Group Manager.", exception);
            }
            _pushGroupManager =
                PushGroupManagerFactory.newPushGroupManager(this.servletContext, executor, configuration);
        }
        PushInternalContext.getInstance().setAttribute(PushGroupManager.class.getName(), _pushGroupManager);
        dispatcher = new PathDispatcher();
        addDispatches();
    }

    public static void addTraceListener(TraceListener listener)  {
        traceListeners.add(listener);
    }

    public void dispatchOn(final String pattern, final PseudoServlet servlet) {
        dispatcher.dispatchOn(pattern, servlet);
    }

    public void service(final HttpServletRequest request, final HttpServletResponse response)
    throws Exception {
        try {
            dispatcher.service(request, response);
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
        dispatcher.shutdown();
        ((PushGroupManager)PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName())).shutdown();
        monitoringScheduler.cancel();
    }

    public static void trace(final String message)  {
        for (TraceListener listener : traceListeners)  {
            listener.handleTrace(message);
        }
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
                configuration
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
                configuration
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
                pushContext,
                servletContext,
                monitoringScheduler,
                configuration,
                terminateConnectionOnShutdown
            );
        browserBoundServlet.setUp();
        return browserBoundServlet;
    }

    protected PseudoServlet createBrowserDispatcher() {
        return
            new RemoveParameterPrefix(
                new CheckBrowserIDServlet(
                    new BrowserDispatcher(configuration) {
                        protected PseudoServlet newServer(final String browserID) {
                            return createBrowserBoundServlet(browserID);
                        }
                    }
                )
            );
    }

    protected String[] getCodeResources() {
        return new String[] {"ice.core/bridge-support.uncompressed.js", "ice.push/icepush.uncompressed.js"};
    }

    protected String[] getCompressedCodeResources() {
        return new String[] {"ice.core/bridge-support.js", "ice.push/icepush.js"};
    }

    protected PseudoServlet newNotifyPushID() {
        return new NotifyPushID(pushContext);
    }

    //Application can add itself as a TraceListener to receive
    //diagnostic message callbacks when cloud push occurs
    public interface TraceListener  {
        public void handleTrace(String message);
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
