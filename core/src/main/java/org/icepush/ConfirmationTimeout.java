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

import static org.icesoft.notify.cloud.core.NotificationProvider.Category;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.util.DatabaseEntity;
import org.icesoft.notify.cloud.core.CloudNotificationService;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Transient;

@Entity(value = "confirmation_timeouts")
public class ConfirmationTimeout
implements DatabaseEntity, Serializable {
    private static final long serialVersionUID = 2707934538409138911L;

    private static final Logger LOGGER = Logger.getLogger(ConfirmationTimeout.class.getName());

    @Transient
    private final Lock cloudPushNotificationSetLock = new ReentrantLock();

    private Set<CloudPushNotification> cloudPushNotificationSet = new HashSet<CloudPushNotification>();

    @Id
    private String databaseID;

    private String browserID;

    public ConfirmationTimeout() {
        // Do nothing.
    }

    public ConfirmationTimeout(final String browserID) {
        this(browserID, true);
    }

    protected ConfirmationTimeout(final String browserID, final boolean save) {
        setBrowserID(browserID, false);
        // Confirmation Timeout is tied to a Browser-ID.  Therefore, let the databaseID be the browserID.
        this.databaseID = getBrowserID();
        if (save) {
            save();
        }
    }

    public void cancel(final Set<String> pushIDSet) {
        cancel(pushIDSet, false);
    }

    public void cancel(final Set<String> pushIDSet, final boolean ignoreForced) {
        cancel(pushIDSet, ignoreForced, getInternalPushGroupManager());
    }

    public void cancelAll() {
        cancelAll(false);
    }

    public void cancelAll(final boolean ignoreForced) {
        cancelAll(ignoreForced, getInternalPushGroupManager());
    }

    protected final String getBrowserID() {
        return browserID;
    }

    public final Set<CloudPushNotification> getCloudPushNotificationSet() {
        return Collections.unmodifiableSet(getModifiableCloudPushNotificationSet());
    }

    public final String getDatabaseID() {
        return databaseID;
    }

    public final String getKey() {
        return getDatabaseID();
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

    public void schedule(
        final String pushID, final Map<String, String> propertyMap, final boolean forced, final long timeout) {

        CloudPushNotification _cloudPushNotification =
            newCloudPushNotification(getBrowserID(), pushID, propertyMap, forced, timeout);
        addCloudPushNotification(_cloudPushNotification);
        _cloudPushNotification.schedule();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                Level.FINE,
                "Confirmation Timeout for Push-ID '" + pushID + "' (Browser '" + getBrowserID() + "') with " +
                    "Scheduled Time '" + _cloudPushNotification.getScheduledTime() + "' scheduled.  " +
                        "[now: '" + new Date(System.currentTimeMillis()) + "']");
        }
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

    protected boolean addCloudPushNotification(final CloudPushNotification cloudPushNotification) {
        lockCloudPushNotificationSet();
        try {
            return getModifiableCloudPushNotificationSet().add(cloudPushNotification);
        } finally {
            unlockCloudPushNotificationSet();
        }
    }

    protected void cancel(
        final Set<String> pushIDSet, final boolean ignoreForced,
        final InternalPushGroupManager internalPushGroupManager) {

        lockCloudPushNotificationSet();
        try {
            Iterator<CloudPushNotification> _cloudPushNotificationSetIterator =
                getModifiableCloudPushNotificationSet().iterator();
            while (_cloudPushNotificationSetIterator.hasNext()) {
                CloudPushNotification _cloudPushNotification = _cloudPushNotificationSetIterator.next();
                if (pushIDSet.contains(_cloudPushNotification.getPushID())) {
                    if (_cloudPushNotification.cancel(ignoreForced)) {
                        _cloudPushNotificationSetIterator.remove();
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.log(
                                Level.FINE,
                                "Confirmation Timeout for Push-ID '" + _cloudPushNotification.getPushID() + "' " +
                                    "(Browser '" + getBrowserID() + "') cancelled.  " +
                                        "[now: '" + new Date(System.currentTimeMillis()) + "']");
                        }
                    }
                }
            }
            if (getModifiableCloudPushNotificationSet().isEmpty()) {
                internalPushGroupManager.removeConfirmationTimeout(this);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Confirmation Timeouts for Browser '" + getBrowserID() + "' cancelled.  " +
                            "[now: '" + new Date(System.currentTimeMillis()) + "']");
                }
            }
        } finally {
            unlockCloudPushNotificationSet();
        }
    }

    protected void cancelAll(
        final boolean ignoreForced, final InternalPushGroupManager internalPushGroupManager) {

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                Level.FINE,
                "Cancelling Confirmation Timeouts for Browser '" + getBrowserID() + "'...  " +
                    "[now: '" + new Date(System.currentTimeMillis()) + "']"
            );
        }
        lockCloudPushNotificationSet();
        try {
            Iterator<CloudPushNotification> _cloudPushNotificationSetIterator =
                getModifiableCloudPushNotificationSet().iterator();
            while (_cloudPushNotificationSetIterator.hasNext()) {
                CloudPushNotification _cloudPushNotification = _cloudPushNotificationSetIterator.next();
                if (_cloudPushNotification.cancel(ignoreForced)) {
                    _cloudPushNotificationSetIterator.remove();
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(
                            Level.FINE,
                            "Confirmation Timeout for Push-ID '" + _cloudPushNotification.getPushID() + "' " +
                                "(Browser '" + getBrowserID() + "') cancelled.  " +
                                    "[now: '" + new Date(System.currentTimeMillis()) + "']"
                        );
                    }
                }
            }
            if (getModifiableCloudPushNotificationSet().isEmpty()) {
                internalPushGroupManager.removeConfirmationTimeout(this);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Confirmation Timeouts for Browser '" + getBrowserID() + "' cancelled.  " +
                            "[now: '" + new Date(System.currentTimeMillis()) + "']"
                    );
                }
            }
        } finally {
            unlockCloudPushNotificationSet();
        }
    }

    protected String classMembersToString() {
        return
            new StringBuilder().
                append("browserID: '").append(getBrowserID()).append("', ").
                append("cloudPushNotificationSet: '").append(getCloudPushNotificationSet()).append("'").
                    toString();
    }

    protected final Lock getCloudPushNotificationSetLock() {
        return cloudPushNotificationSetLock;
    }

    protected static InternalPushGroupManager getInternalPushGroupManager() {
        return
            (InternalPushGroupManager)PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName());
    }

    protected final Set<CloudPushNotification> getModifiableCloudPushNotificationSet() {
        return cloudPushNotificationSet;
    }

    protected final void lockCloudPushNotificationSet() {
        getCloudPushNotificationSetLock().lock();
    }

    protected CloudPushNotification newCloudPushNotification(
        final String browserID, final String pushID, final Map<String, String> propertyMap, final boolean forced,
        final long timeout) {

        return new CloudPushNotification(browserID, pushID, propertyMap, forced, timeout);
    }

    protected boolean removeCloudPushNotification(
        final CloudPushNotification cloudPushNotification) {

        return removeCloudPushNotification(cloudPushNotification, getInternalPushGroupManager());
    }

    protected boolean removeCloudPushNotification(
        final CloudPushNotification cloudPushNotification, final InternalPushGroupManager internalPushGroupManager) {

        lockCloudPushNotificationSet();
        try {
            boolean _result;
            if (getModifiableCloudPushNotificationSet().contains(cloudPushNotification)) {
                _result = getModifiableCloudPushNotificationSet().remove(cloudPushNotification);
                if (_result) {
                    if (getModifiableCloudPushNotificationSet().isEmpty()) {
                        internalPushGroupManager.removeConfirmationTimeout(this);
                    }
                }
            } else {
                _result = false;
            }
            return _result;
        } finally {
            unlockCloudPushNotificationSet();
        }
    }

    protected void scheduleExecuteOrCancel(final InternalPushGroupManager internalPushGroupManager) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                Level.FINE,
                "Scheduling, executing or cancelling Configuration Timeouts for Browser '" + getBrowserID() + "'...  " +
                    "[now: '" + new Date(System.currentTimeMillis()) + "']"
            );
        }
        if (internalPushGroupManager.getBrowser(getBrowserID()) != null) {
            lockCloudPushNotificationSet();
            try {
                for (final CloudPushNotification _cloudPushNotification : getModifiableCloudPushNotificationSet()) {
                    _cloudPushNotification.scheduleOrExecute(this, internalPushGroupManager);
                }
            } finally {
                unlockCloudPushNotificationSet();
            }
        } else {
            cancelAll(true, internalPushGroupManager);
        }
    }

    protected final boolean setBrowserID(final String browserID) {
        return setBrowserID(browserID, true);
    }

    protected final void unlockCloudPushNotificationSet() {
        getCloudPushNotificationSetLock().unlock();
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

    private boolean setCloudPushNotificationSet(
        final Set<CloudPushNotification> cloudPushNotificationSet, final boolean save) {

        boolean _modified;
        if (!this.cloudPushNotificationSet.isEmpty() && cloudPushNotificationSet == null) {
            this.cloudPushNotificationSet.clear();
            _modified = true;
            if (save) {
                save();
            }
        } else if (!this.cloudPushNotificationSet.equals(cloudPushNotificationSet) && cloudPushNotificationSet != null) {
            this.cloudPushNotificationSet.clear();
            this.cloudPushNotificationSet.addAll(cloudPushNotificationSet);
            _modified = true;
            if (save) {
                save();
            }
        } else {
            _modified = false;
        }
        return _modified;
    }

    @Embedded
    public static class CloudPushNotification
    implements Serializable {
        private static final long serialVersionUID = 2592022163953943399L;

        private final Map<String, String> propertyMap = new HashMap<String, String>();

        private String browserID;
        private boolean forced;
        private String pushID;
        private long scheduledTime;
        private long timeout;

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

        public CloudPushNotification() {
            // Do nothing.
        }

        public CloudPushNotification(
            final String browserID, final String pushID, final Map<String, String> propertyMap, final boolean forced,
            final long timeout) {

            this(browserID, pushID, propertyMap, forced, timeout, true);
        }

        protected CloudPushNotification(
            final String browserID, final String pushID, final Map<String, String> propertyMap, final boolean forced,
            final long timeout, final boolean save) {

            setBrowserID(browserID, false);
            setPushID(pushID, false);
            setPropertyMap(propertyMap, false);
            setForced(forced, false);
            setTimeout(timeout, false);
            if (save) {
                getConfirmationTimeout().save();
            }
        }

        public boolean cancel() {
            return cancel(false);
        }

        public boolean cancel(final boolean ignoreForced) {
            return cancel(ignoreForced, getInternalPushGroupManager());
        }

        public void execute() {
            execute(getInternalPushGroupManager());
        }

        public final String getBrowserID() {
            return browserID;
        }

        public final Map<String, String> getPropertyMap() {
            return Collections.unmodifiableMap(getModifiablePropertyMap());
        }

        public final String getPushID() {
            return pushID;
        }

        public final long getScheduledTime() {
            return scheduledTime;
        }

        public final long getTimeout() {
            return timeout;
        }

        public final boolean isForced() {
            return forced;
        }

        public void schedule() {
            setScheduledTime(System.currentTimeMillis() + getTimeout());
            getConfirmationTimer().schedule(getTimerTask(), new Date(getScheduledTime()));
        }

        public void reschedule() {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Rescheduling Configuration Timeouts for Browser '" + getBrowserID() + "'...  " +
                        "[now: '" + new Date(System.currentTimeMillis()) + "']"
                );
            }
            getConfirmationTimer().schedule(getTimerTask(), new Date(getScheduledTime()));
        }

        public void scheduleOrExecute() {
            scheduleOrExecute(getInternalPushGroupManager());
        }

        @Override
        public String toString() {
            return
                new StringBuilder().
                    append("CloudPushNotification[").
                        append(classMembersToString()).
                    append("]").
                        toString();
        }

        protected boolean cancel(final boolean ignoreForced, final InternalPushGroupManager internalPushGroupManager) {
            boolean _result;
            if (ignoreForced || !isForced()) {
                getTimerTask().cancel();
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Cloud Push Notification for Push-ID '" + getPushID() + "' " +
                            "(Browser '" + getBrowserID() + "') " +
                                "scheduled for '" + new Date(getScheduledTime()) + "' cancelled.  " +
                                    "[now: '" + new Date(System.currentTimeMillis()) + "']");
                }
                _result = true;
            } else {
                _result = false;
            }
            return _result;
        }

        protected String classMembersToString() {
            return
                new StringBuilder().
                    append("browserID: '").append(getBrowserID()).append("', ").
                    append("forced: '").append(isForced()).append("', ").
                    append("propertyMap: '").append(getPropertyMap()).append("', ").
                    append("pushID: '").append(getPushID()).append("', ").
                    append("scheduledTime: '").append(getScheduledTime()).append("', ").
                    append("timeout: '").append(getTimeout()).append("'").
                        toString();
        }

        protected Map<Category, Map<String, String>> convertPropertyMap(final Map<String, String> propertyMap) {
            Map<Category, Map<String, String>> _categoryToPropertyMap = new HashMap<Category, Map<String, String>>();
            for (final Map.Entry<String, String> _mapEntry : propertyMap.entrySet()) {
                String _key = _mapEntry.getKey();
                String _value = _mapEntry.getValue();
                if (_key.contains("$")) {
                    Category _category =
                        Category.fromValue(_key.substring(0, _key.indexOf("$")).toUpperCase());
                    Map<String, String> _propertyMap;
                    if (_categoryToPropertyMap.containsKey(_category)) {
                        _propertyMap = _categoryToPropertyMap.get(_category);
                    } else {
                        _propertyMap = new HashMap<String, String>();
                        _categoryToPropertyMap.put(_category, _propertyMap);
                    }
                    _propertyMap.put(_key.substring(_key.indexOf("$") + 1), _value);
                } else {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(
                            Level.FINE,
                            "Ignoring Property Map entry with Name '" + _key + "' and Value '" + _value + "'."
                        );
                    }
                }
            }
            return _categoryToPropertyMap;
        }

        protected void execute(
            final ConfirmationTimeout confirmationTimeout, final InternalPushGroupManager internalPushGroupManager) {

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Executing Configuration Timeouts for Browser '" + getBrowserID() + "'...  " +
                        "[now: '" + new Date(System.currentTimeMillis()) + "']"
                );
            }
            Browser _browser = internalPushGroupManager.getBrowser(getBrowserID());
            NotifyBackURI _notifyBackURI = internalPushGroupManager.getNotifyBackURI(_browser.getNotifyBackURI());
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Cloud Push Notification for Push-ID '" + getPushID() + "' " +
                        "(Browser '" + getBrowserID() + "') occurred.  " +
                            "[URI: '" + _notifyBackURI + "', timeout: '" + getTimeout() + "']");
            }
            try {
                if (_notifyBackURI != null) {
                    for (final String _pushIDString : _browser.getPushIDSet()) {
                        internalPushGroupManager.park(_pushIDString, _notifyBackURI.getURI());
                    }
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(
                            Level.FINE,
                            "Cloud Push dispatched for Push-ID '" + getPushID() + "' " +
                                "(Browser '" + getBrowserID() + "')."
                        );
                    }
                    _notifyBackURI.touch();
                    CloudNotificationService _cloudNotificationService =
                        internalPushGroupManager.getCloudNotificationService();
                    if (_cloudNotificationService != null) {
                        if (getPropertyMap().containsKey("targetURI")) {
                            getModifiablePropertyMap().put("url", getModifiablePropertyMap().remove("targetURI"));
                        }
                        _cloudNotificationService.pushToNotifyBackURI(
                            _notifyBackURI.getURI(), convertPropertyMap(getPropertyMap())
                        );
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
                if (cancel(true, internalPushGroupManager)) {
                    confirmationTimeout.removeCloudPushNotification(this, internalPushGroupManager);
                }
            } catch (final Exception exception) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(
                        Level.WARNING,
                        "Exception caught on confirmationTimeout TimerTask.",
                        exception);
                }
            }
        }

        protected void execute(
            final InternalPushGroupManager internalPushGroupManager) {

            execute(getConfirmationTimeout(internalPushGroupManager), internalPushGroupManager);
        }

        protected ConfirmationTimeout getConfirmationTimeout() {
            return getConfirmationTimeout(getInternalPushGroupManager());
        }

        protected ConfirmationTimeout getConfirmationTimeout(final InternalPushGroupManager internalPushGroupManager) {
            return internalPushGroupManager.getConfirmationTimeout(getBrowserID());
        }

        protected final Timer getConfirmationTimer() {
            return ((Timer)PushInternalContext.getInstance().getAttribute(Timer.class.getName() + "$confirmation"));
        }

        protected static InternalPushGroupManager getInternalPushGroupManager() {
            return
                (InternalPushGroupManager)
                    PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName());
        }

        protected final Map<String, String> getModifiablePropertyMap() {
            return propertyMap;
        }

        protected final TimerTask getTimerTask() {
            return timerTask;
        }

        protected void scheduleOrExecute(
            final ConfirmationTimeout confirmationTimeout, final InternalPushGroupManager internalPushGroupManager) {

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Executing or cancelling Configuration Timeouts for Browser '" + getBrowserID() + "'...  " +
                        "[now: '" + new Date(System.currentTimeMillis()) + "']"
                );
            }
            if (System.currentTimeMillis() < getScheduledTime()) {
                reschedule();
            } else {
                execute(confirmationTimeout, internalPushGroupManager);
            }
        }

        protected void scheduleOrExecute(
            final InternalPushGroupManager internalPushGroupManager) {

            scheduleOrExecute(getConfirmationTimeout(internalPushGroupManager), internalPushGroupManager);
        }

        protected final boolean setBrowserID(final String browserID) {
            return setBrowserID(browserID, true);
        }

        protected final boolean setForced(final boolean forced) {
            return setForced(forced, true);
        }

        protected final boolean setPropertyMap(final Map<String, String> propertyMap) {
            return setPropertyMap(propertyMap, true);
        }

        protected final boolean setPushID(final String pushID) {
            return setPushID(pushID, true);
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
                    getConfirmationTimeout().save();
                }
            } else {
                _modified = false;
            }
            return _modified;
        }

        private boolean setForced(final boolean forced, final boolean save) {
            boolean _modified;
            if (this.forced != forced) {
                this.forced = forced;
                _modified = true;
                if (save) {
                    getConfirmationTimeout().save();
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
                    getConfirmationTimeout().save();
                }
            } else if (!this.propertyMap.equals(propertyMap) && propertyMap != null) {
                this.propertyMap.clear();
                this.propertyMap.putAll(propertyMap);
                _modified = true;
                if (save) {
                    getConfirmationTimeout().save();
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
                    getConfirmationTimeout().save();
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
                    getConfirmationTimeout().save();
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
                    getConfirmationTimeout().save();
                }
            } else {
                _modified = false;
            }
            return _modified;
        }
    }
}
