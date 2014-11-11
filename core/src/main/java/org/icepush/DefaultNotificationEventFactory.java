package org.icepush;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultNotificationEventFactory
implements NotificationEventFactory {
    private static final Logger LOGGER = Logger.getLogger(DefaultNotificationEventFactory.class.getName());

    public NotificationEvent createNotificationEvent(
        final String groupName, final String pushType, final String notificationProvider,
        final PushConfiguration pushConfiguration, final Object source) {

        return new NotificationEvent(groupName, pushType, notificationProvider, source);
    }
}
