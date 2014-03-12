package org.icepush;

import java.util.Map;
import java.util.Set;

public interface InternalPushGroupManager
extends PushGroupManager {
    boolean cancelConfirmationTimeout(String browserID);

    boolean cancelExpiryTimeout(String pushID);

    Map<String, Browser> getBrowserMap();

    Group getGroup(String groupName);

    Map<String, Group> getGroupMap();

    Map<String, PushID> getPushIDMap();

    void groupTouched(String groupName, long lastAccess);

    boolean isParked(String pushID);

    boolean removeGroup(String groupName);

    void removePendingNotification(String pushID);

    void removePendingNotifications(Set<String> pushIDSet);

    boolean removePushID(String pushID);

    boolean startConfirmationTimeout(String browserID, String groupName);

    boolean startConfirmationTimeout(String browserID, String groupName, long sequenceNumber);

    boolean startConfirmationTimeout(String browserID, String groupName, long sequenceNumber, long timeout);

    boolean startExpiryTimeout(String pushID);

    boolean startExpiryTimeout(String pushID, String browserID, long timeout);
}
