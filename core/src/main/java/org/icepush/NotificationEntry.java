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
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotificationEntry
implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(NotificationEntry.class.getName());

    private final String pushID;
    private final String groupName;
    private final String payload;
    private final PushConfiguration pushConfiguration;

    public NotificationEntry(
        final String pushID, final String groupName, final String payload, final PushConfiguration pushConfiguration) {

        this.pushID = pushID;
        this.groupName = groupName;
        this.payload = payload;
        this.pushConfiguration = pushConfiguration;
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
                );
    }

    public String getGroupName() {
        return groupName;
    }

    public String getPayload() {
        return payload;
    }

    public PushConfiguration getPushConfiguration() {
        return pushConfiguration;
    }

    public String getPushID() {
        return pushID;
    }

    @Override
    public int hashCode() {
        int _hashCode = getPushID() != null ? getPushID().hashCode() : 0;
        _hashCode = 31 * _hashCode + (getGroupName() != null ? getGroupName().hashCode() : 0);
        _hashCode = 31 * _hashCode + (getPayload() != null ? getPayload().hashCode() : 0);
        return _hashCode;
    }

    @Override
    public String toString() {
        return
            new StringBuilder().
                append("NotificationEntry[").
                    append("pushID: '").append(getPushID()).append("', ").
                    append("groupName: '").append(getGroupName()).append("', ").
                    append("pushConfiguration: '").append(getPushConfiguration()).append("'").
                append("]").
                    toString();
    }
}
