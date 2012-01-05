/*
 * Copyright 2004-2012 ICEsoft Technologies Canada Corp.
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
package org.icepush;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PushContext {
    private static final Logger log = Logger.getLogger(PushContext.class.getName());
    private static final String BrowserIDCookieName = "ice.push.browser";
    //assign noop manager to avoid NPEs before the real manager is assign during startup
    private PushGroupManager pushGroupManager = NoopPushGroupManager.Instance;

    private int browserCounter = 0;
    private int subCounter = 0;

    public PushContext(final ServletContext context) {
        context.setAttribute(PushContext.class.getName(), this);
    }

    public synchronized String createPushId(HttpServletRequest request, HttpServletResponse response) {
        String browserID = getBrowserIDFromCookie(request);
        if (browserID == null) {
            String currentBrowserID = (String)
                    request.getAttribute(BrowserIDCookieName);
            if (null == currentBrowserID) {
                browserID = generateBrowserID();
                Cookie cookie = new Cookie(BrowserIDCookieName, browserID);
                cookie.setPath("/");
                response.addCookie(cookie);
                request.setAttribute(BrowserIDCookieName, browserID);
            } else {
                browserID = currentBrowserID;
            }
        }

        String id = browserID + ":" + generateSubID();
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Created new pushId '" + id + "'.");
        }
        return id;
    }

    public void push(final String groupName) {
        pushGroupManager.push(groupName);
    }

    public void push(final String groupName, PushConfiguration config) {
        pushGroupManager.push(groupName, config);
    }

    public void addGroupMember(final String groupName, final String pushId) {
        pushGroupManager.addMember(groupName, pushId);
    }

    public void removeGroupMember(final String groupName, final String pushId) {
        pushGroupManager.removeMember(groupName, pushId);
    }

    public void setPushGroupManager(final PushGroupManager pushGroupManager) {
        this.pushGroupManager = pushGroupManager;
    }

    public static synchronized PushContext getInstance(ServletContext context) {
        return (PushContext) context.getAttribute(PushContext.class.getName());
    }

    private static String getBrowserIDFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (BrowserIDCookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    synchronized String generateBrowserID() {
        return Long.toString(++browserCounter, 36) + Long.toString(System.currentTimeMillis(), 36);
    }

    private synchronized String generateSubID() {
        return Integer.toString((++subCounter) + (hashCode() / 10000), 36);
    }
}
