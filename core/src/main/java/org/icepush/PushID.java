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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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

    private final Set<String> groupSet = new HashSet<String>();

    @Id
    private String databaseID;

    private String id;
    private String browserID;
    private String subID;

    private long pushIDTimeout;

    public PushID() {
        // Do nothing.
    }

    public PushID(
        final String id, final long pushIDTimeout) {

        this(
            id,
            pushIDTimeout,
            true
        );
    }

    public PushID(
        final String id, final String browserID, final String subID, final long pushIDTimeout) {

        this(
            id,
            browserID,
            subID,
            pushIDTimeout,
            true
        );
    }

    protected PushID(
        final String id, final long pushIDTimeout, final boolean save) {

        this(
            id,
            id.substring(0, id.indexOf(':')),
            id.substring(id.indexOf(':') + 1),
            pushIDTimeout,
            save
        );
    }

    protected PushID(
        final String id, final String browserID, final String subID, final long pushIDTimeout, final boolean save) {

        setID(id, false);
        setBrowserID(browserID, false);
        setSubID(subID, false);
        setPushIDTimeout(pushIDTimeout, false);
        // Let the databaseID be the pushID.
        setDatabaseID(getID());
        if (save) {
            save();
        }
    }

    public boolean addToGroup(final String groupName) {
        boolean _modified;
        if (getModifiableGroupSet().add(groupName)) {
            _modified = true;
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Push-ID '" + getID() + "' added to Group '" + groupName + "'."
                );
            }
            save();
        } else {
            _modified = false;
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

    public Set<String> getGroupSet() {
        return Collections.unmodifiableSet(getModifiableGroupSet());
    }

    public String getID() {
        return id;
    }

    public String getKey() {
        return getID();
    }

    public long getPushIDTimeout() {
        return pushIDTimeout;
    }

    public String getSubID() {
        return subID;
    }

    public boolean removeFromGroup(final String groupName) {
        boolean _modified = getModifiableGroupSet().remove(groupName);
        if (_modified) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Push-ID '" + getID() + "' removed from Group '" + groupName + "'."
                );
            }
            save();
        }
        if (getModifiableGroupSet().isEmpty()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE, "Disposed PushID '" + getID() + "' since it no longer belongs to any Group.");
            }
            getInternalPushGroupManager().removePushID(getID());
        }
        return _modified;
    }

    public void save() {
        // VRAS-683 : Disable Notification Service's MongoDB usage for icesoft_technologies database and its collections
//        if (PushInternalContext.getInstance().getAttribute(Datastore.class.getName()) != null) {
//            ConcurrentMap<String, PushID> _pushIDMap =
//                (ConcurrentMap<String, PushID>)PushInternalContext.getInstance().getAttribute("pushIDMap");
//            if (_pushIDMap.containsKey(getID())) {
//                _pushIDMap.put(getID(), this);
//                if (LOGGER.isLoggable(Level.FINE)) {
//                    LOGGER.log(
//                        Level.FINE,
//                        "Saved PushID '" + this + "' to datastore."
//                    );
//                }
//            }
//        }
    }

    public boolean setPushIDTimeout(final long pushIDTimeout) {
        return setPushIDTimeout(pushIDTimeout, true);
    }

    public boolean startExpiryTimeout() {
        return startExpiryTimeout(getInternalPushGroupManager());
    }

    public boolean startExpiryTimeout(final long timeout) {
        return startExpiryTimeout(getInternalPushGroupManager(), timeout);
    }

    public boolean startExpiryTimeout(final String browserID, final long sequenceNumber) {
        return startExpiryTimeout(getInternalPushGroupManager(), browserID, sequenceNumber);
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
                append("groupSet: '").append(getModifiableGroupSet()).append("', ").
                append("id: '").append(getID()).append("', ").
                append("pushIDTimeout: '").append(getPushIDTimeout()).append("', ").
                append("subID: '").append(getSubID()).append("'").
                    toString();
    }

    protected void discard(final InternalPushGroupManager internalPushGroupManager) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "PushID '" + getID() + "' discarded.");
            }
            internalPushGroupManager.removePushID(getID());
            internalPushGroupManager.removePendingNotification(getID());
            for (final String _groupName : getModifiableGroupSet()) {
                Group _group =
                    internalPushGroupManager.getGroup(_groupName);
                if (_group != null) {
                    _group.removePushID(getID(), internalPushGroupManager);
                }
            }
    }

    protected Set<String> getModifiableGroupSet() {
        return groupSet;
    }

    protected static InternalPushGroupManager getInternalPushGroupManager() {
        return
            (InternalPushGroupManager)PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName());
    }

    protected boolean setBrowserID(final String browserID) {
        return setBrowserID(browserID, true);
    }

    protected boolean setDatabaseID(final String databaseID) {
        return setDatabaseID(databaseID, true);
    }

    protected boolean setID(final String id) {
        return setID(id, true);
    }

    protected boolean setSubID(final String subID) {
        return setSubID(subID, true);
    }

    protected boolean startExpiryTimeout(
        final InternalPushGroupManager internalPushGroupManager) {

        return internalPushGroupManager.startExpiryTimeout(getID());
    }

    protected boolean startExpiryTimeout(
        final InternalPushGroupManager internalPushGroupManager, final long timeout) {

        return internalPushGroupManager.startExpiryTimeout(getID(), timeout);
    }

    protected boolean startExpiryTimeout(
        final InternalPushGroupManager internalPushGroupManager, final String browserID, final long timeout) {

        return internalPushGroupManager.startExpiryTimeout(getID(), browserID, timeout);
    }

    private boolean setBrowserID(final String browserID, final boolean save) {
        boolean _modified;
        if ((this.browserID == null && browserID != null) ||
            (this.browserID != null && !this.browserID.equals(browserID))) {

            this.browserID = browserID;
            _modified = true;
            if (save) {
                save();
            }
        } else {
            _modified = false;
        }
        return _modified;
    }

    private boolean setDatabaseID(final String databaseID, final boolean save) {
        boolean _modified;
        if ((this.databaseID == null && databaseID != null) ||
            (this.databaseID != null && !this.databaseID.equals(databaseID))) {

            this.databaseID = databaseID;
            _modified = true;
            if (save) {
                save();
            }
        } else {
            _modified = false;
        }
        return _modified;
    }

    private boolean setID(final String id, final boolean save) {
        boolean _modified;
        if ((this.id == null && id != null) ||
            (this.id != null && !this.id.equals(id))) {

            this.id = id;
            _modified = true;
            if (save) {
                save();
            }
        } else {
            _modified = false;
        }
        return _modified;
    }

    private boolean setPushIDTimeout(final long pushIDTimeout, final boolean save) {
        boolean _modified;
        if (this.pushIDTimeout != pushIDTimeout) {
            this.pushIDTimeout = pushIDTimeout;
            _modified = true;
            if (save) {
                save();
            }
        } else {
            _modified = false;
        }
        return _modified;
    }

    private boolean setSubID(final String subID, final boolean save) {
        boolean _modified;
        if ((this.subID == null && subID != null) ||
            (this.subID != null && !this.subID.equals(subID))) {

            this.subID = subID;
            _modified = true;
            if (save) {
                save();
            }
        } else {
            _modified = false;
        }
        return _modified;
    }
}
