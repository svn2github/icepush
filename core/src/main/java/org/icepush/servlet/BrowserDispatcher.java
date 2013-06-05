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

import org.icepush.Configuration;
import org.icepush.PushContext;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class BrowserDispatcher implements PseudoServlet {
    private final static Logger log = Logger.getLogger(BrowserDispatcher.class.getName());
    private static final String BrowserIDCookieName = "ice.push.browser";
    private final Map browserBoundServlets = new HashMap();
    private final long browserTimeout;

    public BrowserDispatcher(Configuration configuration) {
        this.browserTimeout = configuration.getAttributeAsLong("browserTimeout", 10 * 60 * 1000);
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws Exception {
        discardUnusedServlets();
        String browserID = getBrowserIDFromCookie(request);
        checkSession(browserID);
        lookupServer(browserID).service(request, response);
    }

    public void shutdown() {
        synchronized (browserBoundServlets) {
            Iterator i = new ArrayList(browserBoundServlets.values()).iterator();
            while (i.hasNext()) {
                ((PseudoServlet)i.next()).shutdown();
            }
        }
    }

    protected abstract PseudoServlet newServer(String browserID) throws Exception;

    protected void checkSession(String browserID) throws Exception {
        synchronized (browserBoundServlets) {
            if (!browserBoundServlets.containsKey(browserID)) {
                browserBoundServlets.put(browserID, new BrowserEntry(browserID, this.newServer(browserID)));
            }
        }
    }

    protected PseudoServlet lookupServer(final String browserID) {
        synchronized (browserBoundServlets) {
            return (PseudoServlet)browserBoundServlets.get(browserID);
        }
    }

    public static String getBrowserIDFromCookie(HttpServletRequest request) {
        String browserID = request.getHeader(BrowserIDCookieName);
        if (null != browserID)  {
            return browserID;
        }
        return getBrowserIDFromCookies(request.getCookies());
    }

    public static String getBrowserIDFromCookies(Cookie[] cookies)  {
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (BrowserIDCookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    private void discardUnusedServlets() {
        synchronized (browserBoundServlets) {
            Iterator i = new ArrayList(browserBoundServlets.values()).iterator();
            while (i.hasNext()) {
                ((BrowserEntry)i.next()).discardIfExpired();
            }
        }
    }

    private class BrowserEntry implements PseudoServlet {
        private String id;
        private PseudoServlet servlet;
        private long lastAccess = System.currentTimeMillis();

        private BrowserEntry(String id, PseudoServlet servlet) {
            this.id = id;
            this.servlet = servlet;
            if (log.isLoggable(Level.FINEST)) {
                log.finest("New browser detected, assigning ID '" + id + "'.");
            }
        }

        public void service(HttpServletRequest request, HttpServletResponse response) throws Exception {
            lastAccess = System.currentTimeMillis();
            servlet.service(request, response);
        }

        public void shutdown() {
            servlet.shutdown();
        }

        public void discardIfExpired() {
            if (lastAccess + browserTimeout < System.currentTimeMillis()) {
                try {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Discard browser with ID '" + id + "' since is no longer used.");
                    }
                    servlet.shutdown();
                } catch (Throwable t) {
                    log.fine("Failed to discard browser bound server for ID=" + id);
                } finally {
                    synchronized (browserBoundServlets) {
                        browserBoundServlets.remove(id);
                    }
                    log.fine("Discarded browser bound server for ID=" + id);
                }
            }
        }
    }
}
