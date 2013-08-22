package org.icepush;

import java.util.EventListener;

public interface NotificationListener
extends EventListener {
    void notificationSent(NotificationEvent event);
}
