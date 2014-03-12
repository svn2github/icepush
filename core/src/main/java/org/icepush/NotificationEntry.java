package org.icepush;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotificationEntry
implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(NotificationEntry.class.getName());

    private final String pushID;
    private final String groupName;

    public NotificationEntry(final String pushID, final String groupName) {
        this.pushID = pushID;
        this.groupName = groupName;
    }

    @Override
    public boolean equals(final Object object) {
        return
             object instanceof NotificationEntry &&
             ((NotificationEntry)object).pushID.equals(pushID) &&
             ((NotificationEntry)object).groupName.equals(groupName);
    }

    public String getGroupName() {
        return groupName;
    }

    public String getPushID() {
        return pushID;
    }

    @Override
    public int hashCode() {
        int _result = getPushID() != null ? getPushID().hashCode() : 0;
        _result = 31 * _result + (getGroupName() != null ? getGroupName().hashCode() : 0);
        return _result;
    }

    @Override
    public String toString() {
        return
            new StringBuilder().
                append("NotificationEntry[").
                    append("pushID: '").append(pushID).append("', ").
                    append("groupName: '").append(groupName).append("'").
                append("]").
                    toString();
    }
}
