package org.icepush.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class ObjectPool<O> {
    private static final Logger LOGGER = Logger.getLogger(ObjectPool.class.getName());

    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition available = lock.newCondition();
    private final List<O> inList = new ArrayList<O>();
    private final List<O> outList = new ArrayList<O>();

    private final int maxSize;

    protected ObjectPool(final int maxSize) {
        // todo: Remove this logging.
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.log(Level.INFO, "ObjectPool :: maxSize: '" + maxSize + "'");
        }
        this.maxSize = maxSize;
    }

    public O borrowObject() {
        lock.lock();
        try {
            while (true) {
                if (!inList.isEmpty()) {
                    O _object = inList.remove(0);
                    outList.add(_object);
                    return _object;
                } else if (outList.size() != maxSize) {
                    O _object = newObject();
                    outList.add(_object);
                    return _object;
                } else {
                    available.awaitUninterruptibly();
                }
            }
        } finally {
            // todo: Remove this logging.
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, "ObjectPool :: in: '" + inList.size() + "', out: '" + outList.size() + "'");
            }
            lock.unlock();
        }
    }

    public void invalidateObject(final O object) {
        lock.lock();
        try {
            if (outList.contains(object)) {
                outList.remove(object);
                if (outList.size() == maxSize - 1) {
                    available.signal();
                }
            }
        } finally {
            // todo: Remove this logging.
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, "ObjectPool :: in: '" + inList.size() + "', out: '" + outList.size() + "'");
            }
            lock.unlock();
        }
    }

    public void returnObject(final O object) {
        lock.lock();
        try {
            if (outList.contains(object)) {
                outList.remove(object);
                inList.add(object);
                if (inList.size() == 1) {
                    available.signal();
                }
            }
        } finally {
            // todo: Remove this logging.
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, "ObjectPool :: in: '" + inList.size() + "', out: '" + outList.size() + "'");
            }
            lock.unlock();
        }
    }

    public void shutdown() {
        lock.lock();
        try {
            outList.clear();
            inList.clear();
        } finally {
            lock.unlock();
        }
    }
    
    protected abstract O newObject();
}
