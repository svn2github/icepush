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

import static org.icesoft.util.MapUtilities.isNotNullAndIsNotEmpty;
import static org.icesoft.util.ObjectUtilities.isNotEqual;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
//import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

//import org.icepush.util.DatabaseEntity;

//import org.mongodb.morphia.Datastore;
//import org.mongodb.morphia.annotations.Entity;
//import org.mongodb.morphia.annotations.Id;

//@Entity(value = "notification_entries")
public class NotificationEntry
implements /*DatabaseEntity, */Serializable {
    private static final long serialVersionUID = -1062640428098513163L;

    private static final Logger LOGGER = Logger.getLogger(NotificationEntry.class.getName());

    private final Map<String, Object> propertyMap = new HashMap<String, Object>();

    private boolean cloudNotificationForced;

//    @Id
//    private String databaseID = UUID.randomUUID().toString();

    private String pushID;
    private String groupName;
    private String payload;

    public NotificationEntry() {
        // Do nothing.
    }

    public NotificationEntry(
        final String pushID, final String groupName, final String payload) {

        this(pushID, groupName, payload, null, false);
    }

    public NotificationEntry(
        final String pushID, final String groupName, final String payload, final Map<String, Object> propertyMap) {

        this(pushID, groupName, payload, propertyMap, false);
    }

    public NotificationEntry(
        final String pushID, final String groupName, final String payload, final Map<String, Object> propertyMap,
        final boolean cloudNotificationForced) {

        setPushID(pushID);
        setGroupName(groupName);
        setPayload(payload);
        setPropertyMap(propertyMap);
        setCloudNotificationForced(cloudNotificationForced);
    }

    @Override
    public boolean equals(final Object object) {
        return
            object instanceof NotificationEntry &&
                ((NotificationEntry)object).getPushID().equals(getPushID()) &&
                ((NotificationEntry)object).getGroupName().equals(getGroupName()) &&
                (
                    (((NotificationEntry)object).getPayload() == null && getPayload() == null) ||
                    (
                        ((NotificationEntry)object).getPayload() != null &&
                        ((NotificationEntry)object).getPayload().equals(getPayload())
                    )
                ) &&
                ((NotificationEntry)object).getModifiablePropertyMap().entrySet().containsAll(getModifiablePropertyMap().entrySet()) &&
                ((NotificationEntry)object).getModifiablePropertyMap().size() == getModifiablePropertyMap().size() &&
                ((NotificationEntry)object).isCloudNotificationForced() == isCloudNotificationForced();
    }

//    public String getDatabaseID() {
//        return databaseID;
//    }

    public String getGroupName() {
        return groupName;
    }

//    public String getKey() {
//        return getDatabaseID();
//    }

    public String getPayload() {
        return payload;
    }

    public Map<String, Object> getPropertyMap() {
        return Collections.unmodifiableMap(getModifiablePropertyMap());
    }

    public String getPushID() {
        return pushID;
    }

    @Override
    public int hashCode() {
        int _hashCode = getPushID() != null ? getPushID().hashCode() : 0;
        _hashCode = 31 * _hashCode + (getGroupName() != null ? getGroupName().hashCode() : 0);
        _hashCode = 31 * _hashCode + (getPayload() != null ? getPayload().hashCode() : 0);
        _hashCode = 31 * _hashCode + (getModifiablePropertyMap() != null ? getModifiablePropertyMap().hashCode() : 0);
        _hashCode = 31 * _hashCode + (isCloudNotificationForced() ? 1 : 0);
        return _hashCode;
    }

    public boolean isCloudNotificationForced() {
        return cloudNotificationForced;
    }

//    public void save() {
//        Datastore _datastore = ((Datastore)PushInternalContext.getInstance().getAttribute(Datastore.class.getName()));
//        if (_datastore != null) {
//            _datastore.save(this);
//        }
//    }

    @Override
    public String toString() {
        return
            new StringBuilder().
                append("NotificationEntry[").
                    append(classMembersToString()).
                append("]").
                    toString();
    }

    protected String classMembersToString() {
        return
            new StringBuilder().
                append("cloudNotificationForced: '").append(isCloudNotificationForced()).append("', ").
                append("groupName: '").append(getGroupName()).append("', ").
                append("payload: '").append(getPayload()).append("', ").
                append("propertyMap: '").append(getModifiablePropertyMap()).append("', ").
                append("pushID: '").append(getPushID()).append("'").
                    toString();
    }

    protected Map<String, Object> getModifiablePropertyMap() {
        return propertyMap;
    }

    protected boolean setCloudNotificationForced(final boolean cloudNotificationForced) {
        boolean _modified;
        if (isNotEqual(isCloudNotificationForced(), cloudNotificationForced)) {
            this.cloudNotificationForced = cloudNotificationForced;
            _modified = true;
//            save();
        } else {
            _modified = false;
        }
        return _modified;
    }

    protected boolean setGroupName(final String groupName) {
        boolean _modified;
        if (isNotEqual(getGroupName(), groupName)) {
            this.groupName = groupName;
            _modified = true;
//            save();
        } else {
            _modified = false;
        }
        return _modified;
    }

    protected boolean setPayload(final String payload) {
        boolean _modified;
        if (isNotEqual(getPayload(), payload)) {
            this.payload = payload;
            _modified = true;
//            save();
        } else {
            _modified = false;
        }
        return _modified;
    }

    protected boolean setPropertyMap(final Map<String, Object> propertyMap) {
        boolean _modified;
        if (isNotEqual(getModifiablePropertyMap(), propertyMap)) {
            getModifiablePropertyMap().clear();
            if (isNotNullAndIsNotEmpty(propertyMap)) {
                getModifiablePropertyMap().putAll(propertyMap);
            }
            _modified = true;
//            save();
        } else {
            _modified = false;
        }
        return _modified;
    }

    protected boolean setPushID(final String pushID) {
        boolean _modified;
        if (isNotEqual(getPushID(), pushID)) {
            this.pushID = pushID;
            _modified = true;
//            save();
        } else {
            _modified = false;
        }
        return _modified;
    }
}
