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
    public PushConfiguration(final Map<String, Object> attributeMap)  {
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
        String delay = request.getParameter("delay");
        if (delay != null) {
            String duration = request.getParameter("duration");
            if (duration != null) {
                if (_pushConfiguration == null) {
                    _pushConfiguration = new PushConfiguration();
                }
                _pushConfiguration.delayed(Long.parseLong(delay), Long.parseLong(duration));
            }
        }
        String at = request.getParameter("at");
        if (at != null) {
            String duration = request.getParameter("duration");
            if (duration != null) {
                if (_pushConfiguration == null) {
                    _pushConfiguration = new PushConfiguration();
                }
                _pushConfiguration.scheduled(new Date(Long.parseLong(at)), Long.parseLong(duration));
            }
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

    public long getScheduledAt() {
        return scheduledAt;
    }

    public long getDuration() {
        return duration;
    }

    @Override
    public int hashCode() {
        return getAttributeMap().hashCode();
    }

    public Object putAttribute(final String key, final Object value) {
        return getModifiableAttributeMap().put(key, value);
    }

    public void putAllAttributes(final Map<String, Object> attributeMap) {
        for (final Map.Entry<String, Object> _attributeEntry : attributeMap.entrySet()) {
            putAttribute(_attributeEntry.getKey(), _attributeEntry.getValue());
        }
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
}
