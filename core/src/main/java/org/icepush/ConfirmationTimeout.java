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

public class ConfirmationTimeout
extends TimerTask {
    private static final Logger LOGGER = Logger.getLogger(ConfirmationTimeout.class.getName());

    protected final LocalPushGroupManager localPushGroupManager =
        (LocalPushGroupManager)PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName());

    private final String browserID;
    private final String groupName;
    private final long minCloudPushInterval;
    private final long timeout;

    protected ConfirmationTimeout(
        final String browserID, final String groupName, final long timeout, final long minCloudPushInterval) {

        this.browserID = browserID;
        this.groupName = groupName;
        this.timeout = timeout;
        this.minCloudPushInterval = minCloudPushInterval;
    }

    @Override
    public void run() {
        Browser _browser = localPushGroupManager.getBrowser(getBrowserID());
        NotifyBackURI _notifyBackURI = _browser.getNotifyBackURI();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                Level.FINE,
                "Confirmation timeout occurred for Browser '" + getBrowserID() + "' " +
                    "(URI: '" + _notifyBackURI + "', timeout: '" + getTimeout() + "').");
        }
        try {
            if (_notifyBackURI != null) {
                for (final String _pushIDString : _browser.getPushIDSet()) {
                    localPushGroupManager.park(_pushIDString, _notifyBackURI);
                }
                if (_notifyBackURI.getTimestamp() + getMinCloudPushInterval() <= System.currentTimeMillis()) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, "Cloud Push dispatched for Browser '" + getBrowserID() + "'.");
                    }
                    _notifyBackURI.touch();
                    localPushGroupManager.getOutOfBandNotifier().
                        broadcast(
                            new PushNotification(
                                _browser.getPushConfiguration().getAttributes()),
                            new String[] {
                                getBrowserID()
                            },
                            groupName);
                }
            }
            _browser.cancelConfirmationTimeout();
        } catch (final Exception exception) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(
                    Level.WARNING,
                    "Exception caught on confirmationTimeout TimerTask.",
                    exception);
            }
        }
    }

    protected String getBrowserID() {
        return browserID;
    }

    protected String getGroupName() {
        return groupName;
    }

    protected long getMinCloudPushInterval() {
        return minCloudPushInterval;
    }

    protected long getTimeout() {
        return timeout;
    }
}
