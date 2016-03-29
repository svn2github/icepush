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
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.util.DatabaseEntity;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Entity(value = "groups")
public class Group
implements DatabaseEntity, Serializable {
    private static final long serialVersionUID = -2793842028376415034L;

    private static final Logger LOGGER = Logger.getLogger(Group.class.getName());

    @Id
    private String databaseID;

    private long groupTimeout;
    private String name;
    private Set<String> pushIDSet = new HashSet<String>();

    private long lastAccess = System.currentTimeMillis();

    public Group() {
        // Do nothing.
    }

    protected Group(final Group group) {
        this(group.getName(), group.getGroupTimeout());
        pushIDSet.addAll(group.getPushIDSet());
        lastAccess = group.getLastAccess();
    }

    public Group(final String name, final long groupTimeout) {
        this.name = name;
        this.groupTimeout = groupTimeout;
        this.databaseID = getName();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Group '" + getName() + "' created.");
        }
    }

    public boolean addPushID(final String pushID) {
        boolean _modified = getPushIDSet().add(pushID);
        if (_modified) {
            save();
        }
        return _modified;
    }

    @Override
    public boolean equals(final Object object) {
        return super.equals(object);
    }

    public String getDatabaseID() {
        return databaseID;
    }

    public String getKey() {
        return getName();
    }

    public String getName() {
        return name;
    }

    public boolean removePushID(final String pushID) {
        return removePushID(pushID, getInternalPushGroupManager());
    }

    public void save() {
        if (PushInternalContext.getInstance().getAttribute(Datastore.class.getName()) != null) {
            ConcurrentMap<String, Group> _groupMap =
                (ConcurrentMap<String, Group>)PushInternalContext.getInstance().getAttribute("groupMap");
            if (_groupMap.containsKey(getKey())) {
                _groupMap.put(getKey(), this);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Saved Group '" + this + "' to Database."
                    );
                }
            }
        }
    }

    @Override
    public String toString() {
        return
            new StringBuilder().
                append("Group[").
                    append(classMembersToString()).
                append("]").
                    toString();
    }

    protected String classMembersToString() {
        return
            new StringBuilder().
                append("name: '").append(getName()).append("', ").
                append("pushIDSet: '").append(getPushIDSet()).append("', ").
                append("lastAccess: '").append(getLastAccess()).append("'").
                    toString();
    }

    protected long getGroupTimeout() {
        return groupTimeout;
    }

    protected static InternalPushGroupManager getInternalPushGroupManager() {
        return
            (InternalPushGroupManager)PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName());
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

    protected boolean removePushID(final String pushID, final InternalPushGroupManager internalPushGroupManager) {
        boolean _modified = getPushIDSet().remove(pushID);
        if (_modified) {
            if (!getPushIDSet().isEmpty()) {
                save();
            }
        }
        if (getPushIDSet().isEmpty()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE, "Disposed Group '" + getName() + "' since it no longer contains any Push-IDs.");
            }
            internalPushGroupManager.removeGroup(getName());
        }
        return _modified;
    }

    protected void touch() {
        touch(System.currentTimeMillis());
    }

    protected void touch(final long timestamp) {
        lastAccess = timestamp;
        save();
    }

    protected void touchIfMatching(final Set<String> pushIDSet) {
        for (final String _pushID : pushIDSet) {
            if (getPushIDSet().contains(_pushID)) {
                touch();
                getInternalPushGroupManager().groupTouched(getName(), getLastAccess());
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
                LOGGER.log(Level.FINE, "Group '" + getName() + "' expired.");
            }
            getInternalPushGroupManager().removeGroup(getName());
            getInternalPushGroupManager().removePendingNotifications(getPushIDSet());
            for (final String _pushIDString : getPushIDSet()) {
                PushID _pushID = getInternalPushGroupManager().getPushID(_pushIDString);
                if (_pushID != null) {
                    _pushID.removeFromGroup(getName());
                }
            }
        }
    }
}
