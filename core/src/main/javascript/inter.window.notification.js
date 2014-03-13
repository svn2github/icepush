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
    if (!window.localStorage.getItem(name)) {
        window.localStorage.setItem(name, '');
    }

    var oldValue = window.localStorage.getItem(name);

    function storageListener() {
        var newValue = window.localStorage.getItem(name);
        try {
            if (oldValue != newValue) {
                callback(split(newValue, ' '));
            }
        } finally {
            oldValue = newValue;
        }
    }

    if (window.addEventListener) {
        window.addEventListener('storage', storageListener, false);
    } else {
        document.attachEvent('onstorage', storageListener);//IE8
    }

    return object(function(method) {
        method(notifyWindows, function(self, newValue) {
            //the random number is required to force locaStorage event notification when stored value has not changed
            window.localStorage.setItem(name, join(newValue, ' ') + ' ' + Math.random());
            //notify the current window as well, when not running in IE
            if (!/MSIE/.test(navigator.userAgent)) {
                callback(newValue);
            }
        });

        method(disposeBroadcast, noop);
    });
}

function CookieBasedNotificationBroadcaster(name, callback) {
    //read/create cookie that contains the notified pushID
    var notifiedPushIDs = lookupCookie(name, function() {
        return Cookie(name, '');
    });

    //monitor & pick updates for this window
    var notificationMonitor = run(Delay(function() {
        try {
            var ids = split(value(notifiedPushIDs), ' ');
            if (notEmpty(ids)) {
                var notifiedIDs = callback(ids);
                update(notifiedPushIDs, join(complement(ids, notifiedIDs), ' '));
            }
        } catch (e) {
            warn(namespace.logger, 'failed to listen for updates', e);
        }
    }, 300));

    return object(function(method) {
        method(notifyWindows, function(self, receivedPushIDs) {
            var ids = split(value(notifiedPushIDs), ' ');
            update(notifiedPushIDs, join(asSet(concatenate(ids, receivedPushIDs)), ' '));
        });

        method(disposeBroadcast, function(self) {
            stop(notificationMonitor);
        });
    });
}
