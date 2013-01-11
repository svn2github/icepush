/*
 * Copyright 2004-2012 ICEsoft Technologies Canada Corp.
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

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LocalNotificationBroadcaster implements NotificationBroadcaster {
    private static final Logger LOGGER = Logger.getLogger(LocalNotificationBroadcaster.class.getName());

    private Set<Receiver> receivers = new CopyOnWriteArraySet<Receiver>();

    public void addReceiver(final Receiver receiver) {
        receivers.add(receiver);
    }

    public void broadcast(final String[] notifiedPushIds) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Local Notification Broadcaster broadcasting " + Arrays.asList(notifiedPushIds));
        }
        for (final Receiver receiver : receivers) {
            receiver.receive(notifiedPushIds);
        }
    }

    public void deleteReceiver(final Receiver observer) {
        receivers.remove(observer);
    }
}
