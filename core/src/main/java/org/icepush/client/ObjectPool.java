/*
 * Copyright 2004-2013 ICEsoft Technologies Canada Corp.
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
 *
 */
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
