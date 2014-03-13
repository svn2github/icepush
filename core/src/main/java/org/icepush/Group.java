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

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Group
implements Serializable {
    private static final long serialVersionUID = -2793842028376415034L;

    private static final Logger LOGGER = Logger.getLogger(Group.class.getName());

    private final long groupTimeout;
    private final String name;
    private final Set<String> pushIDSet = new HashSet<String>();

    private long lastAccess = System.currentTimeMillis();

    protected Group(final Group group) {
        this(group.getName(), group.getGroupTimeout());
        pushIDSet.addAll(group.getPushIDSet());
        lastAccess = group.getLastAccess();
    }

    protected Group(final String name, final long groupTimeout) {
        this.name = name;
        this.groupTimeout = groupTimeout;
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Push Group '" + getName() + "' created.");
        }
    }

    public boolean addPushID(final String pushID) {
        return getPushIDSet().add(pushID);
    }

    public String getName() {
        return name;
    }

    public boolean removePushID(final String pushID) {
        boolean _modified = getPushIDSet().remove(pushID);
        if (getPushIDSet().isEmpty()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE, "Disposed Push Group '" + getName() + "' since it no longer contains any PushIDs.");
            }
            ((InternalPushGroupManager)
                PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName())
            ).removeGroup(getName());
        }
        return _modified;
    }

    @Override
    public String toString() {
        return
            new StringBuilder().
                append("Group[").
                    append(membersAsString()).
                append("]").
                    toString();
    }

    protected long getGroupTimeout() {
        return groupTimeout;
    }

    protected long getLastAccess() {
        return lastAccess;
    }

    protected String[] getPushIDs() {
        return getPushIDSet().toArray(new String[getPushIDSet().size()]);
    }

    protected Set<String> getPushIDSet() {
        return pushIDSet;
    }

    protected String membersAsString() {
        return
            new StringBuilder().
                append("name: '").append(getName()).append("', ").
                append("pushIDSet: '").append(getPushIDSet()).append("', ").
                append("lastAccess: '").append(getLastAccess()).append("'").
                    toString();
    }

    protected void touch() {
        touch(System.currentTimeMillis());
    }

    protected void touch(final long timestamp) {
        lastAccess = timestamp;
    }

    protected void touchIfMatching(final Set<String> pushIDSet) {
        for (final String _pushID : pushIDSet) {
            if (getPushIDSet().contains(_pushID)) {
                touch();
                ((InternalPushGroupManager)
                    PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName())
                ).groupTouched(getName(), getLastAccess());
                //no need to touchIfMatching again
                //return right away without checking the expiration
                return;
            }
        }
    }

    void discardIfExpired() {
        //expire group
        if (getLastAccess() + getGroupTimeout() < System.currentTimeMillis()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Push Group '" + getName() + "' expired.");
            }
            ((InternalPushGroupManager)
                PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName())
            ).removeGroup(getName());
            ((InternalPushGroupManager)
                PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName())
            ).removePendingNotifications(getPushIDSet());
            for (final String _pushIDString : getPushIDSet()) {
                PushID _pushID =
                    ((InternalPushGroupManager)
                        PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName())
                    ).getPushID(_pushIDString);
                if (_pushID != null) {
                    _pushID.removeFromGroup(getName());
                }
            }
        }
    }
}
