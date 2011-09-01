package org.icepush;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractPushGroupManager
implements PushGroupManager {
    private static final Logger LOGGER = Logger.getLogger(AbstractPushGroupManager.class.getName());

    private final Set<PushGroupListener> pushGroupListenerSet = new HashSet<PushGroupListener>();

    public void addPushGroupListener(final PushGroupListener listener) {
        if (!pushGroupListenerSet.contains(listener)) {
            pushGroupListenerSet.add(listener);
        }
    }

    public void removePushGroupListener(final PushGroupListener listener) {
        if (pushGroupListenerSet.contains(listener)) {
            pushGroupListenerSet.remove(listener);
        }
    }

    protected void groupTouched(final String groupName, final Long timestamp) {
        PushGroupEvent _event = new PushGroupEvent(this, groupName, null, timestamp);
        for (final PushGroupListener listener : pushGroupListenerSet) {
            listener.groupTouched(_event);
        }
    }

    protected void memberAdded(final String groupName, final String pushId) {
        PushGroupEvent _event = new PushGroupEvent(this, groupName, pushId, null);
        for (final PushGroupListener listener : pushGroupListenerSet) {
            listener.memberAdded(_event);
        }
    }

    protected void memberRemoved(final String groupName, final String pushId) {
        PushGroupEvent _event = new PushGroupEvent(this, groupName, pushId, null);
        for (final PushGroupListener listener : pushGroupListenerSet) {
            listener.memberRemoved(_event);
        }
    }

    protected void pushed(final String groupName) {
        PushGroupEvent _event = new PushGroupEvent(this, groupName,  null, null);
        for (final PushGroupListener listener : pushGroupListenerSet) {
            listener.pushed(_event);
        }
    }

    protected void pushIDTouched(final String pushId, final Long timestamp) {
        PushGroupEvent _event = new PushGroupEvent(this, null, pushId, timestamp);
        for (final PushGroupListener listener : pushGroupListenerSet) {
            listener.pushIDTouched(_event);
        }
    }
}
