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

import java.util.List;
import java.util.Map;

public interface PushGroupManager {
    void addMember(String groupName, String pushId);

    void push(String groupName);

    void push(String groupName, PushConfiguration config);

    void park(String[] pushIds, String notifyBackURI);
    
    void pruneParkedIDs(String notifyBackURI, List<String> listenedPushIds);

    String[] getPendingNotifications();

    void clearPendingNotifications(List pushIdList);

    void addNotificationReceiver(NotificationBroadcaster.Receiver receiver);

    void deleteNotificationReceiver(NotificationBroadcaster.Receiver observer);

    void removeMember(String groupName, String pushId);

    void addPushGroupListener(PushGroupListener listener);

    void removePushGroupListener(PushGroupListener listener);

    Map<String, String[]> getGroupMap();

    void shutdown();
}
