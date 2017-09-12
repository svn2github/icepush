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

import java.util.Map;
import java.util.Set;

public interface InternalPushGroupManager
extends PushGroupManager {
    void addAllNotificationEntries(Set<NotificationEntry> notificationEntrySet);

    void broadcastNotificationEntries(Set<NotificationEntry> notificationEntrySet, long duration, String groupName);

    boolean cancelExpiryTimeout(String pushID);

    Map<String, Browser> getBrowserMap();

    ExpiryTimeout getExpiryTimeout(String pushID);

    Group getGroup(String groupName);

    Map<String, Group> getGroupMap();

    Map<String, PushID> getPushIDMap();

    void groupTouched(String groupName, long lastAccess);

    NotificationEntry newNotificationEntry(
        String pushID, String groupName, String payload);

    NotificationEntry newNotificationEntry(
        String pushID, String groupName, String payload, Map<String, Object> propertyMap);

    boolean removeExpiryTimeout(ExpiryTimeout expiryTimeout);

    boolean removeGroup(String groupName);

    void removePendingNotification(String pushID);

    void removePendingNotifications(Set<String> pushIDSet);

    boolean removePushID(String pushID);

    void scanForExpiry();

    boolean startExpiryTimeout(String pushID);

    boolean startExpiryTimeout(String pushID, long timeout);

    boolean startExpiryTimeout(String pushID, String browserID, long timeout);
}
