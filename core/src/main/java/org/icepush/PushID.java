package org.icepush;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PushID
implements Serializable {
    private static final long serialVersionUID = 2845881329862716766L;

    private static final Logger LOGGER = Logger.getLogger(PushID.class.getName());

    // This counter is only used by LOGGER
    private static AtomicInteger globalExpiryTimeoutCounter = new AtomicInteger(0);

    private final String id;

    private transient final long cloudPushIDTimeout;
    private transient final Set<String> groups = new HashSet<String>();
    private transient final LocalPushGroupManager localPushGroupManager;
    private transient final long pushIDTimeout;

    private Browser browser;

    private transient TimerTask expiryTimeout;

    // This counter is only used by LOGGER
    private AtomicInteger expiryTimeoutCounter = new AtomicInteger(0);

    protected PushID(
        final String id, final String group, final long pushIDTimeout, final long cloudPushIDTimeout,
        final LocalPushGroupManager localPushGroupManager) {

        this.id = id;
        this.pushIDTimeout = pushIDTimeout;
        this.cloudPushIDTimeout = cloudPushIDTimeout;
        this.localPushGroupManager = localPushGroupManager;
        addToGroup(group);
    }

    public void addToGroup(String group) {
        groups.add(group);
    }

    public boolean cancelExpiryTimeout() {
        if (expiryTimeout != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Cancel expiry timeout for PushID '" + id + "'.\r\n\r\n" +
                        "Expiry Timeout Counter              : " +
                            expiryTimeoutCounter.decrementAndGet() + "\r\n" +
                        "Global Expiry Timeout Counter       : " +
                            globalExpiryTimeoutCounter.decrementAndGet() + "\r\n");
            }
            expiryTimeout.cancel();
            expiryTimeout = null;
            return true;
        }
        return false;
    }

    public void discard() {
        if (!localPushGroupManager.isParked(id)) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "PushID '" + id + "' discarded.");
            }
            localPushGroupManager.removePushID(id);
            localPushGroupManager.removePendingNotification(id);
            for (String groupName : groups) {
                Group group = localPushGroupManager.getGroup(groupName);
                if (group != null) {
                    group.removePushID(id);
                }
            }
        }
    }

    public Browser getBrowser() {
        return browser;
    }

    public String getID() {
        return id;
    }

    public void removeFromGroup(String group) {
        groups.remove(group);
        if (groups.isEmpty()) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE, "Disposed PushID '" + id + "' since it no longer belongs to any Push Group.");
            }
            localPushGroupManager.removePushID(id);
        }
    }

    public void setBrowser(final Browser browser) {
        if (this.browser == null) {
            this.browser = browser;
        }
    }

    public boolean startExpiryTimeout() {
        return startExpiryTimeout(null, browser != null ? browser.getSequenceNumber() : -1);
    }

    public boolean startExpiryTimeout(final Browser browser, final long sequenceNumber) {
        boolean _isCloudPushID = browser != null ? browser.getNotifyBackURI() != null : false;
        if (expiryTimeout == null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Start expiry timeout for PushID '" + id + "' (" +
                            "timeout: '" + (!_isCloudPushID ? pushIDTimeout : cloudPushIDTimeout) + "', " +
                            "sequence number: '" + sequenceNumber + "'" +
                    ").\r\n\r\n" +
                        "Expiry Timeout Counter              : " +
                            expiryTimeoutCounter.incrementAndGet() + "\r\n" +
                        "Global Expiry Timeout Counter       : " +
                            globalExpiryTimeoutCounter.incrementAndGet() + "\r\n");
            }
            try {
                ((Timer)PushInternalContext.getInstance().getAttribute(Timer.class.getName() + "$expiry")).
                    schedule(
                        expiryTimeout = newExpiryTimeout(_isCloudPushID),
                        !_isCloudPushID ? pushIDTimeout : cloudPushIDTimeout);
                return true;
            } catch (final IllegalStateException exception) {
                // timeoutTimer was cancelled or its timer thread terminated.
                return false;
            }
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                Level.FINE,
                "Expiry timeout already scheduled for PushID '" + id + "' (" +
                    "timeout: '" + (!_isCloudPushID ? pushIDTimeout : cloudPushIDTimeout) + "'" +
                ").");
        }
        return false;
    }

    protected TimerTask getExpiryTimeout() {
        return expiryTimeout;
    }

    protected ExpiryTimeout newExpiryTimeout(final boolean isCloudPushID) {
        return new ExpiryTimeout(this, isCloudPushID);
    }

    protected static class ExpiryTimeout
    extends TimerTask {
        private static final Logger LOGGER = Logger.getLogger(ExpiryTimeout.class.getName());

        protected final PushID pushID;
        protected final boolean isCloudPushID;

        protected ExpiryTimeout(final PushID pushID, final boolean isCloudPushID) {
            this.pushID = pushID;
            this.isCloudPushID = isCloudPushID;
        }

        @Override
        public void run() {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Expiry timeout occurred for PushID '" + pushID.getID() + "' (" +
                        "timeout: '" + (!isCloudPushID ? pushID.pushIDTimeout : pushID.cloudPushIDTimeout) + "').");
            }
            try {
                pushID.discard();
                pushID.cancelExpiryTimeout();
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
}
