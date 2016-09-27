package org.icepush;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.util.DatabaseEntity;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Entity(value = "notifications")
public class Notification
implements DatabaseEntity, Runnable, Serializable {
    private static final long serialVersionUID = 6628467057491983399L;

    private static final Logger LOGGER = Logger.getLogger(Notification.class.getName());

    @Id
    private String databaseID = UUID.randomUUID().toString();

    private Set<String> exemptPushIDSet = new HashSet<String>();

    private String groupName;
    private String payload;
    private long duration;
    private long scheduledAt;

    public Notification() {
        // Do nothing.
    }

    public Notification(
        final String groupName, final String payload, final long duration, final long scheduledAt,
        final Set<String> exemptPushIDSet) {

        this(
            groupName, payload, duration, scheduledAt, exemptPushIDSet, true
        );
    }

    public Notification(
        final String groupName, final String payload, final PushConfiguration pushConfiguration) {

        this(
            groupName, payload, pushConfiguration, true
        );
    }

    protected Notification(
        final String groupName, final String payload, final long duration, final long scheduledAt,
        final Set<String> exemptPushIDSet, final boolean save) {

        setGroupName(groupName, false);
        setPayload(payload, false);
        setDuration(duration, false);
        setScheduledAt(scheduledAt, false);
        setExemptPushIDSet(exemptPushIDSet, false);
        if (save) {
            save();
        }
    }

    protected Notification(
        final String groupName, final String payload, final PushConfiguration pushConfiguration, final boolean save) {

        this(
            groupName, payload, pushConfiguration.getDuration(), pushConfiguration.getScheduledAt(),
            (Set<String>)pushConfiguration.getAttribute("pushIDSet"), save
        );
    }

    public void coalesceWith(final Notification nextNotification) {
        Group group = getInternalPushGroupManager().getGroup(getGroupName());
        if (group != null) {
            nextNotification.getModifiableExemptPushIDSet().addAll(Arrays.asList(group.getPushIDs()));
        }
    }

    @Override
    public boolean equals(final Object object) {
        return
            object instanceof Notification &&
                ((Notification)object).getDuration() == getDuration() &&
                ((Notification)object).getExemptPushIDSet().containsAll(getExemptPushIDSet()) &&
                ((Notification)object).getExemptPushIDSet().size() == getExemptPushIDSet().size() &&
                ((Notification)object).getGroupName().equals(getGroupName()) &&
                (
                    (((Notification)object).getPayload() == null && getPayload() == null) ||
                    (
                        ((Notification)object).getPayload() != null &&
                        ((Notification)object).getPayload().equals(getPayload())
                    )
                ) &&
                ((Notification)object).getScheduledAt() == getScheduledAt();
    }

    @Override
    public int hashCode() {
        // TODO: Improve this implementation!
        return getGroupName().hashCode();
    }

    public final String getDatabaseID() {
        return databaseID;
    }

    public final long getDuration() {
        return duration;
    }

    public final Set<String> getExemptPushIDSet() {
        return Collections.unmodifiableSet(getModifiableExemptPushIDSet());
    }

    public final String getGroupName() {
        return groupName;
    }

    public final String getKey() {
        return getDatabaseID();
    }

    public final String getPayload() {
        return payload;
    }

    public final long getScheduledAt() {
        return scheduledAt;
    }

    public void run() {
        try {
            Group _group = getInternalPushGroupManager().getGroup(getGroupName());
            if (_group != null) {
                Set<String> _pushIDSet = new HashSet<String>(Arrays.asList(_group.getPushIDs()));
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Notification triggered for Group '" + getGroupName() + "' with " +
                            "original Push-ID Set '" + _pushIDSet + "'."
                    );
                }
                _pushIDSet.removeAll(getExemptPushIDSet());
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Notification triggered for Group '" + getGroupName() + "' with " +
                            "Push-ID Set '" + _pushIDSet + "' after exemption."
                    );
                }
                Set<NotificationEntry> _notificationEntrySet = new HashSet<NotificationEntry>();
                for (final String pushID : _pushIDSet) {
                    _notificationEntrySet.add(newNotificationEntry(pushID));
                }
                filterNotificationEntrySet(_notificationEntrySet);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Notification triggered for Group '" + getGroupName() + "' with " +
                            "Notification Entry Set '" + _notificationEntrySet + "' after filtering."
                    );
                }
                getInternalPushGroupManager().addAllNotificationEntries(_notificationEntrySet);
                beforeBroadcast(_notificationEntrySet);
                getInternalPushGroupManager().
                    broadcastNotificationEntries(_notificationEntrySet, getDuration(), getGroupName());
            }
        } finally {
            getInternalPushGroupManager().scanForExpiry();
        }
    }

    public void save() {
        Datastore _datastore = ((Datastore)PushInternalContext.getInstance().getAttribute(Datastore.class.getName()));
        if (_datastore != null) {
            _datastore.save(this);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Persisted Notification '" + this + "' to datastore."
                );
            }
        }
    }

    @Override
    public String toString() {
        return
            new StringBuilder().
                append("Notification[").
                    append(classMembersToString()).
                append("]").
                    toString();
    }

    protected void beforeBroadcast(final Set<NotificationEntry> notificationEntrySet) {
    }

    protected String classMembersToString() {
        return
            new StringBuilder().
                append("duration: '").append(getDuration()).append("', ").
                append("exemptPushIDSet: '").append(getExemptPushIDSet()).append("', ").
                append("groupName: '").append(getGroupName()).append("', ").
                append("payload: '").append(getPayload()).append("', ").
                append("scheduledAt: '").append(new Date(getScheduledAt())).append("'").
                    toString();
    }

    protected void filterNotificationEntrySet(final Set<NotificationEntry> notificationEntrySet) {
    }

    protected static InternalPushGroupManager getInternalPushGroupManager() {
        return
            (InternalPushGroupManager)PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName());
    }

    protected final Set<String> getModifiableExemptPushIDSet() {
        return exemptPushIDSet;
    }

    protected NotificationEntry newNotificationEntry(
        final String pushID) {

        return
            getInternalPushGroupManager().
                newNotificationEntry(pushID, getGroupName(), getPayload());
    }

    protected final boolean setDuration(final long duration) {
        return setDuration(duration, true);
    }

    protected final boolean setExemptPushIDSet(final Set<String> exemptPushIDSet) {
        return setExemptPushIDSet(exemptPushIDSet, true);
    }

    protected final boolean setGroupName(final String groupName) {
        return setGroupName(groupName, true);
    }

    protected final boolean setPayload(final String payload) {
        return setPayload(payload, true);
    }

    protected final boolean setScheduledAt(final long scheduledAt) {
        return setScheduledAt(scheduledAt, true);
    }

    private boolean setDuration(final long duration, final boolean save) {
        boolean _modified;
        if (this.duration != duration) {
            this.duration = duration;
            _modified = true;
            if (save) {
                save();
            }
        } else {
            _modified = false;
        }
        return _modified;
    }

    private boolean setExemptPushIDSet(final Set<String> exemptPushIDSet, final boolean save) {
        boolean _modified;
        if (!this.exemptPushIDSet.isEmpty() && exemptPushIDSet == null) {
            this.exemptPushIDSet.clear();
            _modified = true;
            if (save) {
                save();
            }
        } else if (!this.exemptPushIDSet.equals(exemptPushIDSet) && exemptPushIDSet != null) {
            this.exemptPushIDSet = exemptPushIDSet;
            _modified = true;
            if (save) {
                save();
            }
        } else {
            _modified = false;
        }
        return _modified;
    }

    private boolean setGroupName(final String groupName, final boolean save) {
        boolean _modified;
        if ((this.groupName == null && groupName != null) ||
            (this.groupName != null && !this.groupName.equals(groupName))) {

            this.groupName = groupName;
            _modified = true;
            if (save) {
                save();
            }
        } else {
            _modified = false;
        }
        return _modified;
    }

    private boolean setPayload(final String payload, final boolean save) {
        boolean _modified;
        if ((this.payload == null && payload != null) ||
            (this.payload != null && !this.payload.equals(payload))) {

            this.payload = payload;
            _modified = true;
            if (save) {
                save();
            }
        } else {
            _modified = false;
        }
        return _modified;
    }

    private boolean setScheduledAt(final long scheduledAt, final boolean save) {
        boolean _modified;
        if (this.scheduledAt != scheduledAt) {
            this.scheduledAt = scheduledAt;
            _modified = true;
            if (save) {
                save();
            }
        } else {
            _modified = false;
        }
        return _modified;
    }
}
