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
package org.icepush;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public boolean addMember(final String groupName, final String pushId) {
        return false;
    }

    public void addNotificationReceiver(final NotificationBroadcaster.Receiver observer) {
    }

    public void addPushGroupListener(final PushGroupListener listener) {
    }

    public void backOff(final String browserID, final long delay) {
    }

    public void cancelExpiryTimeouts(final String browserID) {
    }

    public void clearPendingNotifications(final Set<String> pushIDSet) {
    }

    public void deleteNotificationReceiver(final NotificationBroadcaster.Receiver observer) {
    }

    public Browser getBrowser(final String browserID) {
        return null;
    }

    public Map<String, String[]> getGroupPushIDsMap() {
        return Collections.EMPTY_MAP;
    }

    public OutOfBandNotifier getOutOfBandNotifier() {
        return null;
    }

    public Set<NotificationEntry> getPendingNotificationSet() {
        return Collections.emptySet();
    }

    public PushID getPushID(final String pushIDString) {
        return null;
    }

    public NotifyBackURI newNotifyBackURI(final String uri) {
        return null;
    }

    public void push(final String groupName) {
    }

    public void push(final String groupName, final PushConfiguration config) {
    }

    public void park(final String pushId, final NotifyBackURI notifyBackURI) {
    }
    
    public void pruneParkedIDs(final NotifyBackURI notifyBackURI, final Set<String> listenedPushIDSet)  {
    }

    public void removeBlockingConnectionServer(final String browserID) {
    }

    public boolean removeBrowser(final Browser browser) {
        return false;
    }

    public boolean removeMember(final String groupName, final String pushId) {
        return false;
    }

    public void removePushGroupListener(final PushGroupListener listener) {
    }

    public void scan(final String[] confirmedPushIDs) {
    }

    public void shutdown() {
    }

    public void startExpiryTimeouts(final String browserID) {
    }
}
