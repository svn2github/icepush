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

import static org.icesoft.util.StringUtilities.isNotNullAndIsNotEmpty;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.icepush.util.DatabaseEntity;
import org.icesoft.util.Configuration;
import org.icesoft.util.servlet.ServletContextConfiguration;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
//import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Transient;

@Entity(value = "browsers")
public class Browser
implements DatabaseEntity, Serializable {
    private static final long serialVersionUID = 733124798366750761L;

    private static final Logger LOGGER = Logger.getLogger(Browser.class.getName());

    public static final String BROWSER_ID_NAME = "ice.push.browser";
    public static final String BROWSER_TIMEOUT_NAME = "org.icepush.browserTimeout";
    public static final long BROWSER_TIMEOUT_DEFAULT_VALUE = 3L * 365L * 24L * 60L * 60L * 1000L;             // 3 years

    private static AtomicInteger browserCounter = new AtomicInteger(0);

    @Id
    private String databaseID;

    private String id;

    private long lastAccessTimestamp = System.currentTimeMillis();

    @Transient
    private final Lock notifiedPushIDSetLock = new ReentrantLock();

//    @Reference(concreteClass = HashSet.class)
    @Transient
    private Set<NotificationEntry> notifiedPushIDSet = new HashSet<NotificationEntry>();

    @Transient
    private final Lock lastNotifiedPushIDSetLock = new ReentrantLock();

//    @Reference(concreteClass = HashSet.class)
    @Transient
    private Set<NotificationEntry> lastNotifiedPushIDSet = new HashSet<NotificationEntry>();

    private Set<String> pushIDSet = Collections.emptySet();

    private String notifyBackURI;
    private Status status;

    public Browser() {
        // Do nothing.
    }

    public Browser(final Browser browser) {
        this(browser, true);
    }

    public Browser(final String id) {
        this(id, true);
    }

    protected Browser(final Browser browser, final boolean save) {
        this(browser.getID(), false);
        setNotifyBackURI(browser.getNotifyBackURI(), false);
        setLastAccessTimestamp(browser.getLastAccessTimestamp(), false);
        setPushIDSet(browser.getPushIDSet(), false);
        setStatus(newStatus(browser.getStatus(), this), false);
        if (save) {
            save();
        }
    }

    protected Browser(final String id, final boolean save) {
        setID(id, false);
        setDatabaseID(getID(), false);
        // Setting the Status MUST be done last.
        setStatus(newStatus(), false);
        if (save) {
            save();
        }
    }

    public boolean addNotifiedPushIDs(final Collection<NotificationEntry> notifiedPushIDCollection) {
        lockNotifiedPushIDSet();
        try {
            boolean _modified = getModifiableNotifiedPushIDSet().addAll(notifiedPushIDCollection);
            if (_modified) {
                save();
            }
            return _modified;
        } finally {
            unlockNotifiedPushIDSet();
        }
    }

    public boolean cancelConfirmationTimeout(final boolean ignoreForced) {
        return cancelConfirmationTimeout(ignoreForced, getInternalPushGroupManager());
    }

    public boolean clearLastNotifiedPushIDSet() {
        lockLastNotifiedPushIDSet();
        try {
            boolean _modified;
            if (hasLastNotifiedPushIDs()) {
                getModifiableLastNotifiedPushIDSet().clear();
                _modified = true;
                save();
            } else {
                _modified = false;
            }
            return _modified;
        } finally {
            unlockLastNotifiedPushIDSet();
        }
    }

    public static String generateBrowserID() {
        return Long.toString(browserCounter.incrementAndGet(), 36) + Long.toString(System.currentTimeMillis(), 36);
    }

    public static String getBrowserID(final HttpServletRequest request) {
        String browserID = getBrowserIDFromCookie(request);
        if (browserID == null) {
            return getBrowserIDFromParameter(request);
        } else {
            return browserID;
        }
    }

    public String getDatabaseID() {
        return databaseID;
    }

    public String getID() {
        return id;
    }

    public String getKey() {
        return getID();
    }

    public long getLastAccessTimestamp() {
        return lastAccessTimestamp;
    }

    public Set<NotificationEntry> getLastNotifiedPushIDSet() {
        lockLastNotifiedPushIDSet();
        try {
            return Collections.unmodifiableSet(getModifiableLastNotifiedPushIDSet());
        } finally {
            unlockLastNotifiedPushIDSet();
        }
    }

    public Set<NotificationEntry> getNotifiedPushIDSet() {
        lockNotifiedPushIDSet();
        try {
            return Collections.unmodifiableSet(new HashSet<NotificationEntry>(getModifiableNotifiedPushIDSet()));
        } finally {
            unlockNotifiedPushIDSet();
        }
    }

    public String getNotifyBackURI() {
        return notifyBackURI;
    }

    public Set<String> getPushIDSet() {
        return Collections.unmodifiableSet(pushIDSet);
    }

    public long getSequenceNumber() {
        return status.getSequenceNumber();
    }

    public Status getStatus() {
        return status;
    }

    public static long getTimeout(final Configuration configuration) {
        if (configuration != null) {
            String _prefix = configuration.getPrefix();
            if (_prefix == null || _prefix.trim().length() == 0) {
                return configuration.getAttributeAsLong(BROWSER_TIMEOUT_NAME, BROWSER_TIMEOUT_DEFAULT_VALUE);
            } else if (BROWSER_TIMEOUT_NAME.startsWith(_prefix)) {
                String _browserTimeoutName = BROWSER_TIMEOUT_NAME.substring(_prefix.trim().length());
                if (_browserTimeoutName.startsWith(".")) {
                    _browserTimeoutName = _browserTimeoutName.substring(1);
                }
                return configuration.getAttributeAsLong(_browserTimeoutName, BROWSER_TIMEOUT_DEFAULT_VALUE);
            }
        }
        return BROWSER_TIMEOUT_DEFAULT_VALUE;
    }

    public static long getTimeout(final ServletContext servletContext)
    throws IllegalArgumentException {
        return getTimeout(new ServletContextConfiguration(servletContext));
    }

    public boolean hasLastNotifiedPushIDs() {
        lockLastNotifiedPushIDSet();
        try {
            return getLastNotifiedPushIDSet().isEmpty();
        } finally {
            unlockLastNotifiedPushIDSet();
        }
    }

    public boolean hasNotifiedPushIDs() {
        lockNotifiedPushIDSet();
        try {
            return !getNotifiedPushIDSet().isEmpty();
        } finally {
            unlockNotifiedPushIDSet();
        }
    }

    public boolean hasNotifyBackURI() {
        return isNotNullAndIsNotEmpty(getNotifyBackURI());
    }

    public boolean isCloudPushEnabled() {
        InternalPushGroupManager _internalPushGroupManager = getInternalPushGroupManager();
        for (final String _pushIDString : pushIDSet) {
            PushID _pushID = _internalPushGroupManager.getPushID(_pushIDString);
            if (_pushID != null && _pushID.isCloudPushEnabled()) {
                return true;
            }
        }
        return false;
    }

    public boolean removeNotifiedPushIDs(final Collection<NotificationEntry> notifiedPushIDCollection) {
        lockNotifiedPushIDSet();
        try {
            boolean _modified = getModifiableNotifiedPushIDSet().removeAll(notifiedPushIDCollection);
            if (_modified) {
                save();
            }
            return _modified;
        } finally {
            unlockNotifiedPushIDSet();
        }
    }

    public boolean retainNotifiedPushIDs(final Collection<NotificationEntry> notifiedPushIDCollection) {
        lockNotifiedPushIDSet();
        try {
            boolean _modified = getModifiableNotifiedPushIDSet().retainAll(notifiedPushIDCollection);
            if (_modified) {
                save();
            }
            return _modified;
        } finally {
            unlockNotifiedPushIDSet();
        }
    }

    public void save() {
        if (PushInternalContext.getInstance().getAttribute(Datastore.class.getName()) != null) {
            ConcurrentMap<String, Browser> _browserMap =
                (ConcurrentMap<String, Browser>)PushInternalContext.getInstance().getAttribute("browserMap");
            if (_browserMap.containsKey(getKey())) {
                _browserMap.put(getKey(), this);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Saved Browser '" + this + "' to Database."
                    );
                }
            }
        }
    }

    // Solely used by BrowserDispatcher
    public boolean setLastAccessTimestamp(final long lastAccessTimestamp) {
        return setLastAccessTimestamp(lastAccessTimestamp, true);
    }

    public boolean setNotifyBackURI(final String notifyBackURI, final boolean broadcastIfIsNew) {
        return setNotifyBackURI(notifyBackURI, broadcastIfIsNew, true);
    }

    @Override
    public String toString() {
        return
            new StringBuilder().
                append("Browser[").
                    append(classMembersToString()).
                append("]").
                    toString();
    }

    protected boolean cancelConfirmationTimeout(
        final boolean ignoreForced, final InternalPushGroupManager pushGroupManager) {

        return pushGroupManager.cancelConfirmationTimeouts(getID(), getPushIDSet(), ignoreForced);
    }

    protected String classMembersToString() {
        return
            new StringBuilder().
                append("id: '").append(getID()).append("', ").
                append("lastAccessTimestamp: '").append(new Date(getLastAccessTimestamp())).append("', ").
                append("lastNotifiedPushIDSet: '").append(getLastNotifiedPushIDSet()).append("', ").
                append("notifiedPushIDSet: '").append(getNotifiedPushIDSet()).append("', ").
                append("notifyBackURI: '").append(getNotifyBackURI()).append("', ").
                append("pushIDSet: '").append(getPushIDSet()).append("', ").
                append("status: '").append(getStatus()).append(", '").
                append("databaseID: '").append(getDatabaseID()).append("'").
                    toString();
    }

    protected static InternalPushGroupManager getInternalPushGroupManager() {
        return
            (InternalPushGroupManager)PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName());
    }

    protected final Lock getLastNotifiedPushIDSetLock() {
        return lastNotifiedPushIDSetLock;
    }

    protected final Set<NotificationEntry> getModifiableLastNotifiedPushIDSet() {
        return lastNotifiedPushIDSet;
    }

    protected final Set<NotificationEntry> getModifiableNotifiedPushIDSet() {
        return notifiedPushIDSet;
    }

    protected final Lock getNotifiedPushIDSetLock() {
        return notifiedPushIDSetLock;
    }

    protected final void lockLastNotifiedPushIDSet() {
        getLastNotifiedPushIDSetLock().lock();
    }

    protected final void lockNotifiedPushIDSet() {
        getNotifiedPushIDSetLock().lock();
    }

    protected Status newStatus() {
        return new Status(this);
    }

    protected Status newStatus(final Status status, final Browser browser) {
        return new Status(status, browser);
    }

    protected final boolean setDatabaseID(final String databaseID) {
        return setDatabaseID(databaseID, true);
    }

    protected final boolean setID(final String id) {
        return setID(id, true);
    }

    protected boolean setLastNotifiedPushIDSet(final Set<NotificationEntry> lastNotifiedPushIDSet) {
        return setLastNotifiedPushIDSet(lastNotifiedPushIDSet, true);
    }

    protected boolean setPushIDSet(final Set<String> pushIDSet) {
        return setPushIDSet(pushIDSet, true);
    }

    protected boolean setSequenceNumber(final long sequenceNumber) {
        return setSequenceNumber(sequenceNumber, true);
    }

    protected boolean setStatus(final Status status) {
        return setStatus(status, true);
    }

    protected boolean setStatus(final Status status, final boolean save) {
        boolean _modified;
        if ((this.status == null && status != null) ||
            (this.status != null && !this.status.equals(status))) {

            this.status = status;
            _modified = true;
            if (save) {
                save();
            }
        } else {
            _modified = false;
        }
        return _modified;
    }

    protected void unlockLastNotifiedPushIDSet() {
        getLastNotifiedPushIDSetLock().unlock();
    }

    protected void unlockNotifiedPushIDSet() {
        getNotifiedPushIDSetLock().unlock();
    }

    private static String getBrowserIDFromCookie(final HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (BROWSER_ID_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    private static String getBrowserIDFromParameter(final HttpServletRequest request) {
        return request.getParameter(BROWSER_ID_NAME);
    }

    private boolean setDatabaseID(final String databaseID, final boolean save) {
        boolean _modified;
        if ((this.databaseID == null && databaseID != null) ||
            (this.databaseID != null && !this.databaseID.equals(databaseID))) {

            this.databaseID = databaseID;
            _modified = true;
            if (save) {
                save();
            }
        } else {
            _modified = false;
        }
        return _modified;
    }

    private boolean setID(final String id, final boolean save) {
        boolean _modified;
        if ((this.id == null && id != null) ||
            (this.id != null && !this.id.equals(id))) {

            this.id = id;
            _modified = true;
            if (save) {
                save();
            }
        } else {
            _modified = false;
        }
        return _modified;
    }

    private boolean setLastAccessTimestamp(final long lastAccessTimestamp, final boolean save) {
        boolean _modified;
        if (this.lastAccessTimestamp != lastAccessTimestamp) {
            this.lastAccessTimestamp = lastAccessTimestamp;
            _modified = true;
            if (save) {
                save();
            }
        } else {
            _modified = false;
        }
        return _modified;
    }

    private boolean setLastNotifiedPushIDSet(final Set<NotificationEntry> lastNotifiedPushIDSet, final boolean save) {
        lockLastNotifiedPushIDSet();
        try {
            boolean _modified;
            if (!this.lastNotifiedPushIDSet.equals(lastNotifiedPushIDSet)) {
                this.lastNotifiedPushIDSet = new HashSet<NotificationEntry>(lastNotifiedPushIDSet);
                _modified = true;
                if (save) {
                    save();
                }
            } else {
                _modified = false;
            }
            return _modified;
        } finally {
            unlockLastNotifiedPushIDSet();
        }
    }

    private boolean setNotifyBackURI(final String notifyBackURI, final boolean broadcastIfIsNew, final boolean save) {
        boolean _modified;
        if ((this.notifyBackURI == null && notifyBackURI != null) ||
            (this.notifyBackURI != null && !this.notifyBackURI.equals(notifyBackURI))) {

            this.notifyBackURI = notifyBackURI;
            _modified = true;
            if (this.notifyBackURI != null) {
                getInternalPushGroupManager().getNotifyBackURI(this.notifyBackURI).setBrowserID(getID());
            }
            if (save) {
                save();
            }
        } else {
            if (this.notifyBackURI != null) {
                getInternalPushGroupManager().getNotifyBackURI(this.notifyBackURI).touch();
            }
            _modified = false;
        }
        return _modified;
    }

    private boolean setPushIDSet(final Set<String> pushIDSet, final boolean save) {
        boolean _modified;
        if ((this.pushIDSet == null && pushIDSet != null) ||
            (this.pushIDSet != null && !this.pushIDSet.equals(pushIDSet))) {

            this.pushIDSet = new HashSet<String>(pushIDSet);
            _modified = true;
            if (save) {
                save();
            }
        } else {
            _modified = false;
        }
        return _modified;
    }

    private boolean setSequenceNumber(final long sequenceNumber, final boolean save) {
        boolean _modified = status.setSequenceNumber(sequenceNumber);
        if (_modified) {
            if (save) {
                save();
            }
        }
        return _modified;
    }

    public static class Status
    implements Serializable {
        private static final long serialVersionUID = 2530024421926858382L;

        private static final Logger LOGGER = Logger.getLogger(Status.class.getName());

        private String browserID;

        private long backupConnectionRecreationTimeout;
        private long connectionRecreationTimeout = -1;
        private long sequenceNumber = -1;

        public Status() {
            // Do nothing.
        }

        public Status(final Status status, final Browser browser) {
            this(status, browser, true);
        }

        public Status(final Browser browser) {
            this(browser, true);
        }

        protected Status(final Status status, final Browser browser, final boolean save) {
            setBrowserID(browser.getID(), false);
            setBackupConnectionRecreationTimeout(status.getBackupConnectionRecreationTimeout(), false);
            setConnectionRecreationTimeout(status.getConnectionRecreationTimeout(), false);
            setSequenceNumber(status.getSequenceNumber(), false);
            if (save) {
                getBrowser().save();
            }
        }

        protected Status(final Browser browser, final boolean save) {
            setBrowserID(browser.getID(), false);
            if (save) {
                browser.save();
            }
        }

        public void backUpConnectionRecreationTimeout() {
            backupConnectionRecreationTimeout = connectionRecreationTimeout;
        }

        @Override
        public boolean equals(final Object object) {
            return
                object instanceof Status &&
                    ((Status)object).getBackupConnectionRecreationTimeout() == getBackupConnectionRecreationTimeout() &&
                    ((Status)object).getBrowserID().equals(getBrowserID()) &&
                    ((Status)object).getConnectionRecreationTimeout() == getConnectionRecreationTimeout() &&
                    ((Status)object).getSequenceNumber() == getSequenceNumber();
        }

        public long getBackupConnectionRecreationTimeout() {
            return backupConnectionRecreationTimeout;
        }

        public String getBrowserID() {
            return browserID;
        }

        public long getConnectionRecreationTimeout() {
            return connectionRecreationTimeout;
        }

        public long getSequenceNumber() {
            return sequenceNumber;
        }

        @Override
        public int hashCode() {
            int _hashCode = getBrowserID() != null ? getBrowserID().hashCode() : 0;
            _hashCode =
                31 * _hashCode +
                    (int)(getBackupConnectionRecreationTimeout() ^ (getBackupConnectionRecreationTimeout() >>> 32));
            _hashCode =
                31 * _hashCode +
                    (int)(getConnectionRecreationTimeout() ^ (getConnectionRecreationTimeout() >>> 32));
            _hashCode =
                31 * _hashCode +
                    (int)(getSequenceNumber() ^ (getSequenceNumber() >>> 32));
            return _hashCode;
        }

        public boolean revertConnectionRecreationTimeout() {
            return setConnectionRecreationTimeout(getBackupConnectionRecreationTimeout());
        }

        public boolean setBackupConnectionRecreationTimeout(final long backupConnectionRecreationTimeout) {
            return setBackupConnectionRecreationTimeout(backupConnectionRecreationTimeout, true);
        }

        public boolean setConnectionRecreationTimeout(final long connectionRecreationTimeout) {
            return setConnectionRecreationTimeout(connectionRecreationTimeout, true);
        }

        @Override
        public String toString() {
            return
                new StringBuilder().
                    append("Browser.Status[").
                        append(classMembersToString()).
                    append("]").
                        toString();
        }

        protected String classMembersToString() {
            return
                new StringBuilder().
                    append("backupConnectionRecreationTimeout: ").
                        append("'").append(getBackupConnectionRecreationTimeout()).append("', ").
                    append("connectionRecreationTimeout: ").
                        append("'").append(getConnectionRecreationTimeout()).append("', ").
                    append("sequenceNumber: ").
                        append("'").append(getSequenceNumber()).append("'").
                            toString();
        }

        protected Browser getBrowser() {
            return
                (
                    (InternalPushGroupManager)
                        PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName())
                ).getBrowser(getBrowserID());
        }

        protected boolean setBrowserID(final String browserID) {
            return setBrowserID(browserID, true);
        }

        protected boolean setSequenceNumber(final long sequenceNumber) {
            return setSequenceNumber(sequenceNumber, true);
        }

        private boolean setBackupConnectionRecreationTimeout(
            final long backupConnectionRecreationTimeout, final boolean save) {

            boolean _modified;
            if (this.backupConnectionRecreationTimeout != backupConnectionRecreationTimeout) {
                this.backupConnectionRecreationTimeout = backupConnectionRecreationTimeout;

                _modified = true;
                if (save) {
                    getBrowser().save();
                }
            } else {
                _modified = false;
            }
            return _modified;
        }

        private boolean setBrowserID(
            final String browserID, final boolean save) {

            boolean _modified;
            if ((this.browserID == null && browserID != null) ||
                (this.browserID != null && !this.browserID.equals(browserID))) {

                this.browserID = browserID;
                _modified = true;
                if (save) {
                    getBrowser().save();
                }
            } else {
                _modified = false;
            }
            return _modified;
        }

        private boolean setConnectionRecreationTimeout(
            final long connectionRecreationTimeout, final boolean save) {

            boolean _modified;
            if (this.connectionRecreationTimeout != connectionRecreationTimeout) {
                this.connectionRecreationTimeout = connectionRecreationTimeout;

                _modified = true;
                if (save) {
                    getBrowser().save();
                }
            } else {
                _modified = false;
            }
            return _modified;
        }

        private boolean setSequenceNumber(
            final long sequenceNumber, final boolean save) {

            boolean _modified;
            if (this.sequenceNumber != sequenceNumber) {
                this.sequenceNumber = sequenceNumber;

                _modified = true;
                if (save) {
                    getBrowser().save();
                }
            } else {
                _modified = false;
            }
            return _modified;
        }
    }
}
