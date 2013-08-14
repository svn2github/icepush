package org.icepush;

import java.util.EventObject;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotificationEvent
extends EventObject {
    private static final Logger LOGGER = Logger.getLogger(NotificationEvent.class.getName());

    private final Browser browser;
    private final PushID pushID;
    private final String groupName;

    public NotificationEvent(final Browser browser, final String groupName, final Object source) {
        this(browser, null, groupName, source);
    }

    public NotificationEvent(final PushID pushID, final String groupName, final Object source) {
        this(null, pushID, groupName, source);
    }

    private NotificationEvent(final Browser browser, final PushID pushID, final String groupName, final Object source) {
        super(source);
        this.browser = browser;
        this.pushID = pushID;
        this.groupName = groupName;
    }

    public Browser getBrowser() {
        return browser;
    }

    public String getGroupName() {
        return groupName;
    }

    public PushID getPushID() {
        return pushID;
    }
}
