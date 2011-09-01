package org.icepush.util;

import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ThreadFactory
implements java.util.concurrent.ThreadFactory {
    private static final Logger LOGGER = Logger.getLogger(ThreadFactory.class.getName());

    private final ReentrantLock lock = new ReentrantLock();

    private int counter = 0;
    private boolean daemon = true;
    private String prefix = "Thread";

    public ThreadFactory() {
        // Do nothing.
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean isDaemon() {
        return daemon;
    }

    public Thread newThread(final Runnable runnable) {
        Thread _thread;
        lock.lock();
        try {
            _thread = new Thread(runnable, getPrefix() + " [" + ++counter + "]");
        } finally {
            lock.unlock();
        }
        _thread.setDaemon(isDaemon());
        try {
            /*
             * We attempt to set the context class loader because some J2EE containers don't seem to set this properly,
             * which leads to important classes not being found. However, other J2EE containers see this as a security
             * violation.
             */
            _thread.setContextClassLoader(runnable.getClass().getClassLoader());
        } catch (SecurityException exception) {
            /*
             * If the current security policy does not allow this, we have to hope that the appropriate class loader
             * settings were transferred to this new thread.
             */
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Setting the context class loader is not permitted.", exception);
            }
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "New thread: " + _thread.getName());
        }
        return _thread;
    }

    public void setDaemon(final boolean daemon) {
        this.daemon = daemon;
    }

    public void setPrefix(final String prefix) {
        this.prefix = prefix;
    }
}
