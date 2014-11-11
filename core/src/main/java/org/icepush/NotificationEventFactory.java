package org.icepush;

public interface NotificationEventFactory {
    NotificationEvent createNotificationEvent(
        String groupName, String pushType, String notificationProvider, PushConfiguration pushConfiguration,
        Object source);
}
