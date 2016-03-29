package org.icepush;

import java.io.Serializable;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.util.DatabaseEntity;
import org.mongodb.morphia.annotations.Entity;

@Entity(value = "notifications")
public class NoopNotification
extends Notification
implements DatabaseEntity, Runnable, Serializable {
    private static final long serialVersionUID = 1645221039508400366L;

    private static final Logger LOGGER = Logger.getLogger(NoopNotification.class.getName());

    public NoopNotification() {
        super();
        // Do nothing
    }

    public NoopNotification(
        final String groupName, final String payload, final long duration, final long scheduledAt,
        final Set<String> exemptPushIDSet) {

        this(groupName, payload, duration, scheduledAt, exemptPushIDSet, true);
    }

    public NoopNotification(
        final String groupName, final String payload, final PushConfiguration pushConfiguration) {

        this(groupName, payload, pushConfiguration, true);
    }

    protected NoopNotification(
        final String groupName, final String payload, final long duration, final long scheduledAt,
        final Set<String> exemptPushIDSet, final boolean save) {

        super(groupName, payload, duration, scheduledAt, exemptPushIDSet, false);
        if (save) {
            save();
        }
    }

    protected NoopNotification(
        final String groupName, final String payload, final PushConfiguration pushConfiguration, final boolean save) {

        super(groupName, payload, pushConfiguration, false);
        if (save) {
            save();
        }
    }

    @Override
    public boolean equals(final Object object) {
        return
            object instanceof NoopNotification &&
            super.equals(object);
    }

    @Override
    public void run() {
        // Do nothing.
    }

    @Override
    public String toString() {
        return
            new StringBuilder().
                append("NoopNotification[").
                    append(classMembersToString()).
                append("]").
                    toString();
    }
}
