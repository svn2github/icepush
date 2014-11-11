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

public interface NotificationProvider {

    void addNotificationProviderListener(NotificationListener listener);

    NotificationEventFactory getNotificationEventFactory();

    void send(String browserID, String groupName, PushNotification notification);

    void setNotificationEventFactory(NotificationEventFactory notificationEventFactory);

    void registerWith(OutOfBandNotifier outOfBandNotifier);

    void removeNotificationProviderListener(NotificationListener listener);
}
