package org.icepush;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

public class Browser
implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(Browser.class.getName());

    public static final String BROWSER_ID_NAME = "ice.push.browser";

    private static AtomicInteger browserCounter = new AtomicInteger(0);

    // This counter is only used by LOGGER
    private static AtomicInteger globalConfirmationTimeoutCounter = new AtomicInteger(0);

    private final String id;

    private final transient PushGroupManager pushGroupManager =
        (PushGroupManager)PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName());

    private Set<String> pushIDSet = Collections.emptySet();

    private transient final long minCloudPushInterval;
    private NotifyBackURI notifyBackURI;

    private Status status = newStatus();

    // This counter is only used by LOGGER
    private transient AtomicInteger confirmationTimeoutCounter = new AtomicInteger(0);

    private transient TimerTask confirmationTimeout;
    private transient PushConfiguration pushConfiguration;

    public Browser(final Browser browser) {
        this(browser.getID(), browser.getMinCloudPushInterval());
        setNotifyBackURI(browser.getNotifyBackURI(), false);
        setPushIDSet(browser.getPushIDSet());
        status = new Status(browser.getStatus());
    }

    public Browser(final String id, final long minCloudPushInterval) {
        this.id = id;
        this.minCloudPushInterval = minCloudPushInterval;
    }

    public boolean cancelConfirmationTimeout() {
        if (confirmationTimeout != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Cancel confirmation timeout for Browser '" + id + "'.\r\n\r\n" +
                        "Confirmation Timeout Counter        : " +
                            confirmationTimeoutCounter.decrementAndGet() + "\r\n" +
                        "Global Confirmation Timeout Counter : " +
                            globalConfirmationTimeoutCounter.decrementAndGet() + "\r\n");
            }
            confirmationTimeout.cancel();
            confirmationTimeout = null;
            pushConfiguration = null;
            return true;
        }
        return false;
    }

    public static String generateBrowserID() {
        return Long.toString(browserCounter.incrementAndGet(), 36) + Long.toString(System.currentTimeMillis(), 36);
    }

    public static String getBrowserID(final HttpServletRequest request) {
        String _browserID = getBrowserIDFromHeader(request);
        if (_browserID == null) {
            _browserID = getBrowserIDFromParameter(request);
        }
        return _browserID;
    }

    public String getID() {
        return id;
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

    public boolean setNotifyBackURI(final NotifyBackURI notifyBackURI, final boolean broadcastIfIsNew) {
        boolean _isNew =
            this.notifyBackURI == null ||
            !this.notifyBackURI.getURI().equals(notifyBackURI.getURI());
        if (_isNew) {
            this.notifyBackURI = notifyBackURI;
        } else {
            this.notifyBackURI.touch();
        }
        return _isNew;
    }

    public void setPushConfiguration(final PushConfiguration pushConfiguration) {
        this.pushConfiguration = pushConfiguration;
    }

    public void setPushIDSet(final Set<String> pushIDSet) {
        this.pushIDSet = new HashSet<String>(pushIDSet);
        for (final String _pushIDString : this.pushIDSet) {
            PushID _pushID = pushGroupManager.getPushID(_pushIDString);
            if (_pushID != null)  {
                _pushID.setBrowser(this);
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.log(Level.INFO, "Valid Push-ID " + _pushIDString);
                }
            } else {
                LOGGER.log(Level.INFO, "INVALID Push-ID " + _pushIDString);
            }
        }
    }

    public void setSequenceNumber(final long sequenceNumber) {
        status.setSequenceNumber(sequenceNumber);
    }

    public boolean startConfirmationTimeout(final String groupName) {
        return startConfirmationTimeout(groupName, getSequenceNumber());
    }

    public boolean startConfirmationTimeout(final String groupName, final long sequenceNumber) {
        if (notifyBackURI != null) {
            long now = System.currentTimeMillis();
            long timeout = status.getConnectionRecreationTimeout() * 2;
            LOGGER.log(Level.FINE, "Calculated confirmation timeout: '" + timeout + "'");
            if (notifyBackURI.getTimestamp() + minCloudPushInterval <= now + timeout) {
                return startConfirmationTimeout(groupName, sequenceNumber, timeout);
            } else {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Timeout is within the minimum Cloud Push interval for URI '" + notifyBackURI + "'. (" +
                            "timestamp: '" + notifyBackURI.getTimestamp() + "', " +
                            "minCloudPushInterval: '" + minCloudPushInterval + "', " +
                            "now: '" + now + "', " +
                            "timeout: '" + timeout + "'" +
                        ")");
                }
            }
        }
        return false;
    }

    public boolean startConfirmationTimeout(final String groupName, final long sequenceNumber, final long timeout) {
        if (notifyBackURI != null &&
            notifyBackURI.getTimestamp() + minCloudPushInterval <= System.currentTimeMillis() + timeout &&
            pushConfiguration != null) {

            if (confirmationTimeout == null) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Start confirmation timeout for Browser '" + id + "' (" +
                                "URI: '" + notifyBackURI + "', " +
                                "timeout: '" + timeout + "', " +
                                "sequence number: '" + sequenceNumber + "'" +
                        ").\r\n\r\n" +
                            "Confirmation Timeout Counter        : " +
                                confirmationTimeoutCounter.incrementAndGet() + "\r\n" +
                            "Global Confirmation Timeout Counter : " +
                                globalConfirmationTimeoutCounter.incrementAndGet() + "\r\n");
                }
                try {
                    ((Timer)PushInternalContext.getInstance().getAttribute(Timer.class.getName() + "$confirmation")).
                        schedule(confirmationTimeout = newConfirmationTimeout(groupName, timeout), timeout);
                    return true;
                } catch (final IllegalStateException exception) {
                    // timeoutTimer was cancelled or its timer thread terminated.
                    return false;
                }
            }
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Confirmation timeout already scheduled for PushID '" + id + "' " +
                        "(URI: '" + notifyBackURI + "', timeout: '" + timeout + "').");
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return
            new StringBuilder().
                append("Browser[").
                    append("id: '").append(id).append("', ").
                    append("notifyBackURI: '").append(notifyBackURI).append("', ").
                    append("pushIDSet: '").append(pushIDSet).append("'").
                append("]").
                    toString();
    }

    protected TimerTask getConfirmationTimeout() {
        return confirmationTimeout;
    }

    protected long getMinCloudPushInterval() {
        return minCloudPushInterval;
    }

    protected ConfirmationTimeout newConfirmationTimeout(final String groupName, final long timeout) {
        return new ConfirmationTimeout(this, groupName, timeout, getMinCloudPushInterval());
    }

    protected Status newStatus() {
        return new Status();
    }

    protected void setStatus(final Status status) {
        this.status = status;
    }

    private static String getBrowserIDFromHeader(final HttpServletRequest request) {
        return request.getHeader(BROWSER_ID_NAME);
    }

    private static String getBrowserIDFromParameter(final HttpServletRequest request) {
        return request.getParameter(BROWSER_ID_NAME);
    }

    public static class Status
    implements Serializable {
        private static final Logger LOGGER = Logger.getLogger(Status.class.getName());

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

        public void revertConnectionRecreationTimeout() {
            connectionRecreationTimeout = backupConnectionRecreationTimeout;
        }

        public void setBackupConnectionRecreationTimeout(final long backupConnectionRecreationTimeout) {
            this.backupConnectionRecreationTimeout = backupConnectionRecreationTimeout;
        }

        public void setConnectionRecreationTimeout(final long connectionRecreationTimeout) {
            this.connectionRecreationTimeout = connectionRecreationTimeout;
        }

        public void setSequenceNumber(final long sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
        }

        @Override
        public String toString() {
            return
                new StringBuilder().
                    append("Browser.Status[").append(membersAsString()).append("]").
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

    protected static class ConfirmationTimeout
    extends TimerTask {
        private static final Logger LOGGER = Logger.getLogger(ConfirmationTimeout.class.getName());

        protected final PushGroupManager pushGroupManager =
            (PushGroupManager)PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName());

        protected final Browser browser;
        protected final String groupName;
        protected final long minCloudPushInterval;
        protected final long timeout;

        protected ConfirmationTimeout(
            final Browser browser, final String groupName, final long timeout, final long minCloudPushInterval) {

            this.browser = browser;
            this.groupName = groupName;
            this.timeout = timeout;
            this.minCloudPushInterval = minCloudPushInterval;
        }

        @Override
        public void run() {
            NotifyBackURI _notifyBackURI = browser.getNotifyBackURI();
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Confirmation timeout occurred for Browser '" + browser.getID() + "' " +
                        "(URI: '" + _notifyBackURI + "', timeout: '" + timeout + "').");
            }
            try {
                if (_notifyBackURI != null) {
                    for (final String _pushIDString : browser.getPushIDSet()) {
                        pushGroupManager.park(_pushIDString, _notifyBackURI);
                    }
                    if (_notifyBackURI.getTimestamp() + minCloudPushInterval <= System.currentTimeMillis()) {
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.log(Level.FINE, "Cloud Push dispatched for Browser '" + browser.getID() + "'.");
                        }
                        _notifyBackURI.touch();
                        pushGroupManager.getOutOfBandNotifier().
                            broadcast(
                                new PushNotification(browser.getPushConfiguration().getAttributes()),
                                new Browser[] {
                                    browser
                                },
                                groupName);
                    }
                }
                browser.cancelConfirmationTimeout();
            } catch (Exception exception) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(
                        Level.WARNING,
                        "Exception caught on confirmationTimeout TimerTask.",
                        exception);
                }
            }
        }
    }
}
