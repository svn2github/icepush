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

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icesoft.notify.cloud.core.CloudNotificationService;

public class NoopPushGroupManager
implements PushGroupManager {
    private static final Logger LOGGER = Logger.getLogger(NoopPushGroupManager.class.getName());

    public final static PushGroupManager Instance = new NoopPushGroupManager();

    private NoopPushGroupManager() {
    }

    public void addBlockingConnectionServer(final String browserID, final BlockingConnectionServer server) {
    }

    public boolean addBrowser(final Browser browser) {
        return false;
    }

    public boolean addMember(final String groupName, final String pushID) {
        return false;
    }

    public boolean addMember(final String groupName, final String pushID, final PushConfiguration pushConfiguration) {
        return false;
    }

    public void addNotificationReceiver(final NotificationBroadcaster.Receiver observer) {
    }

    public boolean addNotifyBackURI(final NotifyBackURI notifyBackURI) {
        return false;
    }

    public boolean addNotifyBackURI(final String browserID, final URI notifyBackURI) {
        return false;
    }

    public void addPushGroupListener(final PushGroupListener listener) {
    }

    public void backOff(final String browserID, final long delay) {
    }

    public void cancelExpiryTimeouts(final String browserID) {
    }

    public void clearPendingNotification(final String pushID) {
    }

    public void clearPendingNotifications(final Set<String> pushIDSet) {
    }

    public Browser getBrowser(final String browserID) {
        return null;
    }

    public Map<String, String[]> getGroupPushIDsMap() {
        return Collections.EMPTY_MAP;
    }

    public CloudNotificationService getCloudNotificationService() {
        return null;
    }

    public NotifyBackURI getNotifyBackURI(final String notifyBackURI) {
        return null;
    }

    public Set<NotificationEntry> getPendingNotificationSet() {
        return Collections.emptySet();
    }

    public PushID getPushID(final String pushIDString) {
        return null;
    }

    public boolean hasNotifyBackURI(final String browserID) {
        return false;
    }

    public NotifyBackURI newNotifyBackURI(final String uri) {
        return null;
    }

    public void park(final String pushID, final String notifyBackURI) {
    }

    public void push(final String groupName) {
    }

    public void push(final String groupName, final String payload) {
    }

    public void push(final String groupName, final PushConfiguration pushConfiguration) {
    }

    public void push(final String groupName, final String payload, final PushConfiguration pushConfiguration) {
    }

    public void pruneParkedIDs(final String notifyBackURI, final Set<String> listenedPushIDSet)  {
    }

    public void removeBlockingConnectionServer(final String browserID) {
    }

    public boolean removeBrowser(final String browserID) {
        return false;
    }

    public boolean removeMember(final String groupName, final String pushID) {
        return false;
    }

    public void removeNotificationReceiver(final NotificationBroadcaster.Receiver observer) {
    }

    public boolean removeNotifyBackURI(final String browserID) {
        return false;
    }

    public void removePushGroupListener(final PushGroupListener listener) {
    }

    public void scan(final Set<String> confirmedPushIDSet) {
    }

    public void shutdown() {
    }

    public void startExpiryTimeouts(final String browserID) {
    }
}
