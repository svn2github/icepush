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

    public static enum NotificationType {
        PUSH,
        CLOUD_PUSH
    }

    public static enum TargetType {
        BROWSER_ID,
        PUSH_ID
    }

    private final TargetType targetType;
    private final String targetID;
    private final String groupName;
    private final NotificationType notificationType;

    public NotificationEvent(
        final TargetType targetType, final String targetID, final String groupName,
        final NotificationType notificationType, final Object source) {

        super(source);
        this.targetType = targetType;
        this.targetID = targetID;
        this.groupName = groupName;
        this.notificationType = notificationType;
    }

    public String getGroupName() {
        return groupName;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public String getTargetID() {
        return targetID;
    }

    public TargetType getTargetType() {
        return targetType;
    }
}
