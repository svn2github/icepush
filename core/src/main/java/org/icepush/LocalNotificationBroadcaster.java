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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

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

    private enum ConfirmationStatus { FALSE, TRUE, DELAYED }
    private final Map<Receiver, ConfirmationStatus> receiverConfirmedMap = new HashMap<Receiver, ConfirmationStatus>();
    private final ReentrantLock receiverConfirmedMapLock = new ReentrantLock();

    public void broadcast(final String[] notifiedPushIds, final NotifiedPushIDsHandler notifiedPushIDsHandler) {
        final int size = receivers.size();
        final Map<Receiver, Confirmation> receiverConfirmationMap = new HashMap<Receiver, Confirmation>();
        for (final Receiver receiver : receivers) {
            receiverConfirmedMapLock.lock();
            try {
                if (!receiverConfirmedMap.containsKey(receiver) ||
                    receiverConfirmedMap.get(receiver) == ConfirmationStatus.TRUE) {

                    receiverConfirmedMap.put(receiver, ConfirmationStatus.FALSE);
                    receiverConfirmationMap.put(
                        receiver,
                        new Confirmation() {
                            public void handlingConfirmed(final String[] pushIDs) {
                                receiverConfirmedMapLock.lock();
                                try {
                                    notifiedPushIDsHandler.handle(pushIDs);
                                    if (receiverConfirmedMap.containsKey(receiver)) {
                                        receiverConfirmedMap.put(receiver, ConfirmationStatus.TRUE);
                                    }
                                } finally {
                                    receiverConfirmedMapLock.unlock();
                                }
                            }
                        });
                } else if (
                    receiverConfirmedMap.containsKey(receiver) &&
                    receiverConfirmedMap.get(receiver) == ConfirmationStatus.FALSE) {

                    receiverConfirmedMap.put(receiver, ConfirmationStatus.DELAYED);
                }
            } finally {
                receiverConfirmedMapLock.unlock();
            }
        }
        for (Receiver receiver : receivers) {
            receiverConfirmedMapLock.lock();
            try {
                if (receiverConfirmedMap.get(receiver) == ConfirmationStatus.FALSE) {
                    receiver.receive(notifiedPushIds, receiverConfirmationMap.remove(receiver));
                }
            } finally {
                receiverConfirmedMapLock.unlock();
            }
        }
    }

    public void deleteReceiver(Receiver observer) {
        receivers.remove(observer);
    }
}
