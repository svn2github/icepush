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

import java.util.EventObject;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PushGroupEvent
extends EventObject {
    private static final Logger LOGGER = Logger.getLogger(PushGroupEvent.class.getName());

    private final String groupName;
    private final String pushId;
    private final Long timestamp;

    public PushGroupEvent(final Object source, final String groupName, final String pushId, final Long timestamp) {
        super(source);
        this.groupName = groupName;
        this.pushId = pushId;
        this.timestamp = timestamp;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getPushID() {
        return pushId;
    }

    public Long getTimestamp() {
        return timestamp;
    }
}
