package org.icepush;

import static org.icesoft.util.ObjectUtilities.isNotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractNotification
implements Serializable {
    private static final long serialVersionUID = 3963771201034347341L;

    private static final Logger LOGGER = Logger.getLogger(AbstractNotification.class.getName());

    private final List<NotificationListener> notificationListenerList = new ArrayList<NotificationListener>();

    protected AbstractNotification() {
        // Do nothing.
    }

    public void addNotificationListener(final NotificationListener listener) {
        if (isNotNull(listener) && !getModifiableNotificationListenerList().contains(listener)) {
            getModifiableNotificationListenerList().add(listener);
        }
    }

    public List<NotificationListener> getNotificationListenerList() {
        return Collections.unmodifiableList(getModifiableNotificationListenerList());
    }

    public void removeNotificationListener(final NotificationListener listener)  {
        if (isNotNull(listener) && getModifiableNotificationListenerList().contains(listener)) {
            getModifiableNotificationListenerList().remove(listener);
        }
    }

    protected List<NotificationListener> getModifiableNotificationListenerList() {
        return notificationListenerList;
    }
}
