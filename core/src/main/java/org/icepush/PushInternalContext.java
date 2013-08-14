package org.icepush;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PushInternalContext {
    private static final Logger LOGGER = Logger.getLogger(PushInternalContext.class.getName());

    private static final ReentrantLock INSTANCE_LOCK = new ReentrantLock();

    private static PushInternalContext instance;

    private final ConcurrentMap<String, Object> attributeMap = new ConcurrentHashMap<String, Object>();

    private PushInternalContext() {
        // Do nothing.
    }

    public Object getAttribute(final String name) {
        return attributeMap.get(name);
    }

    public static PushInternalContext getInstance() {
        INSTANCE_LOCK.lock();
        try {
            if (instance == null) {
                instance = new PushInternalContext();
            }
            return instance;
        } finally {
            INSTANCE_LOCK.unlock();
        }
    }

    public void removeAttribute(final String name) {
        attributeMap.remove(name);
    }

    public void setAttribute(final String name, final Object object) {
        attributeMap.put(name, object);
    }
}
