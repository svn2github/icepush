/*
 * Version: MPL 1.1
 *
 * "The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is ICEfaces 1.5 open source software code, released
 * November 5, 2006. The Initial Developer of the Original Code is ICEsoft
 * Technologies Canada, Corp. Portions created by ICEsoft are Copyright (C)
 * 2004-2010 ICEsoft Technologies Canada, Corp. All Rights Reserved.
 *
 * Contributor(s): _____________________.
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
