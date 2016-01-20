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

var notifyWindows = operator();
var disposeBroadcast = operator();

function LocalStorageNotificationBroadcaster(name, callback) {
    var RandomSeparator = ':::';
    var PayloadSeparator = '%%%';

    if (!window.localStorage.getItem(name)) {
        window.localStorage.setItem(name, '');
    }

    function storageListener(e) {
        var newValue = e.newValue;
        if (e.key == name && newValue) {
            var idsAndPayload = split(newValue, RandomSeparator)[0];
            var tuple = split(idsAndPayload, PayloadSeparator);
            var ids = split(tuple[0], ' ');
            var payload = tuple[1];
            callback(ids, payload);
        }
    }

    if (window.addEventListener) {
        window.addEventListener('storage', storageListener, false);
    } else {
        document.attachEvent('onstorage', storageListener);//IE8
    }

    return object(function(method) {
        method(notifyWindows, function(self, ids, payload) {
            var newValue = join(ids, ' ') + PayloadSeparator + payload;
            //the random number is required to force localStorage event notification when stored value has not changed
            window.localStorage.setItem(name, newValue + RandomSeparator + Math.random());
            //notify the current window as well, when not running in IE
            var agent = navigator.userAgent;
            if (!/MSIE/.test(agent) && !/Trident/.test(agent)) {
                callback(ids, payload);
            }
        });

        method(disposeBroadcast, noop);
    });
}

function CookieBasedNotificationBroadcaster(name, callback) {
    var NotificationSeparator = ':::';
    var PayloadSeparator = '%%%';

    //read/create cookie that contains the notified pushID
    var notificationsBucket = lookupCookie(name, function() {
        return Cookie(name, '');
    });

    //monitor & pick updates for this window
    var notificationMonitor = run(Delay(function() {
        try {
            var notifications = split(value(notificationsBucket), NotificationSeparator);
            var remainingNotifications = join(inject(notifications, [], function(result, notification) {
                var tuple = split(notification, PayloadSeparator);
                var ids = split(tuple[0], ' ');
                var payload = tuple[1] || '';
                if (notEmpty(ids)) {
                    var notifiedIDs = callback(ids, payload);
                    var remainingIDs = complement(ids, notifiedIDs);
                    if (notEmpty(remainingIDs)) {
                        append(result, join(notifiedIDs, ' ') + PayloadSeparator + payload);
                    }
                }

                return result;
            }), NotificationSeparator);

            update(notificationsBucket, remainingNotifications);
        } catch (e) {
            warn(namespace.logger, 'failed to listen for updates', e);
        }
    }, 300));

    return object(function(method) {
        method(notifyWindows, function(self, receivedPushIDs, payload) {
            var notifications = asArray(split(value(notificationsBucket), NotificationSeparator));
            var newNotification = join(receivedPushIDs, ' ') + PayloadSeparator + (payload || '');
            append(notifications, newNotification);
            var newNotifications = join(notifications, NotificationSeparator);
            update(notificationsBucket, newNotifications);

            if (size(value(notificationsBucket)) != size(newNotifications)) {
                warn(namespace.logger, 'notifications were dropped because of the cookie size limitation');
            }
        });

        method(disposeBroadcast, function(self) {
            stop(notificationMonitor);
        });
    });
}
