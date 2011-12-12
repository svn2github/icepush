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
