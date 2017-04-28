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
package org.icepush;

import org.icesoft.util.servlet.ServletContextConfiguration;

import static org.icesoft.util.ObjectUtilities.*;
import static org.icesoft.util.PreCondition.checkArgument;
import static org.icesoft.util.StringUtilities.isNotNullAndIsNotEmpty;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PushContext {
    private static final Logger LOGGER = Logger.getLogger(PushContext.class.getName());

    private final ServletContext servletContext;

    private int subCounter = 0;

    /**
     * <p>
     *     Constructs a new PushContext.
     * </p>
     *
     * @param      servletContext
     *                 The Servlet Context.
     */
    private PushContext(final ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    /**
     * <p>
     *     Adds the specified <code>pushID</code> to the group with the specified <code>groupName</code>.
     * </p>
     *
     * @param      groupName
     *                 The name of the group the specified <code>pushID</code> needs to be added to.
     * @param      pushID
     *                 The Push ID that needs to be added.
     * @throws     IllegalArgumentException
     *                 If the specified <code>groupName</code> and/or the specified <code>pushID</code> is
     *                 <code>null</code> or empty.
     */
    public void addGroupMember(final String groupName, final String pushID) {
        checkArgument(
            isNotNullAndIsNotEmpty(groupName),
            "Illegal argument groupName: '" + groupName + "'.  Argument cannot be null or empty."
        );
        checkArgument(
            isNotNullAndIsNotEmpty(pushID),
            "Illegal argument pushID: '" + pushID + "'.  Argument cannot be null or empty."
        );
        getPushGroupManager().addMember(groupName, pushID);
    }

    /**
     * <p>
     *     Adds the specified <code>pushID</code> to the group with the specified <code>groupName</code>.
     * </p>
     *
     * @param      groupName
     *                 The name of the group the specified <code>pushID</code> needs to be added to.
     * @param      pushID
     *                 The Push ID that needs to be added.
     * @param      pushConfiguration
     *                 The Push configuration.
     * @throws     IllegalArgumentException
     *                 If the specified <code>groupName</code> and/or the specified <code>pushID</code> is
     *                 <code>null</code> or empty.
     */
    public void addGroupMember(final String groupName, final String pushID, final PushConfiguration pushConfiguration) {
        checkArgument(
            isNotNullAndIsNotEmpty(groupName),
            "Illegal argument groupName: '" + groupName + "'.  Argument cannot be null or empty."
        );
        checkArgument(
            isNotNullAndIsNotEmpty(pushID),
            "Illegal argument pushID: '" + pushID + "'.  Argument cannot be null or empty."
        );
        getPushGroupManager().addMember(groupName, pushID, pushConfiguration);
    }

    public void addNotifyBackURI(final String browserID, final URI notifyBackURI) {
        checkArgument(
            isNotNullAndIsNotEmpty(browserID),
            "Illegal argument browserID: '" + browserID + "'.  Argument cannot be null or empty."
        );
        checkArgument(
            isNotNull(notifyBackURI),
            "Illegal argument notifyBackURI: '" + notifyBackURI + "'.  Argument cannot be null."
        );
        getPushGroupManager().addNotifyBackURI(browserID, notifyBackURI);
    }

    /**
     * <p>
     *     Instructs the specified browser to back off from ajax push listen for the specified number of milliseconds.
     * </p>
     *
     * @param      browserID
     *                 The ICEpush browser ID as stored in the ice.push.browser cookie.
     * @param      delay
     *                 The delay in milliseconds the browser needs to back off.
     * @throws     IllegalArgumentException
     *                 If the specified <code>browserID</code> is <code>null</code> or empty and/or the specified
     *                 <code>delay</code> is less than <code>0</code>.
     */
    public void backOff(final String browserID, final long delay) {
        checkArgument(
            isNotNullAndIsNotEmpty(browserID),
            "Illegal argument browserID: '" + browserID + "'.  Argument cannot be null or empty."
        );
        checkArgument(
            delay >= 0,
            "Illegal argument delay: " + delay + ".  Argument cannot be less than 0."
        );
        getPushGroupManager().backOff(browserID, delay);
    }

    /**
     * <p>
     *     Creates a Push ID consisting of the Browser ID and Sub ID.  If the specified <code>request</code> does not
     *     contain a Browser ID already, it will be created and set on the specified <code>request</code> and
     *     <code>response</code>.
     * </p>
     *
     * @param      request
     *                 The HTTP Servlet request.
     * @param      response
     *                 The HTTP Servlet response.
     * @return     The created Push ID.
     * @throws     IllegalArgumentException
     *                 If the specified <code>request</code> and/or <code>response</code> is <code>null</code>.
     */

    public synchronized String createPushId(final HttpServletRequest request, final HttpServletResponse response) {
        checkArgument(
            isNotNull(request),
            "Illegal argument request: '" + request + "'.  Argument cannot be null."
        );
        checkArgument(
            isNotNull(response),
            "Illegal argument response: '" + response + "'.  Argument cannot be null."
        );
        String browserID = Browser.getBrowserID(request);
        if (browserID == null) {
            String currentBrowserID = (String)request.getAttribute(Browser.BROWSER_ID_NAME);
            if (null == currentBrowserID) {
                browserID = Browser.generateBrowserID();
                Cookie cookie = new Cookie(Browser.BROWSER_ID_NAME, browserID);
                cookie.setMaxAge((int)(Browser.getTimeout(getServletContext()) / 1000));
                cookie.setPath("/");
                response.addCookie(cookie);
                request.setAttribute(Browser.BROWSER_ID_NAME, browserID);
            } else {
                browserID = currentBrowserID;
            }
        }
        String _pushID = browserID + ":" + generateSubID();
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Created new Push-ID '" + _pushID + "'.");
        }
        getPushGroupManager().createPushID(_pushID);
        return _pushID;
    }

    public synchronized String createPushId(
        final HttpServletRequest request, final HttpServletResponse response, final long pushIDTimeout,
        final long cloudPushIDTimeout) {

        checkArgument(
            isNotNull(request),
            "Illegal argument request: '" + request + "'.  Argument cannot be null."
        );
        checkArgument(
            isNotNull(response),
            "Illegal argument response: '" + response + "'.  Argument cannot be null."
        );
        String browserID = Browser.getBrowserID(request);
        if (browserID == null) {
            String currentBrowserID = (String)request.getAttribute(Browser.BROWSER_ID_NAME);
            if (null == currentBrowserID) {
                browserID = Browser.generateBrowserID();
                Cookie cookie = new Cookie(Browser.BROWSER_ID_NAME, browserID);
                cookie.setMaxAge((int)(Browser.getTimeout(getServletContext()) / 1000));
                cookie.setPath("/");
                response.addCookie(cookie);
                request.setAttribute(Browser.BROWSER_ID_NAME, browserID);
            } else {
                browserID = currentBrowserID;
            }
        }
        String _pushID = browserID + ":" + generateSubID();
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Created new Push-ID '" + _pushID + "'.");
        }
        getPushGroupManager().createPushID(_pushID, pushIDTimeout, cloudPushIDTimeout);
        return _pushID;
    }

    public void deletePushID(final String pushID) {
        checkArgument(
            isNotNullAndIsNotEmpty(pushID),
            "Illegal argument pushID: '" + pushID + "'.  Argument cannot be null or empty."
        );
        getPushGroupManager().deletePushID(pushID);
    }

    public boolean hasNotifyBackURI(final String browserID) {
        checkArgument(
            isNotNullAndIsNotEmpty(browserID),
            "Illegal argument browserID: '" + browserID + "'.  Argument cannot be null or empty."
        );
        return getPushGroupManager().hasNotifyBackURI(browserID);
    }

    /**
     * <p>
     *     Initiate a Server Push to the members of the group specified by the <code>groupName</code>.
     * </p>
     *
     * @param      groupName
     *                 The group name of the group.
     * @see        #push(String, PushConfiguration)
     * @throws     IllegalArgumentException
     *                 If the specified <code>groupName</code> is <code>null</code> or empty.
     */
    public void push(final String groupName) {
        checkArgument(
            isNotNullAndIsNotEmpty(groupName),
            "Illegal argument groupName: '" + groupName + "'.  Argument cannot be null or empty."
        );
        getPushGroupManager().push(groupName);
    }

    /**
     * <p>
     *     Initiate a Server Push to the members of the group specified by the <code>groupName</code> with the specified
     *     <code>payload</code>.
     * </p>
     *
     * @param      groupName
     *                 The group name of the group.
     * @param      payload
     *                 The payload to be send to the members of the group.
     * @see        #push(String, String, org.icepush.PushConfiguration)
     * @throws     IllegalArgumentException
     *                 If the specified <code>groupName</code> is <code>null</code> or empty.
     */
    public void push(final String groupName, final String payload) {
        checkArgument(
            isNotNullAndIsNotEmpty(groupName),
            "Illegal argument groupName: '" + groupName + "'.  Argument cannot be null or empty."
        );
        getPushGroupManager().push(groupName, payload);
    }

    /**
     * <p>
     *     Initiate a Server Push to the members of the group specified by the <code>groupName</code>.
     * </p>
     *
     * @param      groupName
     *                 The group name of the group.
     * @param      pushConfiguration
     *                 The Push configuration.
     * @see        #push(String)
     * @throws     IllegalArgumentException
     *                 If the specified <code>groupName</code> is <code>null</code> or empty or if the specified
     *                 <code>pushConfiguration</code> does contain the attribute <i>'subject'</i> but does not contain
     *                 the attribute <i>'targetURI'</i>.
     */
    public void push(final String groupName, final PushConfiguration pushConfiguration) {
        checkArgument(
            isNotNullAndIsNotEmpty(groupName),
            "Illegal argument groupName: '" + groupName + "'.  Argument cannot be null or empty."
        );
        checkArgument(
            isNull(pushConfiguration) ||
                !pushConfiguration.containsAttributeKey("subject") ||
                pushConfiguration.containsAttributeKey("targetURI"),
            "Illegal argument pushConfiguration: '" + pushConfiguration + "'.  " +
                "Argument must contain attribute 'targetURI' when it contains attribute 'subject'."
        );
        getPushGroupManager().push(groupName, pushConfiguration);
    }

    /**
     * <p>
     *     Initiate a Server Push to the members of the group specified by the <code>groupName</code> with the specified
     *     <code>payload</code>.
     * </p>
     *
     * @param      groupName
     *                 The group name of the group.
     * @param      payload
     *                 The payload to be send to the members of the group.
     * @param      pushConfiguration
     *                 The Push configuration.
     * @see        #push(String, String, org.icepush.PushConfiguration)
     * @throws     IllegalArgumentException
     *                 If the specified <code>groupName</code> is <code>null</code> or empty or if the specified
     *                 <code>pushConfiguration</code> does contain the attribute <i>'subject'</i> but does not contain
     *                 the attribute <i>'targetURI'</i>.
     */
    public void push(final String groupName, final String payload, final PushConfiguration pushConfiguration) {
        checkArgument(
            isNotNullAndIsNotEmpty(groupName),
            "Illegal argument groupName: '" + groupName + "'.  Argument cannot be null or empty."
        );
        checkArgument(
            isNull(pushConfiguration) ||
                !pushConfiguration.containsAttributeKey("subject") ||
                pushConfiguration.containsAttributeKey("targetURI"),
            "Illegal argument pushConfiguration: '" + pushConfiguration + "'.  " +
                "Argument must contain attribute 'targetURI' when it contains attribute 'subject'."
        );
        getPushGroupManager().push(groupName, payload, pushConfiguration);
    }

    /**
     * <p>
     *     Removes the specified <code>pushId</code> from the group with the specified <code>groupName</code>.
     * </p>
     *
     * @param      groupName
     *                 The name of the group the specified <code>pushId</code> needs to be removed from.
     * @param      pushID
     *                 The Push ID that needs to be removed.
     * @throws     IllegalArgumentException
     *                 If the specified <code>groupName</code> and/or the specified <code>pushID</code> is
     *                 <code>null</code> or empty.
     */
    public void removeGroupMember(final String groupName, final String pushID) {
        checkArgument(
            isNotNullAndIsNotEmpty(groupName),
            "Illegal argument groupName: '" + groupName + "'.  Argument cannot be null or empty."
        );
        checkArgument(
            isNotNullAndIsNotEmpty(pushID),
            "Illegal argument pushID: '" + pushID + "'.  Argument cannot be null or empty."
        );
        getPushGroupManager().removeMember(groupName, pushID);
    }

    public void removeNotifyBackURI(final String browserID) {
        checkArgument(
            isNotNullAndIsNotEmpty(browserID),
            "Illegal argument browserID: '" + browserID + "'.  Argument cannot be null or empty."
        );
        getPushGroupManager().removeNotifyBackURI(browserID);
    }

    /**
     * <p>
     *     Gets the PushContext instance associated with the specified <code>servletContext</code>.
     * </p>
     *
     * @param      servletContext
     *                 The ServletContext from which to get the PushContext.
     * @return     The PushContext.
     * @throws     IllegalArgumentException
     *                 If the specified <code>servletContext</code> is <code>null</code>.
     */
    public static synchronized PushContext getInstance(final ServletContext servletContext) {
        checkArgument(
            servletContext != null,
            "Illegal argument servletContext: '" + servletContext + "'.  Argument cannot be null."
        );
        PushContext pushContext = (PushContext)servletContext.getAttribute(PushContext.class.getName());
        if (pushContext == null) {
            servletContext.setAttribute(PushContext.class.getName(), pushContext = new PushContext(servletContext));
        }
        return pushContext;
    }

    protected PushGroupManager getPushGroupManager() {
        return (PushGroupManager)PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName());
    }

    protected final ServletContext getServletContext() {
        return servletContext;
    }

    private synchronized String generateSubID() {
        return Integer.toString((++subCounter) + (hashCode() / 10000), 36);
    }
}
