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

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NoopPushGroupManager implements PushGroupManager {
    public final static PushGroupManager Instance = new NoopPushGroupManager();

    private NoopPushGroupManager() {
    }

    public void addMember(String groupName, String pushId) {
    }

    public void addNotificationReceiver(NotificationBroadcaster.Receiver observer) {
    }

    public void deleteNotificationReceiver(NotificationBroadcaster.Receiver observer) {
    }

    public void addPushGroupListener(PushGroupListener listener) {
    }

    public String[] getPendingNotifications() {
        return new String[0];
    }

    public void clearPendingNotifications(List pushIdList) {
    }

    public Map<String, String[]> getGroupMap() {
        return Collections.EMPTY_MAP;
    }

    public void push(String groupName) {
    }

    public void push(String groupName, PushConfiguration config) {
    }

    public void park(String[] pushIds, String notifyBackURI) {
    }

    public void removeMember(String groupName, String pushId) {
    }

    public void removePushGroupListener(PushGroupListener listener) {
    }

    public void shutdown() {
    }
}
