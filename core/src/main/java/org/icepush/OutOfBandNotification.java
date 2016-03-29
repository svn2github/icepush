package org.icepush;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.icepush.util.DatabaseEntity;

import org.mongodb.morphia.annotations.Entity;

@Entity(value = "notifications")
public class OutOfBandNotification
extends Notification
implements DatabaseEntity, Runnable, Serializable {
    private static final long serialVersionUID = -9150842377129767388L;

    private static final Logger LOGGER = Logger.getLogger(OutOfBandNotification.class.getName());

    private Map<String, String> propertyMap = new HashMap<String, String>();

    public OutOfBandNotification() {
        super();
        // Do nothing.
    }

    public OutOfBandNotification(
        final String groupName, final String payload, final Map<String, String> propertyMap, final long duration,
        final long scheduledAt, final Set<String> exemptPushIDSet) {

        this(
            groupName, payload, propertyMap, duration, scheduledAt, exemptPushIDSet, true
        );
    }

    public OutOfBandNotification(
        final String groupName, final String payload, final PushConfiguration pushConfiguration) {

        this(
            groupName, payload, pushConfiguration, true
        );
    }

    protected OutOfBandNotification(
        final String groupName, final String payload, final Map<String, String> propertyMap, final long duration,
        final long scheduledAt, final Set<String> exemptPushIDSet, final boolean save) {

        super(groupName, payload, duration, scheduledAt, exemptPushIDSet, false);
        setPropertyMap(propertyMap, false);
        if (save) {
            save();
        }
    }

    protected OutOfBandNotification(
        final String groupName, final String payload, final PushConfiguration pushConfiguration, final boolean save) {

        super(groupName, payload, pushConfiguration, false);
        Map<String, String> _propertyMap = new HashMap<String, String>();
        for (final Map.Entry<String, Object> _attributeEntry : pushConfiguration.getAttributeMap().entrySet()) {
            // TODO: Actually filter the properties supported by the Cloud Notification Service.
            if (_attributeEntry.getValue() instanceof String) {
                _propertyMap.put(_attributeEntry.getKey(), (String)_attributeEntry.getValue());
            }
        }
        setPropertyMap(_propertyMap, false);
        if (save) {
            save();
        }
    }

    @Override
    public boolean equals(final Object object) {
        return
            object instanceof OutOfBandNotification &&
                ((OutOfBandNotification)object).getPropertyMap().entrySet().containsAll(getPropertyMap().entrySet()) &&
                ((OutOfBandNotification)object).getPropertyMap().size() == getPropertyMap().size() &&
                super.equals(object);
    }

    public final Map<String, String> getPropertyMap() {
        return Collections.unmodifiableMap(getModifiablePropertyMap());
    }

    @Override
    public String toString() {
        return
            new StringBuilder().
                append("OutOfBandNotification[").
                    append(classMembersToString()).
                append("]").
                    toString();
    }

    @Override
    protected void beforeBroadcast(final Set<NotificationEntry> notificationEntrySet) {
        // Only needed for Cloud Push
        getInternalPushGroupManager().startConfirmationTimeouts(notificationEntrySet);
    }

    @Override
    protected String classMembersToString() {
        return
            new StringBuilder().
                append("propertyMap: '").append(getPropertyMap()).append("', ").
                append(super.classMembersToString()).
                    toString();
    }

    protected final Map<String, String> getModifiablePropertyMap() {
        return propertyMap;
    }

    @Override
    protected NotificationEntry newNotificationEntry(
        final String pushID) {

        return newNotificationEntry(pushID, getGroupName(), getPayload(), getPropertyMap());
    }

    protected NotificationEntry newNotificationEntry(
        final String pushID, final String groupName, final String payload, final Map<String, String> propertyMap) {

        return new NotificationEntry(pushID, groupName, payload, propertyMap);
    }

    protected final boolean setPropertyMap(final Map<String, String> propertyMap) {
        return setPropertyMap(propertyMap, true);
    }

    private boolean setPropertyMap(final Map<String, String> propertyMap, final boolean save) {
        boolean _modified;
        if (!this.propertyMap.isEmpty() && propertyMap == null) {
            this.propertyMap.clear();
            _modified = true;
            if (save) {
                save();
            }
        } else if (!this.propertyMap.equals(propertyMap) && propertyMap != null) {
            this.propertyMap = propertyMap;
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
