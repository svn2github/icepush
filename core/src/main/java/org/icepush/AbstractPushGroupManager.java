/*
 * Copyright 2004-2014 ICEsoft Technologies Canada Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS
 * IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.icepush;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractPushGroupManager
implements InternalPushGroupManager, PushGroupManager {
    private static final Logger LOGGER = Logger.getLogger(AbstractPushGroupManager.class.getName());

    private final Set<PushGroupListener> pushGroupListenerSet = new HashSet<PushGroupListener>();

    public void addPushGroupListener(final PushGroupListener listener) {
        if (!pushGroupListenerSet.contains(listener)) {
            pushGroupListenerSet.add(listener);
        }
    }

    public void groupTouched(final String groupName, final long timestamp) {
        PushGroupEvent _event = new PushGroupEvent(this, groupName, null, timestamp);
        for (final PushGroupListener listener : pushGroupListenerSet) {
            listener.groupTouched(_event);
        }
    }

    public void removePushGroupListener(final PushGroupListener listener) {
        if (pushGroupListenerSet.contains(listener)) {
            pushGroupListenerSet.remove(listener);
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
