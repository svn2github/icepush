/*
 * Copyright 2004-2014 ICEsoft Technologies Canada Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS
 * IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.icepush;

import static org.icesoft.util.ObjectUtilities.isNotNull;
import static org.icesoft.util.PreCondition.checkArgument;
import static org.icesoft.util.StringUtilities.isNotNullAndIsNotEmpty;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

public class PushConfiguration
implements Serializable {
    private static final long serialVersionUID = -5770414701296818792L;

    private static final Logger LOGGER = Logger.getLogger(PushConfiguration.class.getName());

    private static final Pattern NAME_VALUE = Pattern.compile("\\=");

    private final Map<String, Object> attributeMap = new HashMap<String, Object>();

    private long scheduledAt = System.currentTimeMillis();
    private long duration = 0;

    /**
     * <p>
     *     Constructs a new PushConfiguration.
     * </p>
     *
     * @see        #PushConfiguration(Map)
     */
    public PushConfiguration()  {
    }

    /**
     * <p>
     *     Constructs a new PushConfiguration with the specified <code>attributes</code>.
     * </p>
     *
     * @param      attributeMap
     *                 The attribute map of the new PushConfiguration to be constructed.
     * @see        #PushConfiguration
     */
    public PushConfiguration(final Map<String, Object> attributeMap)
    throws IllegalArgumentException {
        putAllAttributes(attributeMap);
    }

    public boolean containsAttributeKey(final String key) {
        return getModifiableAttributeMap().containsKey(key);
    }

    public PushConfiguration delayed(final long delay, final long duration) {
        setScheduledAt(System.currentTimeMillis() + delay);
        setDuration(duration);
        return this;
    }

    @Override
    public boolean equals(final Object object) {
        return
            object instanceof PushConfiguration &&
                ((PushConfiguration)object).getAttributeMap().equals(getAttributeMap()) &&
                ((PushConfiguration)object).getDuration() == getDuration() &&
                ((PushConfiguration)object).getScheduledAt() == getScheduledAt();
    }

    public static PushConfiguration fromRequest(final HttpServletRequest request) {
        PushConfiguration _pushConfiguration;
        String[] _options = request.getParameterValues("option");
        if (_options != null && _options.length > 0) {
            _pushConfiguration = new PushConfiguration();
            for (final String _option : _options) {
                String[] _nameValuePair = NAME_VALUE.split(_option);
                _pushConfiguration.putAttribute(_nameValuePair[0], _nameValuePair[1]);
            }
        } else {
            _pushConfiguration = null;
        }
        String _delay = request.getParameter("delay");
        if (isNotNullAndIsNotEmpty(_delay)) {
            String _duration = request.getParameter("duration");
            if (isNotNullAndIsNotEmpty(_duration)) {
                if (_pushConfiguration == null) {
                    _pushConfiguration = new PushConfiguration();
                }
                _pushConfiguration.delayed(Long.parseLong(_delay), Long.parseLong(_duration));
            }
        }
        String _at = request.getParameter("at");
        if (isNotNullAndIsNotEmpty(_at)) {
            String _duration = request.getParameter("duration");
            if (isNotNullAndIsNotEmpty(_duration)) {
                if (_pushConfiguration == null) {
                    _pushConfiguration = new PushConfiguration();
                }
                _pushConfiguration.scheduled(new Date(Long.parseLong(_at)), Long.parseLong(_duration));
            }
        }
        /*
         * For now put subject, detail and targetURI in the Push Configuration's attribute map instead of making these
         * 'first class' attributes like at, delay and duration.
         */
        _pushConfiguration =
            putPushNotificationAttribute(
                "global", "detail", request.getParameter("global.detail"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "global", "expireTime", request.getParameter("global.expireTime"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "global", "icon", request.getParameter("global.icon"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "global", "payload", request.getParameter("global.payload"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "global", "priority", request.getParameter("global.priority"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "global", "subject", request.getParameter("global.subject"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "global", "targetURI", request.getParameter("global.targetURI"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "cloud", "detail", request.getParameter("cloud.detail"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "cloud", "expireTime", request.getParameter("cloud.expireTime"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "cloud", "icon", request.getParameter("cloud.icon"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "cloud", "payload", request.getParameter("cloud.payload"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "cloud", "priority", request.getParameter("cloud.priority"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "cloud", "subject", request.getParameter("cloud.subject"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "cloud", "targetURI", request.getParameter("cloud.targetURI"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "email", "detail", request.getParameter("email.detail"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "email", "expireTime", request.getParameter("email.expireTime"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "email", "icon", request.getParameter("email.icon"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "email", "payload", request.getParameter("email.payload"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "email", "priority", request.getParameter("email.priority"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "email", "subject", request.getParameter("email.subject"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "email", "targetURI", request.getParameter("email.targetURI"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "sms", "detail", request.getParameter("sms.detail"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "sms", "expireTime", request.getParameter("sms.expireTime"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "sms", "icon", request.getParameter("sms.icon"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "sms", "payload", request.getParameter("sms.payload"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "sms", "priority", request.getParameter("sms.priority"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "sms", "subject", request.getParameter("sms.subject"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "sms", "targetURI", request.getParameter("sms.targetURI"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "global", "detail", request.getParameter("detail"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "global", "expireTime", request.getParameter("expireTime"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "global", "icon", request.getParameter("icon"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "global", "payload", request.getParameter("payload"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "global", "priority", request.getParameter("priority"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "global", "subject", request.getParameter("subject"), _pushConfiguration
            );
        _pushConfiguration =
            putPushNotificationAttribute(
                "global", "targetURI", request.getParameter("targetURI"), _pushConfiguration
            );
        String _forced = request.getParameter("forced");
        if (isNotNullAndIsNotEmpty(_forced)) {
            if (_pushConfiguration == null) {
                _pushConfiguration = new PushConfiguration();
            }
            _pushConfiguration.putAttribute("forced", Boolean.parseBoolean(_forced));
        }
        return _pushConfiguration;
    }

    public Object getAttribute(final String key) {
        if (getModifiableAttributeMap().containsKey(key)) {
            return getModifiableAttributeMap().get(key);
        } else {
            return null;
        }
    }

    public Map<String, Object> getAttributeMap() {
        return Collections.unmodifiableMap(getModifiableAttributeMap());
    }

    public long getDuration() {
        return duration;
    }

    public long getScheduledAt() {
        return scheduledAt;
    }

    @Override
    public int hashCode() {
        return getModifiableAttributeMap().hashCode();
    }

    public void putAllAttributes(final Map<String, Object> attributeMap)
    throws IllegalArgumentException {
        for (final Map.Entry<String, Object> _attributeEntry : attributeMap.entrySet()) {
            // throws IllegalArgumentException
            putAttribute(_attributeEntry.getKey(), _attributeEntry.getValue());
        }
    }

    public Object putAttribute(final String key, final Object value)
    throws IllegalArgumentException {
        checkArgument(
            isNotNullAndIsNotEmpty(key), "Illegal argument key: '" + key + "'.  Argument cannot be null or empty."
        );
        checkArgument(
            isNotNull(value), "Illegal argument value: '" + value + "'.  Argument cannot be null."
        );
        return getModifiableAttributeMap().put(key, value);
    }

    public Object removeAttribute(final String key) {
        return getModifiableAttributeMap().remove(key);
    }

    public PushConfiguration scheduled(final Date time, final long duration) {
        setScheduledAt(time.getTime());
        setDuration(duration);
        return this;
    }

    @Override
    public String toString() {
        return
            new StringBuilder().
                append("PushConfiguration[").
                    append(classMembersToString()).
                append("]").
                    toString();
    }

    protected String classMembersToString() {
        return
            new StringBuilder().
                append("attributeMap: '").append(getModifiableAttributeMap()).append("', ").
                append("duration: '").append(getDuration()).append("', ").
                append("scheduledAt: '").append(new Date(getScheduledAt())).append("'").
                    toString();
    }

    protected Map<String, Object> getModifiableAttributeMap() {
        return attributeMap;
    }

    protected boolean setDuration(final long duration) {
        boolean _modified;
        if (this.duration != duration) {
            this.duration = duration;
            _modified = true;
        } else {
            _modified = false;
        }
        return _modified;
    }

    protected boolean setScheduledAt(final long scheduledAt) {
        boolean _modified;
        if (this.scheduledAt != scheduledAt) {
            this.scheduledAt = scheduledAt;
            _modified = true;
        } else {
            _modified = false;
        }
        return _modified;
    }

    private static PushConfiguration putPushNotificationAttribute(
        final String category, final String name, final String value, final PushConfiguration pushConfiguration) {

        PushConfiguration _pushConfiguration = pushConfiguration;
        if (isNotNullAndIsNotEmpty(value)) {
            if (_pushConfiguration == null) {
                _pushConfiguration = new PushConfiguration();
            }
            _pushConfiguration.putAttribute(category + "$" + name, value);
        }
        return _pushConfiguration;
    }
}
