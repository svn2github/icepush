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

    private Map<String, String> propertyMap = new HashMap<String, String>();

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

        this(pushID, groupName, payload, null);
    }

    public NotificationEntry(
        final String pushID, final String groupName, final String payload, final Map<String, String> propertyMap) {

        setPushID(pushID);
        setGroupName(groupName);
        setPayload(payload);
        setPropertyMap(propertyMap);
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
                ((NotificationEntry)object).getPropertyMap().entrySet().containsAll(getPropertyMap().entrySet()) &&
                ((NotificationEntry)object).getPropertyMap().size() == getPropertyMap().size();
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

    public Map<String, String> getPropertyMap() {
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
        return _hashCode;
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
                append("groupName: '").append(getGroupName()).append("', ").
                append("payload: '").append(getPayload()).append("', ").
                append("propertyMap: '").append(getPropertyMap()).append("', ").
                append("pushID: '").append(getPushID()).append("'").
                    toString();
    }

    protected Map<String, String> getModifiablePropertyMap() {
        return propertyMap;
    }

    protected boolean setPropertyMap(final Map<String, String> propertyMap) {
        boolean _modified;
        if (!this.propertyMap.isEmpty() && propertyMap == null) {
            this.propertyMap.clear();
            _modified = true;
//            save();
        } else if (!this.propertyMap.equals(propertyMap) && propertyMap != null) {
            this.propertyMap = propertyMap;
            _modified = true;
//            save();
        } else {
            _modified = false;
        }
        return _modified;
    }

    protected boolean setGroupName(final String groupName) {
        boolean _modified;
        if ((this.groupName == null && groupName != null) ||
            (this.groupName != null && !this.groupName.equals(groupName))) {

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
        if ((this.payload == null && payload != null) ||
            (this.payload != null && !this.payload.equals(payload))) {

            this.payload = payload;
            _modified = true;
//            save();
        } else {
            _modified = false;
        }
        return _modified;
    }

    protected boolean setPushID(final String pushID) {
        boolean _modified;
        if ((this.pushID == null && pushID != null) ||
            (this.pushID != null && !this.pushID.equals(pushID))) {

            this.pushID = pushID;
            _modified = true;
//            save();
        } else {
            _modified = false;
        }
        return _modified;
    }
}
