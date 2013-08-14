package org.icepush;

import java.util.EventObject;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotificationEvent
extends EventObject {
    private static final Logger LOGGER = Logger.getLogger(NotificationEvent.class.getName());

    private final Browser browser;
    private final PushID pushID;

    public NotificationEvent(final Browser browser, final Object source) {
        this(browser, null, source);
    }

    public NotificationEvent(final PushID pushID, final Object source) {
        this(null, pushID, source);
    }

    private NotificationEvent(final Browser browser, final PushID pushID, final Object source) {
        super(source);
        this.browser = browser;
        this.pushID = pushID;
    }

    public Browser getBrowser() {
        return browser;
    }

    public PushID getPushID() {
        return pushID;
    }
}
