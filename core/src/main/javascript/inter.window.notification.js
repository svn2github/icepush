/*
 * Version: MPL 1.1
 *
 * "The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is ICEfaces 1.5 open source software code, released
 * November 5, 2006. The Initial Developer of the Original Code is ICEsoft
 * Technologies Canada, Corp. Portions created by ICEsoft are Copyright (C)
 * 2004-2010 ICEsoft Technologies Canada, Corp. All Rights Reserved.
 *
 * Contributor(s): _____________________.
 *
 */

var notifyWindows = operator();
var disposeBroadcast = operator();

function LocalStorageNotificationBroadcaster(name, callback) {
    if (!window.localStorage.getItem(name)) {
        window.localStorage.setItem(name, '');
    }

    var storageListener = function(e) {
        var newValue = e.newValue || window.localStorage.getItem(name);
        callback(split(newValue, ' '));
    };

    if (window.addEventListener) {
        window.addEventListener('storage', storageListener, false);
    } else {
        document.attachEvent('onstorage', storageListener);//IE8
    }

    return object(function(method) {
        method(notifyWindows, function(self, newValue) {
            //the random number is required to force locaStorage event notification when stored value has not changed
            window.localStorage.setItem(name, join(newValue, ' ') + ' ' + Math.random());
            //notify the current window as well
            callback(newValue);
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
