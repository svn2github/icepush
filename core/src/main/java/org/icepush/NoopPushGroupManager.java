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
import java.util.List;
import java.util.Map;

public class NoopPushGroupManager implements PushGroupManager {
    public final static PushGroupManager Instance = new NoopPushGroupManager();

    private NoopPushGroupManager() {
    }

    public void addBlockingConnectionServer(final BlockingConnectionServer server) {
    }

    public void addMember(final String groupName, final String pushId) {
    }

    public void addNotificationReceiver(final NotificationBroadcaster.Receiver observer) {
    }

    public void addPushGroupListener(final PushGroupListener listener) {
    }

    public void backOff(final String browserID, final long delay) {
    }

    public void cancelConfirmationTimeout(final List<String> pushIDList) {
    }

    public void cancelExpiryTimeout(final List<String> pushIDList) {
    }

    public void clearPendingNotifications(final List<String> pushIdList) {
    }

    public void deleteNotificationReceiver(final NotificationBroadcaster.Receiver observer) {
    }

    public Map<String, String[]> getGroupMap() {
        return Collections.EMPTY_MAP;
    }

    public String[] getPendingNotifications() {
        return new String[0];
    }

    public void push(final String groupName) {
    }

    public void push(final String groupName, final PushConfiguration config) {
    }

    public void park(final String pushId, final NotifyBackURI notifyBackURI) {
    }
    
    public void pruneParkedIDs(final NotifyBackURI notifyBackURI, final List<String> listenedPushIds)  {
    }

    public void recordListen(final List<String> pushIdList, final int sequenceNumber) {
    }

    public void removeBlockingConnectionServer(final BlockingConnectionServer server) {
    }

    public void removeMember(final String groupName, final String pushId) {
    }

    public void removePushGroupListener(final PushGroupListener listener) {
    }

    public void scan(final String[] confirmedPushIDs) {
    }

    public boolean setNotifyBackURI(
        final List<String> pushIDList, final NotifyBackURI notifyBackURI, final boolean broadcast) {

        return false;
    }

    public void shutdown() {
    }

    public void startConfirmationTimeout(
        final List<String> pushIDList, final NotifyBackURI notifyBackURI, final long timeout) {

    }

    public void startExpiryTimeout(final List<String> pushIDList, final NotifyBackURI notifyBackURI) {
    }
}
