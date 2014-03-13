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
        setAttribute(PushGroupManager.class.getName(), NoopPushGroupManager.Instance);
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
