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
package org.icepush.jsp.samples.region;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;

import org.icepush.notify.GroupNotifier;

public class GroupNotificationCounter
extends GroupNotifier
implements Serializable {
	private final Map<String, Long> counters = new Hashtable<String, Long>();

    private long interval = 3000; //default to 3 secs

	public GroupNotificationCounter() {
        super();
	}

	public long getCounter() {
		return getCounter(getGroup());
	}

	public long getCounter(final String group) {
		Long start = counters.get(group);
		if (start == null) {
			return 0;
		} else {
			long now = System.currentTimeMillis();
			if( interval < 1 ) { //ensure interval always > 0
				interval = 1000;
            }
			return (now - start) / interval;
		}
	}

    public long getInterval() {
        return interval;
    }

    public void setGroup(final String group) {
        super.setGroup(group);
        if (!counters.containsKey(group)) {
            counters.put(group, System.currentTimeMillis());
        }
    }

    public void setInterval(final long interval) {
        this.interval = interval;
    }
}
