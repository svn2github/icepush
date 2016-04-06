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

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.util.DatabaseEntity;
import org.icesoft.notify.cloud.core.CloudNotificationService;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Transient;

@Entity(value = "confirmation_timeouts")
public class ConfirmationTimeout
implements DatabaseEntity, Serializable {
    private static final long serialVersionUID = 2707934538409138911L;

    private static final Logger LOGGER = Logger.getLogger(ConfirmationTimeout.class.getName());

    private Map<String, String> propertyMap = new HashMap<String, String>();

    @Id
    private String databaseID;

    @Transient
    private TimerTask timerTask;
    {
        setTimerTask(
            new TimerTask() {
                @Override
                public void run() {
                    execute();
                }
            }
        );
    }

    private String browserID;
    private String groupName;
    private long minCloudPushInterval;
    private long scheduledTime;
    private long timeout;

    public ConfirmationTimeout() {
        // Do nothing.
    }

    public ConfirmationTimeout(
        final String browserID, final String groupName, final Map<String, String> propertyMap, final long timeout,
        final long minCloudPushInterval) {

        this(
            browserID, groupName, propertyMap, timeout, minCloudPushInterval, true
        );
    }

    protected ConfirmationTimeout(
        final String browserID, final String groupName, final Map<String, String> propertyMap, final long timeout,
        final long minCloudPushInterval, final boolean save) {

        setBrowserID(browserID, false);
        setGroupName(groupName, false);
        setPropertyMap(propertyMap, false);
        setTimeout(timeout, false);
        setMinCloudPushInterval(minCloudPushInterval, false);
        // Confirmation Timeout is tied to a Browser-ID.  Therefore, let the databaseID be the browserID.
        this.databaseID = getBrowserID();
        if (save) {
            save();
        }
    }

    public void cancel() {
        cancel(getInternalPushGroupManager());
    }

    public void execute() {
        execute(getInternalPushGroupManager());
    }

    public String getDatabaseID() {
        return databaseID;
    }

    public String getKey() {
        return getDatabaseID();
    }

    public Map<String, String> getPropertyMap() {
        return Collections.unmodifiableMap(getModifiablePropertyMap());
    }

    public void save() {
        ConcurrentMap<String, ConfirmationTimeout> _confirmationTimeoutMap =
            (ConcurrentMap<String, ConfirmationTimeout>)
                PushInternalContext.getInstance().getAttribute("confirmationTimeoutMap");
        if (_confirmationTimeoutMap.containsKey(getKey())) {
            _confirmationTimeoutMap.put(getKey(), this);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Saved Confirmation Timeout '" + this + "' to Database."
                );
            }
        }
    }

    public void schedule(final long scheduledTime) {
        setScheduledTime(scheduledTime);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                    Level.FINE,
                    "Confirmation Timeout for Browser '" + getBrowserID() + "' with " +
                            "Scheduled Time '" + getScheduledTime() + "' scheduled.  " +
                            "(now: '" + new Date(System.currentTimeMillis()) + "')");
        }
        getConfirmationTimer().schedule(getTimerTask(), new Date(getScheduledTime()));
    }

    public void scheduleExecuteOrCancel() {
        scheduleExecuteOrCancel(getInternalPushGroupManager());
    }

    @Override
    public String toString() {
        return
            new StringBuilder().
                append("ConfirmationTimeout[").
                    append(classMembersToString()).
                append("]").
                    toString();
    }

    protected void cancel(final InternalPushGroupManager internalPushGroupManager) {
        getTimerTask().cancel();
        internalPushGroupManager.removeConfirmationTimeout(this);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                Level.FINE,
                "Confirmation Timeout for Browser '" + getBrowserID() + "' at " +
                    "Scheduled Time '" + getScheduledTime() + "' cancelled.  " +
                        "(now: '" + new Date(System.currentTimeMillis()) + "')");
        }
    }

    protected String classMembersToString() {
        return
            new StringBuilder().
                append("browserID: '").append(getBrowserID()).append("', ").
                append("groupName: '").append(getGroupName()).append("', ").
                append("minCloudPushInterval: '").append(getMinCloudPushInterval()).append("', ").
                append("scheduledTime: '").append(new Date(getScheduledTime())).append("', ").
                append("timeout: ").append(getTimeout()).append("'").
                    toString();
    }

    protected void execute(final InternalPushGroupManager internalPushGroupManager) {
        Browser _browser = internalPushGroupManager.getBrowser(getBrowserID());
        NotifyBackURI _notifyBackURI = internalPushGroupManager.getNotifyBackURI(_browser.getNotifyBackURI());
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                Level.FINE,
                "Confirmation Timeout for Browser '" + getBrowserID() + "' occurred.  " +
                    "(URI: '" + _notifyBackURI + "', timeout: '" + getTimeout() + "')");
        }
        try {
            if (_notifyBackURI != null) {
                for (final String _pushIDString : _browser.getPushIDSet()) {
                    internalPushGroupManager.park(_pushIDString, _notifyBackURI.getURI());
                }
                if (_notifyBackURI.getTimestamp() + getMinCloudPushInterval() <= System.currentTimeMillis()) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, "Cloud Push dispatched for Browser '" + getBrowserID() + "'.");
                    }
                    _notifyBackURI.touch();
                    CloudNotificationService _cloudNotificationService =
                        internalPushGroupManager.getCloudNotificationService();
                    if (_cloudNotificationService != null) {
                        if (getPropertyMap().containsKey("targetURI")) {
                            getModifiablePropertyMap().put("uri", getModifiablePropertyMap().remove("targetURI"));
                        }
                        _cloudNotificationService.pushToNotifyBackURI(_notifyBackURI.getURI(), getPropertyMap());
                    } else {
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.log(Level.FINE, "Cloud Notification Service not found.");
                        }
                    }
                    internalPushGroupManager.clearPendingNotifications(_browser.getPushIDSet());
                    _browser.lockLastNotifiedPushIDSet();
                    _browser.lockNotifiedPushIDSet();
                    try {
                        _browser.removeNotifiedPushIDs(_browser.getLastNotifiedPushIDSet());
                    } finally {
                        _browser.unlockNotifiedPushIDSet();
                        _browser.unlockLastNotifiedPushIDSet();
                    }
                }
            }
            _browser.cancelConfirmationTimeout(internalPushGroupManager);
        } catch (final Exception exception) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(
                    Level.WARNING,
                    "Exception caught on confirmationTimeout TimerTask.",
                    exception);
            }
        }
    }

    protected final String getBrowserID() {
        return browserID;
    }

    protected final Timer getConfirmationTimer() {
        return ((Timer)PushInternalContext.getInstance().getAttribute(Timer.class.getName() + "$confirmation"));
    }

    protected final String getGroupName() {
        return groupName;
    }

    protected static InternalPushGroupManager getInternalPushGroupManager() {
        return
            (InternalPushGroupManager)PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName());
    }

    protected final long getMinCloudPushInterval() {
        return minCloudPushInterval;
    }

    protected final Map<String, String> getModifiablePropertyMap() {
        return propertyMap;
    }

    protected final long getScheduledTime() {
        return scheduledTime;
    }

    protected final long getTimeout() {
        return timeout;
    }

    protected final TimerTask getTimerTask() {
        return timerTask;
    }

    protected void scheduleExecuteOrCancel(final InternalPushGroupManager internalPushGroupManager) {
        if (internalPushGroupManager.getBrowser(getBrowserID()) != null) {
            if (System.currentTimeMillis() < getScheduledTime()) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Scheduling Confirmation Timeout for Browser '" + getBrowserID() + "' at " +
                            "Scheduled Time '" + new Date(getScheduledTime()) + "'.  " +
                                "(now: '" + new Date(System.currentTimeMillis()) + "')"
                    );
                }
                schedule(getScheduledTime());
            } else {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Executing Confirmation Timeout for Browser '" + getBrowserID() + "' with " +
                            "Scheduled Time '" + new Date(getScheduledTime()) + "'.  " +
                                "(now: '" + new Date(System.currentTimeMillis()) + "')"
                    );
                }
                execute(internalPushGroupManager);
            }
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Cancelling without executing Confirmation Timeout for Browser '" + getBrowserID() + "' with " +
                        "Scheduled Time '" + new Date(getScheduledTime()) + "' due to " +
                        "unknown Browser '" + getBrowserID() + "'.  " +
                            "(now: '" + new Date(System.currentTimeMillis()) + "')"
                );
            }
            cancel(internalPushGroupManager);
        }
    }

    protected final boolean setBrowserID(final String browserID) {
        return setBrowserID(browserID, true);
    }

    protected final boolean setGroupName(final String groupName) {
        return setGroupName(groupName, true);
    }

    protected final boolean setMinCloudPushInterval(final long minCloudPushInterval) {
        return setMinCloudPushInterval(minCloudPushInterval, true);
    }

    protected final boolean setPropertyMap(final Map<String, String> propertyMap) {
        return setPropertyMap(propertyMap, true);
    }

    protected final boolean setScheduledTime(final long scheduledTime) {
        return setScheduledTime(scheduledTime, true);
    }

    protected final boolean setTimeout(final long timeout) {
        return setTimeout(timeout, true);
    }

    // Must not be persisted!
    protected final void setTimerTask(final TimerTask timerTask) {
        this.timerTask = timerTask;
    }

    private boolean setBrowserID(final String browserID, final boolean save) {
        boolean _modified;
        if ((this.browserID == null && browserID != null) ||
            (this.browserID != null && !this.browserID.equals(browserID))) {

            this.browserID = browserID;
            _modified = true;
            if (save) {
                save();
            }
        } else {
            _modified = false;
        }
        return _modified;
    }

    private boolean setGroupName(final String groupName, final boolean save) {
        boolean _modified;
        if ((this.groupName == null && groupName != null) ||
            (this.groupName != null && !this.groupName.equals(groupName))) {

            this.groupName = groupName;
            _modified = true;
            if (save) {
                save();
            }
        } else {
            _modified = false;
        }
        return _modified;
    }

    private boolean setMinCloudPushInterval(final long minCloudPushInterval, final boolean save) {
        boolean _modified;
        if (this.minCloudPushInterval != minCloudPushInterval) {
            this.minCloudPushInterval = minCloudPushInterval;
            _modified = true;
            if (save) {
                save();
            }
        } else {
            _modified = false;
        }
        return _modified;
    }

    private boolean setPropertyMap(final Map<String, String> propertyMap, final boolean save) {
        boolean _modified;
        if (!this.propertyMap.isEmpty() && propertyMap == null) {
            this.propertyMap.clear();
            _modified = true;
            if (save) {
                save();
            }
        } else if (!this.propertyMap.equals(propertyMap) && propertyMap != null) {
            this.propertyMap.clear();
            this.propertyMap.putAll(propertyMap);
            _modified = true;
            if (save) {
                save();
            }
        } else {
            _modified = false;
        }
        return _modified;
    }

    private boolean setScheduledTime(final long scheduledTime, final boolean save) {
        boolean _modified;
        if (this.scheduledTime != scheduledTime) {
            this.scheduledTime = scheduledTime;
            _modified = true;
            if (save) {
                save();
            }
        } else {
            _modified = false;
        }
        return _modified;
    }

    private boolean setTimeout(final long timeout, final boolean save) {
        boolean _modified;
        if (this.timeout != timeout) {
            this.timeout = timeout;
            _modified = true;
            if (save) {
                save();
            }
        } else {
            _modified = false;
        }
        return _modified;
    }
}
