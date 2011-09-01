/*
 * Version: MPL 1.1
 *
 * "The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is ICEfaces 1.5 open source software code, released
 * November 5, 2006. The Initial Developer of the Original Code is ICEsoft
 * Technologies Canada, Corp. Portions created by ICEsoft are Copyright (C)
 * 2004-2010 ICEsoft Technologies Canada, Corp. All Rights Reserved.
 *
 * Contributor(s): _____________________.
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
