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
import java.util.Map;
import java.util.Set;

import org.icesoft.notify.cloud.core.CloudNotificationService;

public interface PushGroupManager {
    void addBlockingConnectionServer(String browserID, BlockingConnectionServer server);

    boolean addBrowser(Browser browser);

    boolean addMember(String groupName, String pushID);

    boolean addMember(String groupName, String pushID, PushConfiguration pushConfiguration);

    boolean addNotifyBackURI(NotifyBackURI notifyBackURI);

    boolean addNotifyBackURI(String browserID, URI notifyBackURI);

    void addNotificationReceiver(NotificationBroadcaster.Receiver receiver);

    void addPushGroupListener(PushGroupListener listener);

    void backOff(String browserID, long delay);

    void cancelExpiryTimeouts(String browserID);

    void clearPendingNotification(String pushID);

    void clearPendingNotifications(Set<String> pushIDSet);

    boolean createPushID(String pushID);

    boolean createPushID(String pushID, long pushIDTimeout, long cloudPushIDTimeout);

    boolean deletePushID(String pushID);

    Browser getBrowser(String browserID);

    CloudNotificationService getCloudNotificationService();

    Map<String, String[]> getGroupPushIDsMap();

    NotifyBackURI getNotifyBackURI(String notifyBackURI);

    Set<NotificationEntry> getPendingNotificationSet();

    PushID getPushID(String pushIDString);

    boolean hasNotifyBackURI(String browserID);

    void park(String pushID, String notifyBackURI);

    void push(String groupName);

    void push(String groupName, String payload);

    void push(String groupName, PushConfiguration pushConfiguration);

    void push(String groupName, String payload, PushConfiguration pushConfiguration);

    void pruneParkedIDs(String notifyBackURI, Set<String> listenedPushIDSet);

    void removeBlockingConnectionServer(String browserID);

    boolean removeBrowser(String browserID);

    boolean removeMember(String groupName, String pushID);

    boolean removeNotifyBackURI(String browserID);

    void removeNotificationReceiver(NotificationBroadcaster.Receiver observer);

    void removePushGroupListener(PushGroupListener listener);

    void scan(Set<String> confirmedPushIDSet);

    void shutdown();

    void startExpiryTimeouts(String browserID);

    NotifyBackURI newNotifyBackURI(String uri);
}
