package org.icepush;

public class NotificationEntry {
    private final String pushID;
    private final String groupName;

    public NotificationEntry(final String pushID, final String groupName) {
        this.pushID = pushID;
        this.groupName = groupName;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getPushID() {
        return pushID;
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
