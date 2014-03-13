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

var testLiveliness = operator();
var PushIDLiveliness;
(function () {
    if (useLocalStorage()) {
        PushIDLiveliness = function LocalStoragePushIDLiveliness(pushIdentifiers) {
            var notificationResponsivness = {};

            var testChannel = "ice.push.liveliness";
            var testLivelinessBroadcaster = LocalStorageNotificationBroadcaster(testChannel, function () {
                notifyWindows(confirmLivelinessBroadcaster, pushIdentifiers());
            });

            var confirmationChannel = "ice.push.confirm";
            var confirmLivelinessBroadcaster = LocalStorageNotificationBroadcaster(confirmationChannel, function (confirmedIDs) {
                each(confirmedIDs, function (id) {
                    var count = notificationResponsivness[id];
                    if (count) {
                        notificationResponsivness[id] = count - 1;
                    } else {
                        delete notificationResponsivness[id];
                    }
                });
            });

            return object(function (method) {
                method(testLiveliness, function (self, ids) {
                    //cleanup IDs where not interested in anymore
                    var discardUnresponsiveIds = [];
                    for (var id in notificationResponsivness) {
                        if (notificationResponsivness.hasOwnProperty(id)) {
                            if (not(contains(ids, id))) {
                                append(discardUnresponsiveIds, id);
                            }
                        }
                    }
                    each(discardUnresponsiveIds, function (id) {
                        delete notificationResponsivness[id];
                    });

                    each(ids, function (id) {
                        var count = notificationResponsivness[id];
                        if (count) {
                            notificationResponsivness[id] = count + 1;
                        } else {
                            notificationResponsivness[id] = 1;
                        }
                    });

                    notifyWindows(testLivelinessBroadcaster, ids);

                    return notificationResponsivness;
                });
            });
        }
    } else {
        //no-op implementation
        PushIDLiveliness = function NoOpPushIDLiveliness() {
            return object(function (method) {
                method(testLiveliness, function (self, ids) {
                    return {};
                });
            });
        }
    }
})();

