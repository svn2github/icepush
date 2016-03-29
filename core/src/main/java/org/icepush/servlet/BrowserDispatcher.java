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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icepush.Browser;
import org.icepush.Configuration;
import org.icepush.PushGroupManager;
import org.icepush.PushInternalContext;

public abstract class BrowserDispatcher
implements PseudoServlet {
    private final static Logger LOGGER = Logger.getLogger(BrowserDispatcher.class.getName());

    private final Lock browserEntryMapLock = new ReentrantLock();
    private final Map<String, BrowserEntry> browserEntryMap = new HashMap<String, BrowserEntry>();

    private final long browserTimeout;
    private final long minCloudPushInterval;

    public BrowserDispatcher(final Configuration configuration) {
        this.browserTimeout = configuration.getAttributeAsLong("browserTimeout", 10 * 60 * 1000);
        this.minCloudPushInterval = configuration.getAttributeAsLong("minCloudPushInterval", 10 * 1000);
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws Exception {
        discardUnusedServlets();
        String browserID = Browser.getBrowserID(request);
        checkSession(browserID);
        lookupServer(browserID).service(request, response);
    }

    public void shutdown() {
        lockBrowserEntryMap();
        try {
            List<BrowserEntry> browserEntryList = new ArrayList<BrowserEntry>(getModifiableBrowserEntryMap().values());
            for (final BrowserEntry browserEntry : browserEntryList) {
                browserEntry.shutdown();
            }
        } finally {
            unlockBrowserEntryMap();
        }
    }

    protected void checkSession(final String browserID)
    throws Exception {
        lockBrowserEntryMap();
        try {
            if (!getModifiableBrowserEntryMap().containsKey(browserID)) {
                getPushGroupManager().addBrowser(newBrowser(browserID, getMinCloudPushInterval()));
                getModifiableBrowserEntryMap().
                    put(browserID, new BrowserEntry(browserID, this.newServer(browserID), this));
            }
        } finally {
            unlockBrowserEntryMap();
        }
    }

    protected void discardUnusedServlets() {
        lockBrowserEntryMap();
        try {
            List<BrowserEntry> browserEntryList = new ArrayList<BrowserEntry>(getModifiableBrowserEntryMap().values());
            for (final BrowserEntry browserEntry : browserEntryList) {
                browserEntry.discardIfExpired();
            }
        } finally {
            unlockBrowserEntryMap();
        }
    }

    protected final Lock getBrowserEntryMapLock() {
        return browserEntryMapLock;
    }

    protected final long getBrowserTimeout() {
        return browserTimeout;
    }

    protected final long getMinCloudPushInterval() {
        return minCloudPushInterval;
    }

    protected final Map<String, BrowserEntry> getModifiableBrowserEntryMap() {
        return browserEntryMap;
    }

    protected final PushGroupManager getPushGroupManager() {
        return (PushGroupManager)PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName());
    }

    protected final void lockBrowserEntryMap() {
        getBrowserEntryMapLock().lock();
    }

    protected PseudoServlet lookupServer(final String browserID) {
        lockBrowserEntryMap();
        try {
            return getModifiableBrowserEntryMap().get(browserID);
        } finally {
            unlockBrowserEntryMap();
        }
    }

    protected Browser newBrowser(final String browserID, final long minCloudPushInterval) {
        return new Browser(browserID, minCloudPushInterval);
    }

    protected abstract PseudoServlet newServer(String browserID)
    throws Exception;

    protected final void unlockBrowserEntryMap() {
        getBrowserEntryMapLock().unlock();
    }

    protected static class BrowserEntry
    implements PseudoServlet {
        private static final Logger LOGGER = Logger.getLogger(BrowserEntry.class.getName());

        private final BrowserDispatcher browserDispatcher;
        private final String browserID;
        private final PseudoServlet pseudoServlet;

        public BrowserEntry(
            final String browserID, final PseudoServlet pseudoServlet, final BrowserDispatcher browserDispatcher) {

            this.browserID = browserID;
            this.pseudoServlet = pseudoServlet;
            this.browserDispatcher = browserDispatcher;
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("New browser detected, assigning ID '" + getBrowserID() + "'.");
            }
        }

        public void service(final HttpServletRequest request, final HttpServletResponse response)
        throws Exception {
            getPushGroupManager().getBrowser(getBrowserID()).setLastAccessTimestamp(System.currentTimeMillis());
            getPseudoServlet().service(request, response);
        }

        public void shutdown() {
            getPseudoServlet().shutdown();
        }

        public void discardIfExpired() {
            if (getPushGroupManager().getBrowser(getBrowserID()).getLastAccessTimestamp() +
                    getBrowserDispatcher().getBrowserTimeout() < System.currentTimeMillis()) {

                try {
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.finest("Discard browser with ID '" + getBrowserID() + "' since is no longer used.");
                    }
                    getPseudoServlet().shutdown();
                    getPushGroupManager().removeBrowser(getBrowserID());
                } catch (final Throwable throwable) {
                    LOGGER.fine("Failed to discard browser bound server for ID=" + getBrowserID());
                } finally {
                    getBrowserDispatcher().lockBrowserEntryMap();
                    try {
                        getBrowserDispatcher().getModifiableBrowserEntryMap().remove(getBrowserID());
                    } finally {
                        getBrowserDispatcher().unlockBrowserEntryMap();
                    }
                    LOGGER.fine("Discarded browser bound server for ID=" + getBrowserID());
                }
            }
        }

        protected final BrowserDispatcher getBrowserDispatcher() {
            return browserDispatcher;
        }

        protected final String getBrowserID() {
            return browserID;
        }

        protected final PseudoServlet getPseudoServlet() {
            return pseudoServlet;
        }

        protected final PushGroupManager getPushGroupManager() {
            return (PushGroupManager)PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName());
        }
    }
}
