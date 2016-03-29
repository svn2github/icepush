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
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.util.DatabaseEntity;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Transient;

@Entity(value = "expiry_timeouts")
public class ExpiryTimeout
implements DatabaseEntity, Serializable {
    private static final long serialVersionUID = 8611173833213209542L;

    private static final Logger LOGGER = Logger.getLogger(ExpiryTimeout.class.getName());

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

    private String pushID;
    private boolean cloudPushID;
    private long scheduledTime;

    public ExpiryTimeout() {
        // Do nothing.
    }

    public ExpiryTimeout(final String pushID, final boolean cloudPushID) {
        this(
            pushID, cloudPushID, true
        );
    }

    protected ExpiryTimeout(final String pushID, final boolean cloudPushID, final boolean save) {
        setPushID(pushID, false);
        setCloudPushID(cloudPushID, false);
        // Expiry Timeout is tied to a Push-ID.  Therefore, let the databaseID be the pushID.
        this.databaseID = getPushID();
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

    public void save() {
        ConcurrentMap<String, ExpiryTimeout> _expiryTimeoutMap =
            (ConcurrentMap<String, ExpiryTimeout>)
                PushInternalContext.getInstance().getAttribute("expiryTimeoutMap");
        if (_expiryTimeoutMap.containsKey(getKey())) {
            _expiryTimeoutMap.put(getKey(), this);
            // TODO: Move to Level.FINE
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Saved Expiry Timeout '" + this + "' to Database."
                );
            }
        }
    }

    public void schedule(final long scheduledTime) {
        setScheduledTime(scheduledTime);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                Level.FINE,
                "Expiry Timeout for Push-ID '" + getPushID() + "' at " +
                    "Scheduled Time '" + getScheduledTime() + "' scheduled.  " +
                        "(now: '" + new Date(System.currentTimeMillis()) + "')"
            );
        }
        ((Timer)PushInternalContext.getInstance().getAttribute(Timer.class.getName() + "$expiry")).
            schedule(getTimerTask(), new Date(scheduledTime));
    }

    @Override
    public String toString() {
        return
            new StringBuilder().
                append("ExpiryTimeout[").
                    append(classMembersToString()).
                append("]").
                    toString();
    }

    protected void cancel(final InternalPushGroupManager internalPushGroupManager) {
        getTimerTask().cancel();
        internalPushGroupManager.removeExpiryTimeout(this);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                Level.FINE,
                "Expiry Timeout for Push-ID '" + getPushID() + "' at " +
                    "Scheduled Time '" + getScheduledTime() + "' cancelled.  " +
                        "(now: '" + new Date(System.currentTimeMillis()) + "')"
            );
        }
    }

    protected String classMembersToString() {
        return
            new StringBuilder().
                append("pushID: '").append(getPushID()).append("', ").
                append("isCloudPushID: '").append(isCloudPushID()).append("'").
                append("scheduledTime: '").append(new Date(getScheduledTime())).append("'").
                    toString();
    }

    protected void execute(final InternalPushGroupManager internalPushGroupManager) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                Level.FINE,
                "Expiry Timeout for PushID '" + getPushID() + "' occurred."
            );
        }
        PushID _pushID = internalPushGroupManager.getPushID(getPushID());
        if (_pushID != null) {
            try {
                _pushID.discard(internalPushGroupManager);
                _pushID.cancelExpiryTimeout(internalPushGroupManager);
            } catch (Exception exception) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(
                        Level.WARNING,
                        "Exception caught on expiryTimeout TimerTask.",
                        exception
                    );
                }
            }
        }
    }

    protected static InternalPushGroupManager getInternalPushGroupManager() {
        return
            (InternalPushGroupManager)PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName());
    }

    protected final String getPushID() {
        return pushID;
    }

    protected final long getScheduledTime() {
        return scheduledTime;
    }

    protected final TimerTask getTimerTask() {
        return timerTask;
    }

    protected final boolean isCloudPushID() {
        return cloudPushID;
    }

    protected void scheduleOrExecute() {
        scheduleOrExecute(getInternalPushGroupManager());
    }

    protected void scheduleOrExecute(final InternalPushGroupManager pushGroupManager) {
        if (System.currentTimeMillis() < getScheduledTime()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Scheduling Expiry Timeout for Push-ID '" + getPushID() + "' at " +
                        "Scheduled Time '" + new Date(getScheduledTime()) + "'.  " +
                            "(now: '" + new Date(System.currentTimeMillis()) + "')"
                );
            }
            schedule(getScheduledTime());
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Executing Expiry Timeout for Push-ID '" + getPushID() + "' with " +
                        "Scheduled Time '" + new Date(getScheduledTime()) + "'.  " +
                            "(now: '" + new Date(System.currentTimeMillis()) + "')"
                );
            }
            execute(pushGroupManager);
        }
    }

    protected final boolean setCloudPushID(final boolean cloudPushID) {
        return setCloudPushID(cloudPushID, true);
    }

    protected final boolean setPushID(final String pushID) {
        return setPushID(pushID, true);
    }

    protected final boolean setScheduledTime(final long scheduledTime) {
        return setScheduledTime(scheduledTime, true);
    }

    // Must not be persisted!
    protected final void setTimerTask(final TimerTask timerTask) {
        this.timerTask = timerTask;
    }

    private boolean setCloudPushID(final boolean cloudPushID, final boolean save) {
        boolean _modified;
        if (this.cloudPushID != cloudPushID) {
            this.cloudPushID = cloudPushID;
            _modified = true;
            if (save) {
                save();
            }
        } else {
            _modified = false;
        }
        return _modified;
    }

    private boolean setPushID(final String pushID, final boolean save) {
        boolean _modified;
        if ((this.pushID == null && pushID != null) ||
            (this.pushID != null && !this.pushID.equals(pushID))) {

            this.pushID = pushID;
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
}
