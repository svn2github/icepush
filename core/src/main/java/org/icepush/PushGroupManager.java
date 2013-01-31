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

public interface PushGroupManager {
    void addMember(String groupName, String pushId);

    void push(String groupName);

    void push(String groupName, PushConfiguration config);

    void park(String pushId, NotifyBackURI notifyBackURI);
    
    void pruneParkedIDs(NotifyBackURI notifyBackURI, List<String> listenedPushIds);

    String[] getPendingNotifications();

    void clearPendingNotifications(List<String> pushIdList);

    void addNotificationReceiver(NotificationBroadcaster.Receiver receiver);

    void deleteNotificationReceiver(NotificationBroadcaster.Receiver observer);

    void removeMember(String groupName, String pushId);

    void addPushGroupListener(PushGroupListener listener);

    void removePushGroupListener(PushGroupListener listener);

    Map<String, String[]> getGroupMap();

    void shutdown();

    void recordListen(List<String> pushIDList, int sequenceNumber);

    void startConfirmationTimeout(List<String> pushIDList, NotifyBackURI notifyBackURI, long timeout);

    void cancelConfirmationTimeout(List<String> pushIDList);

    void startExpiryTimeout(List<String> pushIDList, NotifyBackURI notifyBackURI);

    void cancelExpiryTimeout(List<String> pushIDList);

    void addBlockingConnectionServer(BlockingConnectionServer server);

    void removeBlockingConnectionServer(BlockingConnectionServer server);

    void backOff(String browserID, long delay);

    void scan(String[] confirmedPushIDs);

    boolean setNotifyBackURI(List<String> pushIDList, NotifyBackURI notifyBackURI, boolean broadcast);
}
