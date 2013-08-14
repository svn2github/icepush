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

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PushGroupManager {
    void addBlockingConnectionServer(String browserID, BlockingConnectionServer server);

    void addBrowser(Browser browser);

    void addMember(String groupName, String pushId);

    void addNotificationReceiver(NotificationBroadcaster.Receiver receiver);

    void addPushGroupListener(PushGroupListener listener);

    void backOff(String browserID, long delay);

    void cancelExpiryTimeout(Browser browser);

    void clearPendingNotifications(Set<String> pushIDSet);

    void deleteNotificationReceiver(NotificationBroadcaster.Receiver observer);

    Browser getBrowser(String browserID);

    Map<String, String[]> getGroupMap();

    OutOfBandNotifier getOutOfBandNotifier();

    String[] getPendingNotifications();

    PushID getPushID(String pushIDString);

    void park(String pushId, NotifyBackURI notifyBackURI);

    void push(String groupName);

    void push(String groupName, PushConfiguration config);

    void pruneParkedIDs(NotifyBackURI notifyBackURI, Set<String> listenedPushIDSet);

    void removeBlockingConnectionServer(String browserID);

    void removeBrowser(Browser browser);

    void removeMember(String groupName, String pushId);

    void removePushGroupListener(PushGroupListener listener);

    void scan(String[] confirmedPushIDs);

    void shutdown();

    void startExpiryTimeout(Browser browser);
}
