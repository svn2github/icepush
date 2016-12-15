package org.icepush;

import java.util.EventListener;

public interface NotificationListener
extends EventListener {
    void onBeforeBroadcast(final NotificationEvent event);

    void onBeforeExecution(final NotificationEvent event);
}
