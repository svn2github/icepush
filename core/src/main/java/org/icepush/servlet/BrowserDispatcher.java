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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icepush.Browser;
import org.icepush.Configuration;

public abstract class BrowserDispatcher
implements PseudoServlet {
    private final static Logger log = Logger.getLogger(BrowserDispatcher.class.getName());
    private final long browserTimeout;

    protected final Map<String, BrowserEntry> browserBoundServlets = new HashMap<String, BrowserEntry>();

    public BrowserDispatcher(final Configuration configuration) {
        this.browserTimeout = configuration.getAttributeAsLong("browserTimeout", 10 * 60 * 1000);
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws Exception {
        discardUnusedServlets();
        String browserID = Browser.getBrowserID(request);
        checkSession(browserID);
        lookupServer(browserID).service(request, response);
    }

    public void shutdown() {
        synchronized (browserBoundServlets) {
            List<BrowserEntry> browserEntryList = new ArrayList<BrowserEntry>(browserBoundServlets.values());
            for (final BrowserEntry browserEntry : browserEntryList) {
                browserEntry.shutdown();
            }
        }
    }

    protected void checkSession(String browserID)
    throws Exception {
        synchronized (browserBoundServlets) {
            if (!browserBoundServlets.containsKey(browserID)) {
                browserBoundServlets.put(browserID, new BrowserEntry(browserID, this.newServer(browserID)));
            }
        }
    }

    protected void discardUnusedServlets() {
        synchronized (browserBoundServlets) {
            List<BrowserEntry> browserEntryList = new ArrayList<BrowserEntry>(browserBoundServlets.values());
            for (final BrowserEntry browserEntry : browserEntryList) {
                browserEntry.discardIfExpired();
            }
        }
    }

    protected PseudoServlet lookupServer(final String browserID) {
        synchronized (browserBoundServlets) {
            return browserBoundServlets.get(browserID);
        }
    }

    protected abstract PseudoServlet newServer(String browserID)
    throws Exception;

    protected class BrowserEntry
    implements PseudoServlet {
        private String id;
        private PseudoServlet servlet;
        private long lastAccess = System.currentTimeMillis();

        public BrowserEntry(final String id, final PseudoServlet servlet) {
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
