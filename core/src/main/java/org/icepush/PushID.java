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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.util.DatabaseEntity;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Entity(value = "push_ids")
public class PushID
implements DatabaseEntity, Serializable {
    private static final long serialVersionUID = 2845881329862716766L;

    private static final Logger LOGGER = Logger.getLogger(PushID.class.getName());

    private final Map<String, Boolean> groupMembershipMap = new HashMap<String, Boolean>();

    @Id
    private String databaseID;

    private String id;
    private String browserID;
    private String subID;

    private long cloudPushIDTimeout;
    private long pushIDTimeout;

    public PushID() {
        // Do nothing.
    }

    protected PushID(
        final String id, final long pushIDTimeout, final long cloudPushIDTimeout) {

        this(
            id,
            id.substring(0, id.indexOf(':')),
            id.substring(id.indexOf(':') + 1),
            pushIDTimeout,
            cloudPushIDTimeout
        );
    }

    protected PushID(
        final String id, final String browserID, final String subID, final long pushIDTimeout,
        final long cloudPushIDTimeout) {

        this.id = id;
        this.browserID = browserID;
        this.subID = subID;
        this.pushIDTimeout = pushIDTimeout;
        this.cloudPushIDTimeout = cloudPushIDTimeout;
        // Let the databaseID be the pushID.
        this.databaseID = getID();
    }

    public boolean addToGroup(final String groupName) {
        return addToGroup(groupName, (PushConfiguration)null);
    }

    public boolean addToGroup(final String groupName, final PushConfiguration pushConfiguration) {
        boolean _modified = false;
        Boolean _currentCloudPush;
        if (pushConfiguration != null) {
            _currentCloudPush = (Boolean)pushConfiguration.getAttribute("cloudPush");
            if (_currentCloudPush == null) {
                _currentCloudPush = Boolean.TRUE;
            }
        } else {
            _currentCloudPush = Boolean.TRUE;
        }
        Boolean _previousCloudPush = groupMembershipMap.put(groupName, _currentCloudPush);
        if (_previousCloudPush == null || !_previousCloudPush.equals(_currentCloudPush)) {
            _modified = true;
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Push-ID '" + getID() + "' added to Group '" + groupName + "'."
                );
            }
            save();
        }
        return _modified;
    }

    public boolean cancelExpiryTimeout() {
        return cancelExpiryTimeout(getInternalPushGroupManager());
    }

    public void discard() {
        discard(getInternalPushGroupManager());
    }

    public String getBrowserID() {
        return browserID;
    }

    public String getDatabaseID() {
        return databaseID;
    }

    public String getID() {
        return id;
    }

    public String getKey() {
        return getID();
    }

    public String getSubID() {
        return subID;
    }

    public boolean isCloudPushEnabled() {
        for (final boolean _cloudPush : groupMembershipMap.values()) {
            if (_cloudPush) {
                return true;
            }
        }
        return false;
    }

    public boolean isCloudPushEnabled(final String groupName) {
        return groupMembershipMap.get(groupName);
    }

    public boolean removeFromGroup(final String groupName) {
        boolean _modified = groupMembershipMap.remove(groupName) != null;
        if (_modified) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Push-ID '" + getID() + "' removed from Group '" + groupName + "'."
                );
            }
            save();
        }
        if (groupMembershipMap.isEmpty()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE, "Disposed PushID '" + getID() + "' since it no longer belongs to any Group.");
            }
            getInternalPushGroupManager().removePushID(getID());
        }
        return _modified;
    }

    public void save() {
        if (PushInternalContext.getInstance().getAttribute(Datastore.class.getName()) != null) {
            ConcurrentMap<String, PushID> _pushIDMap =
                (ConcurrentMap<String, PushID>)PushInternalContext.getInstance().getAttribute("pushIDMap");
            if (_pushIDMap.containsKey(getID())) {
                _pushIDMap.put(getID(), this);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Saved PushID '" + this + "' to datastore."
                    );
                }
            }
        }
    }

    public boolean startExpiryTimeout() {
        return getInternalPushGroupManager().startExpiryTimeout(getID());
    }

    public boolean startExpiryTimeout(final String browserID, final long sequenceNumber) {
        return getInternalPushGroupManager().startExpiryTimeout(getID(), browserID, sequenceNumber);
    }

    @Override
    public String toString() {
        return
            new StringBuilder().
                append("PushID[").
                    append(classMembersToString()).
                append("]").
                    toString();
    }

    protected boolean cancelExpiryTimeout(final InternalPushGroupManager internalPushGroupManager) {
        return internalPushGroupManager.cancelExpiryTimeout(getID());
    }

    protected String classMembersToString() {
        return
            new StringBuilder().
                append("browserID: '").append(getBrowserID()).append(", ").
                append("cloudPushIDTimeout: '").append(getCloudPushIDTimeout()).append("', ").
                append("groupMembershipMap: '").append(getGroupMembershipMap()).append("', ").
                append("id: '").append(getID()).append("', ").
                append("pushIDTimeout: '").append(getPushIDTimeout()).append("', ").
                append("subID: '").append(getSubID()).append("'").
                    toString();
    }

    protected void discard(final InternalPushGroupManager internalPushGroupManager) {
        if (!
            internalPushGroupManager.isParked(getID())) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "PushID '" + getID() + "' discarded.");
            }
            internalPushGroupManager.removePushID(getID());
            internalPushGroupManager.removePendingNotification(getID());
            for (final String _groupName : getGroupMembershipMap().keySet()) {
                Group _group =
                    internalPushGroupManager.getGroup(_groupName);
                if (_group != null) {
                    _group.removePushID(getID(), internalPushGroupManager);
                }
            }
        }
    }

    protected long getCloudPushIDTimeout() {
        return cloudPushIDTimeout;
    }

    protected Map<String, Boolean> getGroupMembershipMap() {
        return groupMembershipMap;
    }

    protected static InternalPushGroupManager getInternalPushGroupManager() {
        return
            (InternalPushGroupManager)PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName());
    }

    protected long getPushIDTimeout() {
        return pushIDTimeout;
    }
}
