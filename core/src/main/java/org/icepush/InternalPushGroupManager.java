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
    boolean cancelConfirmationTimeout(String browserID);

    boolean cancelExpiryTimeout(String pushID);

    Map<String, Browser> getBrowserMap();

    Group getGroup(String groupName);

    Map<String, Group> getGroupMap();

    Map<String, PushID> getPushIDMap();

    void groupTouched(String groupName, long lastAccess);

    boolean isParked(String pushID);

    boolean removeGroup(String groupName);

    void removePendingNotification(String pushID);

    void removePendingNotifications(Set<String> pushIDSet);

    boolean removePushID(String pushID);

    boolean startConfirmationTimeout(String browserID, String groupName);

    boolean startConfirmationTimeout(String browserID, String groupName, long sequenceNumber);

    boolean startConfirmationTimeout(String browserID, String groupName, long sequenceNumber, long timeout);

    boolean startExpiryTimeout(String pushID);

    boolean startExpiryTimeout(String pushID, String browserID, long timeout);
}
