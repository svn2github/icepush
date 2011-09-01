package org.icepush;

import java.util.EventObject;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PushGroupEvent
extends EventObject {
    private static final Logger LOGGER = Logger.getLogger(PushGroupEvent.class.getName());

    private final String groupName;
    private final String pushId;
    private final Long timestamp;

    public PushGroupEvent(final Object source, final String groupName, final String pushId, final Long timestamp) {
        super(source);
        this.groupName = groupName;
        this.pushId = pushId;
        this.timestamp = timestamp;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getPushID() {
        return pushId;
    }

    public Long getTimestamp() {
        return timestamp;
    }
}
