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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PushConfiguration
implements Serializable {
    private static final long serialVersionUID = -5770414701296818792L;

    private static final Logger LOGGER = Logger.getLogger(PushConfiguration.class.getName());

    private Map<String, Object> attributes = new HashMap<String, Object>();
    private long scheduledAt = System.currentTimeMillis();
    private long duration = 0;

    /**
     * <p>
     *     Constructs a new PushConfiguration.
     * </p>
     *
     * @see        #PushConfiguration(Map<String, Object>)
     */
    public PushConfiguration()  {
        attributes = new HashMap<String, Object>();
    }

    /**
     * <p>
     *     Constructs a new PushConfiguration with the specified <code>attributes</code>.
     * </p>
     *
     * @param      attributes
     *                 The attributes of the new PushConfiguration to be constructed.
     * @see        #PushConfiguration
     */
    public PushConfiguration(Map<String, Object> attributes)  {
        this.attributes = attributes;
    }

    /**
     * <p>
     *     Gets the attributes of this PushConfiguration.
     * </p>
     *
     * @return     The attributes.
     */
    public Map<String, Object> getAttributes()  {
        return attributes;
    }

    public PushConfiguration delayed(long delay, long duration) {
        this.scheduledAt = System.currentTimeMillis() + delay;
        this.duration = duration;
        return this;
    }

    @Override
    public boolean equals(final Object object) {
        return
            object instanceof PushConfiguration &&
            ((PushConfiguration)object).attributes.equals(attributes) &&
            ((PushConfiguration)object).duration == duration &&
            ((PushConfiguration)object).scheduledAt == scheduledAt;
    }

    public PushConfiguration scheduled(Date time, long duration) {
        this.scheduledAt = time.getTime();
        this.duration = duration;
        return this;
    }

    public long getScheduledAt() {
        return scheduledAt;
    }

    public long getDuration() {
        return duration;
    }

    @Override
    public String toString() {
        return
            new StringBuilder().
                append("PushConfiguration[").
                    append("attributes: '").append(getAttributes()).append("', ").
                    append("duration: '").append(getDuration()).append("', ").
                    append("scheduledAt: '").append(getScheduledAt()).append("'").
                append("]").
                    toString();
    }
}