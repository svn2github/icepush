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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

public class Browser
implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(Browser.class.getName());

    public static final String BROWSER_ID_NAME = "ice.push.browser";

    private static AtomicInteger browserCounter = new AtomicInteger(0);

    private final String id;
    private final long minCloudPushInterval;
    private final ConcurrentLinkedQueue<NotificationEntry> notifiedPushIDQueue =
        new ConcurrentLinkedQueue<NotificationEntry>();

    private Set<NotificationEntry> lastNotifiedPushIDSet = new HashSet<NotificationEntry>();
    private NotifyBackURI notifyBackURI;
    private PushConfiguration pushConfiguration;
    private Set<String> pushIDSet = Collections.emptySet();
    private Status status;

    public Browser(final Browser browser) {
        this(browser.getID(), browser.getMinCloudPushInterval());
        setNotifyBackURI(browser.getNotifyBackURI(), false);
        setPushIDSet(browser.getPushIDSet());
        status = new Status(browser.getStatus());
    }

    public Browser(final String id, final long minCloudPushInterval) {
        this.id = id;
        this.minCloudPushInterval = minCloudPushInterval;
        this.status = newStatus();
    }

    public boolean addNotifiedPushIDs(final Collection<NotificationEntry> notifiedPushIDCollection) {
        return this.notifiedPushIDQueue.addAll(notifiedPushIDCollection);
    }

    public boolean cancelConfirmationTimeout() {
        return
            ((InternalPushGroupManager)
                PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName())).
                    cancelConfirmationTimeout(getID());
    }

    public static String generateBrowserID() {
        return Long.toString(browserCounter.incrementAndGet(), 36) + Long.toString(System.currentTimeMillis(), 36);
    }

    public static String getBrowserID(final HttpServletRequest request) {
        return getBrowserIDFromParameter(request);
    }

    public String getID() {
        return id;
    }

    public Set<NotificationEntry> getLastNotifiedPushIDSet() {
        return Collections.unmodifiableSet(lastNotifiedPushIDSet);
    }

    public long getMinCloudPushInterval() {
        return minCloudPushInterval;
    }

    public Set<NotificationEntry> getNotifiedPushIDSet() {
        return Collections.unmodifiableSet(new HashSet<NotificationEntry>(notifiedPushIDQueue));
    }

    public NotifyBackURI getNotifyBackURI() {
        return notifyBackURI;
    }

    public PushConfiguration getPushConfiguration() {
        return pushConfiguration;
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

    public boolean hasNotifiedPushIDs() {
        return !notifiedPushIDQueue.isEmpty();
    }

    public boolean isCloudPushEnabled() {
        InternalPushGroupManager _pushGroupManager =
            (InternalPushGroupManager)
                PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName());
        for (final String _pushIDString : pushIDSet) {
            PushID _pushID = _pushGroupManager.getPushID(_pushIDString);
            if (_pushID != null && _pushID.isCloudPushEnabled()) {
                return true;
            }
        }
        return false;
    }

    public boolean removeNotifiedPushIDs(final Collection<NotificationEntry> notifiedPushIDCollection) {
        return this.notifiedPushIDQueue.removeAll(notifiedPushIDCollection);
    }

    public boolean retainNotifiedPushIDs(final Collection<NotificationEntry> notifiedPushIDCollection) {
        return this.notifiedPushIDQueue.retainAll(notifiedPushIDCollection);
    }

    public boolean setLastNotifiedPushIDSet(final Set<NotificationEntry> lastNotifiedPushIDSet) {
        boolean _modified = false;
        if (!this.lastNotifiedPushIDSet.equals(lastNotifiedPushIDSet)) {
            this.lastNotifiedPushIDSet = new HashSet<NotificationEntry>(lastNotifiedPushIDSet);
            _modified = true;
        }
        return _modified;
    }

    public boolean setNotifyBackURI(final NotifyBackURI notifyBackURI, final boolean broadcastIfIsNew) {
        boolean _modified = false;
        if (this.notifyBackURI == null || !this.notifyBackURI.getURI().equals(notifyBackURI.getURI())) {
            this.notifyBackURI = notifyBackURI;
            _modified = true;
        } else {
            this.notifyBackURI.touch();
        }
        return _modified;
    }

    public boolean setPushConfiguration(final PushConfiguration pushConfiguration) {
        boolean _modified = false;
        if ((this.pushConfiguration == null && pushConfiguration != null) ||
            (this.pushConfiguration != null && !this.pushConfiguration.equals(pushConfiguration))) {

            this.pushConfiguration = pushConfiguration;
            _modified = true;
        }
        return _modified;
    }

    public boolean setPushIDSet(final Set<String> pushIDSet) {
        boolean _modified = false;
        if ((this.pushIDSet == null && pushIDSet != null) ||
            (this.pushIDSet != null && !this.pushIDSet.equals(pushIDSet))) {

            this.pushIDSet = new HashSet<String>(pushIDSet);
            _modified = true;
        }
        return _modified;
    }

    public boolean setSequenceNumber(final long sequenceNumber) {
        return status.setSequenceNumber(sequenceNumber);
    }

    public boolean startConfirmationTimeout(final String groupName) {
        return
            ((InternalPushGroupManager)
                PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName())).
                    startConfirmationTimeout(getID(), groupName);
    }

    public boolean startConfirmationTimeout(final String groupName, final long sequenceNumber) {
        return
            ((InternalPushGroupManager)
                PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName())).
                    startConfirmationTimeout(getID(), groupName, sequenceNumber);
    }

    public boolean startConfirmationTimeout(final String groupName, final long sequenceNumber, final long timeout) {
        return
            ((InternalPushGroupManager)
                PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName())).
                    startConfirmationTimeout(getID(), groupName, sequenceNumber, timeout);
    }

    @Override
    public String toString() {
        return
            new StringBuilder().
                append("Browser[").
                    append(membersAsString()).
                append("]").
                    toString();
    }

    protected String membersAsString() {
        return
            new StringBuilder().
                append("id: '").append(getID()).append("', ").
                append("lastNotifiedPushIDSet: '").append(getLastNotifiedPushIDSet()).append("', ").
                append("minCloudPushInterval: '").append(getMinCloudPushInterval()).append("', ").
                append("notifiedPushIDSet: '").append(getNotifiedPushIDSet()).append("', ").
                append("notifyBackURI: '").append(getNotifyBackURI()).append("', ").
                append("pushConfiguration: '").append(getPushConfiguration()).append("', ").
                append("pushIDSet: '").append(getPushIDSet()).append("', ").
                append("status: '").append(getStatus()).append("'").
                    toString();
    }

    protected Status newStatus() {
        return new Status();
    }

    protected void setStatus(final Status status) {
        this.status = status;
    }

    private static String getBrowserIDFromParameter(final HttpServletRequest request) {
        return request.getParameter(BROWSER_ID_NAME);
    }

    public class Status
    implements Serializable {
        private static final long serialVersionUID = 2530024421926858382L;

        private long backupConnectionRecreationTimeout;
        private long connectionRecreationTimeout = -1;
        private long sequenceNumber = -1;

        protected Status() {
            // Do nothing.
        }

        protected Status(final Status status) {
            setBackupConnectionRecreationTimeout(status.getBackupConnectionRecreationTimeout());
            setConnectionRecreationTimeout(status.getConnectionRecreationTimeout());
            setSequenceNumber(status.getSequenceNumber());
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
            if (this.backupConnectionRecreationTimeout != backupConnectionRecreationTimeout) {
                this.backupConnectionRecreationTimeout = backupConnectionRecreationTimeout;

                return true;
            } else {
                return false;
            }
        }

        public boolean setConnectionRecreationTimeout(final long connectionRecreationTimeout) {
            if (this.connectionRecreationTimeout != connectionRecreationTimeout) {
                this.connectionRecreationTimeout = connectionRecreationTimeout;

                return true;
            } else {
                return false;
            }
        }

        public boolean setSequenceNumber(final long sequenceNumber) {
            if (this.sequenceNumber != sequenceNumber) {
                this.sequenceNumber = sequenceNumber;

                return true;
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return
                new StringBuilder().
                    append("Browser.Status[").
                        append(membersAsString()).
                    append("]").
                        toString();
        }

        protected String membersAsString() {
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
    }
}
