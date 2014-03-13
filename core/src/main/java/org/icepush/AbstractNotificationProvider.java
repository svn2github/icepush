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

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

public abstract class AbstractNotificationProvider
implements NotificationProvider {
    private static final Logger LOGGER = Logger.getLogger(AbstractNotificationProvider.class.getName());

    private final Set<NotificationListener>
        listenerSet = new CopyOnWriteArraySet<NotificationListener>();

    public void addNotificationProviderListener(final NotificationListener listener) {
        listenerSet.add(listener);
    }

    public void removeNotificationProviderListener(final NotificationListener listener) {
        listenerSet.remove(listener);
    }

    protected void notificationSent(final NotificationEvent event) {
        for (final NotificationListener listener : listenerSet) {
            listener.notificationSent(event);
        }
    }
}
