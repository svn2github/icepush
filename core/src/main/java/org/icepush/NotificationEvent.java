package org.icepush;

import static org.icesoft.util.PreCondition.checkIfIsNotNull;
import static org.icesoft.util.PreCondition.checkIfIsNotNullAndIsNotEmpty;

import java.io.Serializable;
import java.util.EventObject;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotificationEvent
extends EventObject
implements Serializable {
    private static final long serialVersionUID = -8059868055890033959L;

    private static final Logger LOGGER = Logger.getLogger(NotificationEvent.class.getName());

    private final Set<NotificationEntry> notificationEntrySet;

    public NotificationEvent(final Object source)
    throws NullPointerException {
        super(
            // throws NullPointerException
            checkIfIsNotNull(source, "Illegal argument source: '" + source + "'.  Argument cannot be null.")
        );
        this.notificationEntrySet = null;
    }

    public NotificationEvent(final Object source, final Set<NotificationEntry> notificationEntrySet)
    throws IllegalArgumentException, NullPointerException {
        super(
            // throws NullPointerException
            checkIfIsNotNull(source, "Illegal argument source: '" + source + "'.  Argument cannot be null.")
        );
        this.notificationEntrySet =
            // throws IllegalArgumentException
            checkIfIsNotNullAndIsNotEmpty(
                notificationEntrySet,
                "Illegal argument notificationEntrySet: '" + notificationEntrySet + "'.  " +
                    "Argument cannot be null or empty."
            );
    }

    public Set<NotificationEntry> getNotificationEntrySet() {
        return notificationEntrySet;
    }
}
