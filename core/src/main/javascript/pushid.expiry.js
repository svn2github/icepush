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

var resumePushIDExpiry = operator();
var stopPushIDExpiry = operator();
var PushIDExpiryMonitor;
(function () {
    if (useLocalStorage()) {
        PushIDExpiryMonitor = function(parentLogger) {
            var logger = childLogger(parentLogger, 'pushid-expiry');
            var notificationResponsivness = {};

            var testChannel = "ice.push.liveliness";
            var testLivelinessBroadcaster = LocalStorageNotificationBroadcaster(testChannel, function (verifiedIds) {
                var ids = registeredWindowPushIds();
                var confirmedIds = intersect(verifiedIds, ids);
                if (notEmpty(confirmedIds)) {
                    info(logger, 'send confirmation: ' + ids);
                    notifyWindows(confirmLivelinessBroadcaster, ids);
                }
            });

            var confirmationChannel = "ice.push.confirm";
            var confirmLivelinessBroadcaster = LocalStorageNotificationBroadcaster(confirmationChannel, function (confirmedIDs) {
                console.info('received confirmation: ' + confirmedIDs);
                each(confirmedIDs, function (id) {
                    delete notificationResponsivness[id];
                });
            });

            function requestConfirmLiveliness() {
                var ids = registeredPushIds();
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

                info(logger, 'request confirmation: ' + ids);
                notifyWindows(testLivelinessBroadcaster, ids);

                return notificationResponsivness;
            }

            function removeUnusedPushIDs() {
                var unresponsivePushIds = requestConfirmLiveliness();
                //collect pushIDs that have not confirmed their notification
                var ids = [];
                for (var p in unresponsivePushIds) {
                    if (unresponsivePushIds.hasOwnProperty(p) && unresponsivePushIds[p] > 5) {
                        append(ids, p);
                    }
                }
                //remove unused pushIDs
                if (notEmpty(ids)) {
                    info(logger, 'expirying pushIDs: ' + ids);
                    delistPushIDsWithBrowser(ids);
                }
            }

            var pid = object(function (method) {
                method(stop, noop);
            });

            return object(function (method) {
                method(resumePushIDExpiry, function (self) {
                    info(logger, 'resume monitoring for unused pushIDs');
                    pid = Delay(removeUnusedPushIDs, 10000);
                    run(pid);
                });

                method(stopPushIDExpiry, function (self) {
                    info(logger, 'stopped monitoring for unused pushIDs');
                    stop(pid);
                });
            });
        };
    } else {
        //no-op implementation
        PushIDExpiryMonitor = function () {
            return object(function (method) {
                method(resumePushIDExpiry, noop);
                method(stopPushIDExpiry, noop);
            });
        };
    }
})();

