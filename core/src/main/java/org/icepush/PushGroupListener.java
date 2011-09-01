package org.icepush;

import java.util.EventListener;

public interface PushGroupListener
extends EventListener {
    void groupTouched(PushGroupEvent event);

    void memberAdded(PushGroupEvent event);

    void memberRemoved(PushGroupEvent event);

    void pushed(PushGroupEvent event);

    void pushIDTouched(PushGroupEvent event);
}
