package org.icepush;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

public abstract class AbstractNotificationProvider
implements NotificationProvider {
    private static final Logger LOGGER = Logger.getLogger(AbstractNotificationProvider.class.getName());

    private final Set<NotificationListener>
        listenerSet = new CopyOnWriteArraySet<NotificationListener>();

    public void addNotificationProviderListener(final NotificationListener listener) {
        listenerSet.add(listener);
    }

    public void removeNotificationProviderListener(final NotificationListener listener) {
        listenerSet.remove(listener);
    }

    protected void notificationSent(final NotificationEvent event) {
        for (final NotificationListener listener : listenerSet) {
            listener.notificationSent(event);
        }
    }
}
