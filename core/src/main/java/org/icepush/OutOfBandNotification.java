package org.icepush;

import static org.icesoft.util.ObjectUtilities.isNotNull;
import static org.icesoft.util.StringUtilities.isNotNullAndIsNotEmpty;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

    private Map<String, Object> propertyMap = new HashMap<String, Object>();

    private boolean forced;

    public OutOfBandNotification() {
        super();
        // Do nothing.
    }

    public OutOfBandNotification(
        final String groupName, final String payload, final Map<String, Object> propertyMap, final long duration,
        final long scheduledAt, final Set<String> exemptPushIDSet) {

        this(groupName, payload, propertyMap, false, duration, scheduledAt, exemptPushIDSet, true);
    }

    public OutOfBandNotification(
        final String groupName, final String payload, final Map<String, Object> propertyMap, final boolean forced,
        final long duration, final long scheduledAt, final Set<String> exemptPushIDSet) {

        this(groupName, payload, propertyMap, forced, duration, scheduledAt, exemptPushIDSet, true);
    }

    public OutOfBandNotification(
        final String groupName, final String payload, final PushConfiguration pushConfiguration) {

        this(groupName, payload, pushConfiguration, true);
    }

    protected OutOfBandNotification(
        final String groupName, final String payload, final Map<String, Object> propertyMap, final boolean forced,
        final long duration, final long scheduledAt, final Set<String> exemptPushIDSet, final boolean save) {

        super(groupName, payload, duration, scheduledAt, exemptPushIDSet, false);
        setPropertyMap(propertyMap, false);
        setForced(forced, false);
        if (save) {
            save();
        }
    }

    protected OutOfBandNotification(
        final String groupName, final String payload, final PushConfiguration pushConfiguration, final boolean save) {

        super(groupName, payload, pushConfiguration, false);
        Map<String, Object> _propertyMap = new HashMap<String, Object>();
        Boolean _forced = false;
        for (final Map.Entry<String, Object> _attributeEntry : pushConfiguration.getAttributeMap().entrySet()) {
            // TODO: Actually filter the properties supported by the Cloud Notification Service.
            if (_attributeEntry.getValue() instanceof Boolean) {
                if (_attributeEntry.getKey().equals("forced")) {
                    _forced = (Boolean)_attributeEntry.getValue();
                }
            } else {
                _propertyMap.put(_attributeEntry.getKey(), _attributeEntry.getValue());
            }
        }
        setPropertyMap(_propertyMap, false);
        if (_forced != null) {
            setForced(_forced, false);
        }
        if (save) {
            save();
        }
    }

    @Override
    public boolean equals(final Object object) {
        return
            object instanceof OutOfBandNotification &&
                ((OutOfBandNotification)object).getModifiablePropertyMap().entrySet().containsAll(getModifiablePropertyMap().entrySet()) &&
                ((OutOfBandNotification)object).getModifiablePropertyMap().size() == getModifiablePropertyMap().size() &&
                super.equals(object);
    }

    public Object getProperty(final String key) {
        Object _value;
        if (isNotNullAndIsNotEmpty(key)) {
            _value = getModifiablePropertyMap().get(key);
        } else {
            _value = null;
        }
        return _value;
    }

    public final Map<String, Object> getPropertyMap() {
        return Collections.unmodifiableMap(getModifiablePropertyMap());
    }

    public final boolean isForced() {
        return forced;
    }

    public Object putProperty(final String key, final Object value) {
        Object _previousValue;
        if (isNotNullAndIsNotEmpty(key) && isNotNull(value)) {
            _previousValue = getModifiablePropertyMap().put(key, value);
        } else {
            _previousValue = null;
        }
        return _previousValue;
    }

    public Object removeProperty(final String key) {
        Object _previousValue;
        if (isNotNullAndIsNotEmpty(key)) {
            _previousValue = getModifiablePropertyMap().remove(key);
        } else {
            _previousValue = null;
        }
        return _previousValue;
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
    protected String classMembersToString() {
        return
            new StringBuilder().
                append("forced: '").append(isForced()).append("', ").
                append("propertyMap: '").append(getModifiablePropertyMap()).append("', ").
                append(super.classMembersToString()).
                    toString();
    }

    protected final Map<String, Object> getModifiablePropertyMap() {
        return propertyMap;
    }

    @Override
    protected NotificationEntry newNotificationEntry(
        final String pushID) {

        return
            getInternalPushGroupManager().
                newNotificationEntry(pushID, getGroupName(), getPayload(), getPropertyMap(), isForced());
    }

    protected final boolean setForced(final boolean forced) {
        return setForced(forced, true);
    }

    protected final boolean setPropertyMap(final Map<String, Object> propertyMap) {
        return setPropertyMap(propertyMap, true);
    }

    private boolean setForced(final boolean forced, final boolean save) {
        boolean _modified;
        if (this.forced != forced) {
            this.forced = forced;
            _modified = true;
            if (save) {
                save();
            }
        } else {
            _modified = false;
        }
        return _modified;
    }

    private boolean setPropertyMap(final Map<String, Object> propertyMap, final boolean save) {
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
