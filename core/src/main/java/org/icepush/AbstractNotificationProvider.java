package org.icepush;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractNotificationProvider
implements NotificationProvider {
    private static final Logger LOGGER = Logger.getLogger(AbstractNotificationProvider.class.getName());

    private final Set<NotificationProviderListener>
        listenerSet = new CopyOnWriteArraySet<NotificationProviderListener>();

    public void addNotificationProviderListener(final NotificationProviderListener listener) {
        listenerSet.add(listener);
    }

    public void removeNotificationProviderListener(final NotificationProviderListener listener) {
        listenerSet.remove(listener);
    }

    protected void notificationSent(final NotificationEvent event) {
        for (final NotificationProviderListener listener : listenerSet) {
            listener.notificationSent(event);
        }
    }
}
