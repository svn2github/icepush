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

import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExpiryTimeout
extends TimerTask
implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ExpiryTimeout.class.getName());

    protected final InternalPushGroupManager internalPushGroupManager =
        (InternalPushGroupManager)PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName());

    private final String pushID;
    private final boolean isCloudPushID;

    protected ExpiryTimeout(final String pushID, final boolean isCloudPushID) {
        this.pushID = pushID;
        this.isCloudPushID = isCloudPushID;
    }

    @Override
    public void run() {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                Level.FINE,
                "Expiry timeout occurred for PushID '" + getPushID() + "'.");
//            LOGGER.log(
//                Level.FINE,
//                "Expiry timeout occurred for PushID '" + getPushID() + "' (" +
//                    "timeout: '" +
//                        (!isCloudPushID() ? _pushID.getPushIDTimeout() : _pushID.getCloudPushIDTimeout()) +
//                    "').");
        }
        PushID _pushID = internalPushGroupManager.getPushID(getPushID());
        if (_pushID != null) {
            try {
                _pushID.discard();
                _pushID.cancelExpiryTimeout();
            } catch (Exception exception) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(
                        Level.WARNING,
                        "Exception caught on expiryTimeout TimerTask.",
                        exception);
                }
            }
        }
    }

    protected String getPushID() {
        return pushID;
    }

    protected boolean isCloudPushID() {
        return isCloudPushID;
    }
}
