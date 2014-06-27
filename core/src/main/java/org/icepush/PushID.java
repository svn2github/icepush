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

public class PushID
implements Serializable {
    private static final long serialVersionUID = 2845881329862716766L;

    private static final Logger LOGGER = Logger.getLogger(PushID.class.getName());

    private final Set<String> groupSet = new HashSet<String>();

    private final String pushID;
    private final String browserID;
    private final String subID;

    private final long cloudPushIDTimeout;
    private final long pushIDTimeout;

    protected PushID(final String pushID, final long pushIDTimeout, final long cloudPushIDTimeout) {
        this.pushID = pushID;
        this.browserID = this.pushID.substring(0, this.pushID.indexOf(':'));
        this.subID = this.pushID.substring(this.pushID.indexOf(':') + 1);
        this.pushIDTimeout = pushIDTimeout;
        this.cloudPushIDTimeout = cloudPushIDTimeout;
    }

    public boolean addToGroup(final String groupName) {
        return groupSet.add(groupName);
    }

    public boolean cancelExpiryTimeout() {
        return
            ((InternalPushGroupManager)
                PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName())
            ).cancelExpiryTimeout(getID());
    }

    public void discard() {
        if (!
            ((InternalPushGroupManager)
                PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName())
            ).isParked(getID())) {

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "PushID '" + getID() + "' discarded.");
            }
            ((InternalPushGroupManager)
                PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName())
            ).removePushID(getID());
            ((InternalPushGroupManager)
                PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName())
            ).removePendingNotification(getID());
            for (String groupName : getGroupSet()) {
                Group group =
                    ((InternalPushGroupManager)
                        PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName())
                    ).getGroup(groupName);
                if (group != null) {
                    group.removePushID(getID());
                }
            }
        }
    }

    public String getBrowserID() {
        return browserID;
    }

    public String getID() {
        return pushID;
    }

    public String getSubID() {
        return subID;
    }

    public boolean removeFromGroup(String groupName) {
        boolean _modified = groupSet.remove(groupName);
        if (groupSet.isEmpty()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE, "Disposed PushID '" + getID() + "' since it no longer belongs to any Push Group.");
            }
            ((InternalPushGroupManager)
                PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName())
            ).removePushID(getID());
        }
        return _modified;
    }

    public boolean startExpiryTimeout() {
        return
            ((InternalPushGroupManager)
                PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName())
            ).startExpiryTimeout(getID());
    }

    public boolean startExpiryTimeout(final String browserID, final long sequenceNumber) {
        return
            ((InternalPushGroupManager)
                PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName())
            ).startExpiryTimeout(getID(), browserID, sequenceNumber);
    }

    @Override
    public String toString() {
        return
            new StringBuilder().
                append("PushID[").
                    append(membersAsString()).
                append("]").
                    toString();
    }

    protected long getCloudPushIDTimeout() {
        return cloudPushIDTimeout;
    }

    protected Set<String> getGroupSet() {
        return groupSet;
    }

    protected long getPushIDTimeout() {
        return pushIDTimeout;
    }

    protected String membersAsString() {
        return
            new StringBuilder().
                append("browserID: '").append(getBrowserID()).append(", ").
                append("cloudPushIDTimeout: '").append(getCloudPushIDTimeout()).append("', ").
                append("groupSet: '").append(getGroupSet()).append("', ").
                append("pushID: '").append(getID()).append("', ").
                append("pushIDTimeout: '").append(getPushIDTimeout()).append("', ").
                append("subID: '").append(getSubID()).append("'").
                    toString();
    }
}
