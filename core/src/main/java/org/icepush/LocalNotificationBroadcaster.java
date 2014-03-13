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

import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LocalNotificationBroadcaster implements NotificationBroadcaster {
    private static final Logger LOGGER = Logger.getLogger(LocalNotificationBroadcaster.class.getName());

    private Set<Receiver> receivers = new CopyOnWriteArraySet<Receiver>();
    private Timer timer = new Timer(true);

    public void addReceiver(final Receiver receiver) {
        receivers.add(receiver);
    }

    public void broadcast(final Set<NotificationEntry> notificationSet, long duration) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Local Notification Broadcaster broadcasting " + notificationSet);
        }

        //collect interested receivers
        ArrayList<Receiver> interestedReceivers = new ArrayList<Receiver>();
        for (final Receiver receiver : receivers) {
            if (receiver.isInterested(notificationSet)) {
                interestedReceivers.add(receiver);
            }
        }
        //spread receiver notification
        if (!interestedReceivers.isEmpty()) {
            long spreadInterval = duration / interestedReceivers.size();
            int index = 0;
            for (final Receiver receiver : interestedReceivers) {
                timer.schedule(new BroadcastTask(receiver, notificationSet), index * spreadInterval);
                index++;
            }
        }
    }

    public void deleteReceiver(final Receiver observer) {
        receivers.remove(observer);
    }

    public void shutdown() {
        timer.cancel();
    }

    private static class BroadcastTask extends TimerTask {
        private final Receiver receiver;
        private final Set<NotificationEntry> notificationSet;

        public BroadcastTask(Receiver receiver, Set<NotificationEntry> notificationSet) {
            this.receiver = receiver;
            this.notificationSet = notificationSet;
        }

        public void run() {
            receiver.receive(notificationSet);
        }
    }
}
