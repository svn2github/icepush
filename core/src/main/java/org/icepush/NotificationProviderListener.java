package org.icepush;

import java.util.EventListener;

public interface NotificationProviderListener
extends EventListener {
    void notificationSent(NotificationEvent event);
}
