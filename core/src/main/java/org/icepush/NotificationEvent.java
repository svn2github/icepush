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
