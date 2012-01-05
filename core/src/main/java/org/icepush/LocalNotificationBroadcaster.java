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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.Semaphore;

public class LocalNotificationBroadcaster implements NotificationBroadcaster {
    private static final String[] STRINGS = new String[0];
    private ArrayList<Receiver> receivers = new ArrayList<Receiver>();

    public void addReceiver(Receiver receiver) {
        receivers.add(receiver);
    }

    public String[] broadcast(String[] notifiedPushIds) {
        final int size = receivers.size();
        final Semaphore semaphore = new Semaphore(size, true);
        final HashSet<String> confirmedPushIds = new HashSet();

        try {
            semaphore.acquire(size);
            Confirmation confirmation = new Confirmation() {
                public void handlingConfirmed(String[] ids) {
                    confirmedPushIds.addAll(Arrays.asList(ids));
                    semaphore.release();
                }
            };
            for (Receiver receiver : receivers) {
                receiver.receive(notifiedPushIds, confirmation);
            }

            //block until all notified parties confirm the sending of the pushID notifications
            semaphore.acquire(size);
        } catch (InterruptedException e) {
            return STRINGS;
        }

        return confirmedPushIds.toArray(STRINGS);
    }

    public void deleteReceiver(Receiver observer) {
        receivers.remove(observer);
    }
}
