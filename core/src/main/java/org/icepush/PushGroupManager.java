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

public interface PushGroupManager {
    void addBlockingConnectionServer(String browserID, BlockingConnectionServer server);

    boolean addBrowser(Browser browser);

    boolean addMember(String groupName, String pushID);

    void addNotificationReceiver(NotificationBroadcaster.Receiver receiver);

    void addPushGroupListener(PushGroupListener listener);

    void backOff(String browserID, long delay);

    void cancelExpiryTimeouts(String browserID);

    void clearPendingNotification(String pushID);

    void clearPendingNotifications(Set<String> pushIDSet);

    boolean createPushID(String pushID);

    boolean createPushID(String pushID, long pushIDTimeout);

    boolean deletePushID(String pushID);

    Browser getBrowser(String browserID);

    Map<String, String[]> getGroupPushIDsMap();

    Set<NotificationEntry> getPendingNotificationSet();

    PushID getPushID(String pushIDString);

    void push(String groupName);

    void push(String groupName, String payload);

    void push(String groupName, PushConfiguration pushConfiguration);

    void push(String groupName, String payload, PushConfiguration pushConfiguration);

    void removeBlockingConnectionServer(String browserID);

    boolean removeBrowser(String browserID);

    boolean removeMember(String groupName, String pushID);

    void removeNotificationReceiver(NotificationBroadcaster.Receiver observer);

    void removePushGroupListener(PushGroupListener listener);

    void scan(Set<String> confirmedPushIDSet);

    void shutdown();

    void startExpiryTimeouts(String browserID);
}
