/*
 * Copyright 2004-2013 ICEsoft Technologies Canada Corp.
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
 *
 */
package org.icepush;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PushConfiguration {
    private Map<String, Object> attributes = new HashMap();
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
}