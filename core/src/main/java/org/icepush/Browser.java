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
import java.util.Map;
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
    public static final long BROWSER_TIMEOUT_DEFAULT_VALUE = 10 * 60 * 1000;

    private static AtomicInteger browserCounter = new AtomicInteger(0);

    @Id
    private String databaseID;

    private String id;

    private long lastAccessTimestamp;

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
        this(browser.getID());
        setNotifyBackURI(browser.getNotifyBackURI(), false);
        setLastAccessTimestamp(browser.getLastAccessTimestamp());
        setPushIDSet(browser.getPushIDSet());
        status = new Status(browser.getStatus(), this);
    }

    public Browser(final String id) {
        this.id = id;
        this.status = newStatus();
        this.databaseID = getID();
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

    public boolean setLastAccessTimestamp(final long lastAccessTimestamp) {
        boolean _modified;
        if (this.lastAccessTimestamp != lastAccessTimestamp) {
            this.lastAccessTimestamp = lastAccessTimestamp;
            _modified = true;
            save();
        } else {
            _modified = false;
        }
        return _modified;
    }

    public boolean setLastNotifiedPushIDSet(final Set<NotificationEntry> lastNotifiedPushIDSet) {
        lockLastNotifiedPushIDSet();
        try {
            boolean _modified;
            if (!this.lastNotifiedPushIDSet.equals(lastNotifiedPushIDSet)) {
                this.lastNotifiedPushIDSet = new HashSet<NotificationEntry>(lastNotifiedPushIDSet);
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

    public boolean setNotifyBackURI(final String notifyBackURI, final boolean broadcastIfIsNew) {
        boolean _modified;
        if ((this.notifyBackURI == null && notifyBackURI != null) ||
            (this.notifyBackURI != null && !this.notifyBackURI.equals(notifyBackURI))) {

            this.notifyBackURI = notifyBackURI;
            _modified = true;
            if (this.notifyBackURI != null) {
                getInternalPushGroupManager().getNotifyBackURI(this.notifyBackURI).setBrowserID(getID());
            }
            save();
        } else {
            if (this.notifyBackURI != null) {
                getInternalPushGroupManager().getNotifyBackURI(this.notifyBackURI).touch();
            }
            _modified = false;
        }
        return _modified;
    }

    public boolean setPushIDSet(final Set<String> pushIDSet) {
        boolean _modified;
        if ((this.pushIDSet == null && pushIDSet != null) ||
            (this.pushIDSet != null && !this.pushIDSet.equals(pushIDSet))) {

            this.pushIDSet = new HashSet<String>(pushIDSet);
            _modified = true;
            save();
        } else {
            _modified = false;
        }
        return _modified;
    }

    public boolean setSequenceNumber(final long sequenceNumber) {
        boolean _modified = status.setSequenceNumber(sequenceNumber);
        if (_modified) {
            save();
        }
        return _modified;
    }

//    public boolean startConfirmationTimeout(
//        final String groupName, final Map<String, String> propertyMap, final boolean forced) {
//
//        return getInternalPushGroupManager().startConfirmationTimeout(getID(), groupName, propertyMap, forced);
//    }

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
                append("status: '").append(getStatus()).append("'").
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
        return new Status(getID());
    }

    protected void setStatus(final Status status) {
        this.status = status;
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

        protected Status(final String browserID) {
            this.browserID = browserID;
        }

        protected Status(final Status status, final Browser browser) {
            setBackupConnectionRecreationTimeout(status.getBackupConnectionRecreationTimeout());
            setConnectionRecreationTimeout(status.getConnectionRecreationTimeout());
            setSequenceNumber(status.getSequenceNumber());
            this.browserID = browser.getID();
        }

        public void backUpConnectionRecreationTimeout() {
            backupConnectionRecreationTimeout = connectionRecreationTimeout;
        }

        public long getBackupConnectionRecreationTimeout() {
            return backupConnectionRecreationTimeout;
        }

        public long getConnectionRecreationTimeout() {
            return connectionRecreationTimeout;
        }

        public long getSequenceNumber() {
            return sequenceNumber;
        }

        public boolean revertConnectionRecreationTimeout() {
            return setConnectionRecreationTimeout(getBackupConnectionRecreationTimeout());
        }

        public boolean setBackupConnectionRecreationTimeout(final long backupConnectionRecreationTimeout) {
            boolean _modified;
            if (this.backupConnectionRecreationTimeout != backupConnectionRecreationTimeout) {
                this.backupConnectionRecreationTimeout = backupConnectionRecreationTimeout;

                _modified = true;
                getBrowser().save();
            } else {
                _modified = false;
            }
            return _modified;
        }

        public boolean setConnectionRecreationTimeout(final long connectionRecreationTimeout) {
            boolean _modified;
            if (this.connectionRecreationTimeout != connectionRecreationTimeout) {
                this.connectionRecreationTimeout = connectionRecreationTimeout;

                _modified = true;
                getBrowser().save();
            } else {
                _modified = false;
            }
            return _modified;
        }

        public boolean setSequenceNumber(final long sequenceNumber) {
            boolean _modified;
            if (this.sequenceNumber != sequenceNumber) {
                this.sequenceNumber = sequenceNumber;

                _modified = true;
                getBrowser().save();
            } else {
                _modified = false;
            }
            return _modified;
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

        protected String getBrowserID() {
            return browserID;
        }
    }
}
