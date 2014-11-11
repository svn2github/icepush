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

import java.util.EventObject;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotificationEvent
extends EventObject {
    private static final Logger LOGGER = Logger.getLogger(NotificationEvent.class.getName());

    private final String groupName;
    private final String notificationProviderType;
    private final String pushType;

    public NotificationEvent(
        final String groupName, final String pushType, final String notificationProviderType, final Object source) {

        super(source);
        this.groupName = groupName;
        this.pushType = pushType;
        this.notificationProviderType = notificationProviderType;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getNotificationProviderType() {
        return notificationProviderType;
    }

    public String getPushType() {
        return pushType;
    }

    public String toString() {
        return
            new StringBuilder().
                append("NotificationEvent[").
                    append(membersAsString()).
                append("]").
                    toString();
    }

    protected String membersAsString() {
        return
            new StringBuilder().
                append("groupName: '").append(getGroupName()).append("', ").
                append("pushType: '").append(getPushType()).append("', ").
                append("notificationProviderType: '").append(getNotificationProviderType()).append("', ").
                append("source: '").append(getSource()).append("'").
                    toString();
    }
}
