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

if (!window.ice) {
    window.ice = new Object;
}

if (!window.ice.icepush) {
    (function(namespace) {
        window.ice.icepush = true;

        eval(ice.importFrom('ice.lib.functional'));
        eval(ice.importFrom('ice.lib.oo'));
        eval(ice.importFrom('ice.lib.collection'));
        eval(ice.importFrom('ice.lib.string'));
        eval(ice.importFrom('ice.lib.delay'));
        eval(ice.importFrom('ice.lib.cookie'));
        eval(ice.importFrom('ice.lib.window'));
        eval(ice.importFrom('ice.lib.event'));
        eval(ice.importFrom('ice.lib.element'));
        eval(ice.importFrom('ice.lib.logger'));
        eval(ice.importFrom('ice.lib.query'));
        eval(ice.importFrom('ice.lib.http'));
        eval(ice.importFrom('ice.lib.configuration'));

        //local storage doesn't work well in Firefox 3.6 and older
        var ffMatch = navigator.userAgent.match(/Firefox\/(\w\.?\w)/);
        var firefoxGreaterThan3point6 = ffMatch ? (Number(ffMatch[1]) > 3.6) : true;
        var ie = window.attachEvent || /Trident.*rv\:11\./.test(navigator.userAgent) || /MSIE/.test(navigator.userAgent);

        function useLocalStorage() {
            var workingLocalStorage = false;
            if (window.localStorage) {
                var key = 'testLocalStorage';
                var value = String(Math.random());
                try {
                    window.localStorage[key] = value;
                    workingLocalStorage = window.localStorage[key] == value;
                } catch (ex) {
                    return false;
                } finally {
                    window.localStorage.removeItem(key);
                }
            }
            return workingLocalStorage && firefoxGreaterThan3point6 && !ie;
        }

        function detectByReference(ref) {
            return function(o) {
                return o == ref;
            };
        }

        function removeCallbackCallback(callbackList, detector) {
            return function removeCallback() {
                var temp = reject(callbackList, detector);
                empty(callbackList);
                each(temp, curry(append, callbackList));
            }
        }

        //include configuration.js
        //include command.js
        //include slot.js
        //include connection.async.js
        //include inter.window.notification.js
        //include pushid.expiry.js

        var notificationListeners = [];
        namespace.onNotification = function(callback) {
            append(notificationListeners, callback);
            return removeCallbackCallback(notificationListeners, detectByReference(callback));
        };

        var receiveListeners = [];
        namespace.onBlockingConnectionReceive = function(callback) {
            append(receiveListeners, callback);
            return removeCallbackCallback(receiveListeners, detectByReference(callback));
        };

        var serverErrorListeners = [];
        namespace.onBlockingConnectionServerError = function(callback) {
            append(serverErrorListeners, callback);
            return removeCallbackCallback(serverErrorListeners, detectByReference(callback));
        };

        var blockingConnectionUnstableListeners = [];
        namespace.onBlockingConnectionUnstable = function(callback) {
            append(blockingConnectionUnstableListeners, callback);
            return removeCallbackCallback(blockingConnectionUnstableListeners, detectByReference(callback));
        };

        var blockingConnectionLostListeners = [];
        namespace.onBlockingConnectionLost = function(callback) {
            append(blockingConnectionLostListeners, callback);
            return removeCallbackCallback(blockingConnectionLostListeners, detectByReference(callback));
        };

        var blockingConnectionReEstablishedListeners = [];
        namespace.onBlockingConnectionReEstablished = function(callback) {
            append(blockingConnectionReEstablishedListeners, callback);
            return removeCallbackCallback(blockingConnectionReEstablishedListeners, detectByReference(callback));
        };

        //constants
        var PushID = 'ice.pushid';
        var PushIDs = 'ice.pushids';
        var BrowserIDName = 'ice.push.browser';
        var WindowID = 'ice.push.window';
        var NotifiedPushIDs = 'ice.notified.pushids';
        var HeartbeatTimestamp = 'ice.push.heartbeatTimestamp';

        var handler = LocalStorageLogHandler(window.console ? ConsoleLogHandler(debug) : WindowLogHandler(debug, window.location.href));
        namespace.windowID = namespace.windowID || substring(Math.random().toString(16), 2, 7);
        namespace.logger = Logger([ 'icepush' ], handler);
        namespace.info = info;
        var pushIdentifiers = [];

        function registeredWindowPushIds() {
            return pushIdentifiers;
        }

        var pushIDsSlot = Slot(PushIDs);
        function registeredPushIds() {
            try {
                return split(getValue(pushIDsSlot), ' ');
            } catch (e) {
                return [];
            }
        }

        function enlistPushIDsWithBrowser(ids) {
            var registeredIDs = split(getValue(pushIDsSlot), ' ');
            //make sure browser ID is set before registering the push ID
            try {
                lookupCookieValue(BrowserIDName)
            } catch (ex) {
                try {
                    var id = ids[0].split(':')[0];
                    Cookie(BrowserIDName, id);
                } catch (ex) {
                    error(namespace.logger, 'Failed to extract browser ID from push ID.');
                }
            }
            setValue(pushIDsSlot, join(asSet(concatenate(registeredIDs, ids)), ' '));
        }

        function delistPushIDsWithBrowser(ids) {
            if (existsSlot(PushIDs)) {
                var registeredIDs = split(getValue(pushIDsSlot), ' ');
                setValue(pushIDsSlot, join(complement(registeredIDs, ids), ' '));
            }
        }

        function enlistPushIDsWithWindow(ids) {
            enlistPushIDsWithBrowser(ids);
            pushIdentifiers = concatenate(pushIdentifiers, ids);
        }

        function delistPushIDsWithWindow(ids) {
            delistPushIDsWithBrowser(ids);
            pushIdentifiers = complement(pushIdentifiers, ids);
        }

        function throwServerError(response) {
            throw 'Server internal error: ' + contentAsText(response);
        }

        function isJSONResponse(response) {
            var mimeType = getHeader(response, 'Content-Type');
            return mimeType && startsWith(mimeType, 'application/json');
        }

        function JSONRequest(request) {
            setHeader(request, 'Content-Type', 'application/json');
        }

        function browserID() {
            try {
                return lookupCookieValue(BrowserIDName);
            } catch (e) {
                //skip sending browser ID when not yet defined
                return null;
            }
        }

        var commandDispatcher = CommandDispatcher();
        register(commandDispatcher, 'parsererror', ParsingError);
        register(commandDispatcher, 'browser', function(message) {
            Cookie(BrowserIDName, message.browser.id);
        });

        var currentNotifications = [];
        var apiChannel = Client(true);

        //public API
        namespace.uriextension = '';
        namespace.push = {
            register: function(pushIds, callback) {
                if ((typeof callback) == 'function') {
                    enlistPushIDsWithWindow(pushIds);
                    namespace.onNotification(function(ids, payload) {
                        currentNotifications = asArray(intersect(ids, pushIds));
                        if (notEmpty(currentNotifications)) {
                            try {
                                callback(currentNotifications, payload);
                            } catch (e) {
                                error(namespace.logger, 'error thrown by push notification callback', e);
                            }
                        }
                    });
                } else {
                    throw 'the callback is not a function';
                }
            },

            deregister: delistPushIDsWithWindow,

            createPushId: function (retries, callback) {
                var uri = '/notify/' + ice.push.configuration.account + '/realms/' + ice.push.configuration.realm + '/push-ids';
                var body = JSON.stringify({
                    'access_token': ice.push.configuration.access_token,
                    'browser': browserID(),
                    'op': 'create'
                });
                postAsynchronously(apiChannel, uri, body, JSONRequest, $witch(function (condition) {
                    condition(OK, function (response) {
                        if (isJSONResponse(response)) {
                            if (retries && retries > 1) {
                                error(namespace.logger, 'failed to set ice.push.browser cookie');
                                return;
                            }
                            deserializeAndExecute(commandDispatcher, response);
                            retries = retries ? retries + 1 : 1;
                            namespace.push.createPushId(retries, callback);
                        } else {
                            var id = contentAsText(response);
                            callback(id);
                        }
                    });
                    condition(ServerInternalError, throwServerError);
                }));
            },

            notify: function(group, options) {
                var uri = '/notify/' + ice.push.configuration.account + '/realms/' + ice.push.configuration.realm + '/push-ids';
                var body = JSON.stringify({
                    'access_token': ice.push.configuration.access_token,
                    'browser': browserID(),
                    'op': 'push',
                    'push-ids': registeredPushIds(),
                    'push_configuration': options
                });
                postAsynchronously(apiChannel, uri, body, JSONRequest, $witch(function(condition) {
                    condition(ServerInternalError, throwServerError);
                }));
            },

            addGroupMember: function (group, id, options) {
                var uri = '/notify/' + ice.push.configuration.account + '/realms/' + ice.push.configuration.realm + '/groups/' + group + '/push-ids/' + id;
                var body = JSON.stringify({
                    'access_token': ice.push.configuration.access_token,
                    'browser': browserID(),
                    'op': 'add',
                    'push_configuration': options
                });
                postAsynchronously(apiChannel, uri, body, JSONRequest, $witch(function (condition) {
                    condition(ServerInternalError, throwServerError);
                }));
            },

            removeGroupMember: function (group, id) {
                var uri = '/notify/' + ice.push.configuration.account + '/realms/' + ice.push.configuration.realm + '/groups/' + group + '/push-ids/' + id;
                deleteAsynchronously(apiChannel, uri, function(query) {
                    parameter(query, "op", "delete");
                }, FormPost, $witch(function (condition) {
                    condition(ServerInternalError, throwServerError);
                }));
            },

            addNotifyBackURI: function (notifyBackURI) {
                var uri = '/notify/' + ice.push.configuration.account + '/realms/' + ice.push.configuration.realm + '/browsers/' + browserID() + '/notify-back-uris/' + notifyBackURI;
                var body = JSON.stringify({
                    'access_token': ice.push.configuration.access_token,
                    'browser': browserID(),
                    'op': 'add'
                });
                postAsynchronously(apiChannel, uri, body, JSONRequest, $witch(function (condition) {
                    condition(OK, function (response) {
                        if (isJSONResponse(response)) {
                            deserializeAndExecute(response);
                            namespace.push.addNotifyBackURI(notifyBackURI);
                        }
                    });
                    condition(ServerInternalError, throwServerError);
                }));
            },

            removeNotifyBackURI: function () {
                var uri = '/notify/' + ice.push.configuration.account + '/realms/' + ice.push.configuration.realm + '/browsers/' + browserID();
                deleteAsynchronously(apiChannel, uri, function(query) {
                    parameter(query, "op", "remove");
                }, JSONRequest, $witch(function (condition) {
                    condition(ServerInternalError, throwServerError);
                }));
            },

            hasNotifyBackURI: function (resultCallback) {
                var uri = '/notify/' + ice.push.configuration.account + '/realms/' + ice.push.configuration.realm + '/browsers/' + browserID();
                getAsynchronously(apiChannel, uri, function(query) {
                    parameter(query, "op", "has");
                }, JSONRequest, $witch(function (condition) {
                    condition(OK, function(response) {
                        try {
                            var result = JSON.parse(contentAsText(response));
                            resultCallback(!!result.notify_back_enabled);
                        } catch (ex) {
                            resultCallback(false);
                        }
                    });
                    condition(ServerInternalError, throwServerError);
                }));
            },

            configuration: {
                account: 'icesoft_technologies',
                realm: 'icesoft.com',
                access_token: '271c401b-3cb9-41b2-96a5-ccb99eee9a0a'
            }
        };

        function Bridge() {
            var windowID = namespace.windowID;
            var logger = childLogger(namespace.logger, windowID);
            var pushIdExpiryMonitor = PushIDExpiryMonitor(logger);
            var asyncConnection = AsyncConnection(logger, windowID);

            //purge discarded pushIDs from the notification list
            function purgeNonRegisteredPushIDs(ids) {
                var registeredIDs = split(getValue(pushIDsSlot), ' ');
                return intersect(ids, registeredIDs);
            }

            function selectWindowNotifications(ids, payload) {
                try {
                    var windowPushIDs = asArray(intersect(ids, pushIdentifiers));
                    if (notEmpty(windowPushIDs)) {
                        broadcast(notificationListeners, [ windowPushIDs, payload ]);
                        if (payload) {
                            debug(logger, "picked up notifications with payload '" + payload + "' for this window: " + windowPushIDs);
                        } else {
                            debug(logger, "picked up notifications for this window: " + windowPushIDs);
                        }
                        return windowPushIDs;
                    } else {
                        return [];
                    }
                } catch (e) {
                    warn(logger, 'failed to listen for updates', e);
                    return [];
                }
            }

            //choose between localStorage or cookie based inter-window communication
            var notificationBroadcaster = useLocalStorage() ?
                LocalStorageNotificationBroadcaster(NotifiedPushIDs, selectWindowNotifications) : CookieBasedNotificationBroadcaster(NotifiedPushIDs, selectWindowNotifications);

            //register command that handles the notifications message
            register(commandDispatcher, 'notifications', function(notifications) {
                for (var i = 0; i < notifications.childNodes.length; i++) {
                    var notification = notifications[i];
                    notifyWindows(notificationBroadcaster, purgeNonRegisteredPushIDs(asSet(notification['push-ids'])), notification['payload']);
                }
            });
            //register command that handles the noop message
            register(commandDispatcher, 'noop', noop);

            register(commandDispatcher, 'configuration', function(configuration) {
                reconfigure(asyncConnection, configuration);
            });

            register(commandDispatcher, 'back-off', function(delay) {
                debug(logger, 'received back-off');
                try {
                    pauseConnection(asyncConnection);
                } finally {
                    runOnce(Delay(function() {
                        resumeConnection(asyncConnection);
                    }, delay));
                }
            });

            function dispose() {
                try {
                    info(logger, 'shutting down bridge...');
                    dispose = noop;
                    disposeBroadcast(notificationBroadcaster);
                } finally {
                    shutdown(asyncConnection);
                }
            }

            onBeforeUnload(window, function() {
                pauseConnection(asyncConnection);
            });
            onUnload(window, dispose);

            onSend(asyncConnection, function(query) {
                if (heartbeatTimestamp) {
                    parameter(query, HeartbeatTimestamp, heartbeatTimestamp);
                }
            });

            onReceive(asyncConnection, function(response) {
                if (isJSONResponse(response)) {
                    var content = contentAsText(response);
                    deserializeAndExecute(commandDispatcher, content);
                    broadcast(receiveListeners, [ content ]);
                } else {
                    var mimeType = getHeader(response, 'Content-Type');
                    warn(logger, 'unknown content in response - ' + mimeType + ', expected text/xml');
                    dispose();
                }
            });

            onServerError(asyncConnection, function(response) {
                try {
                    warn(logger, 'server side error');
                    broadcast(serverErrorListeners, [ statusCode(response), contentAsText(response)]);
                } finally {
                    dispose();
                }
            });

            whenStopped(asyncConnection, function(reason) {
                debug(logger, reason + ' in window [' + windowID + ']');
                stopPushIDExpiry(pushIdExpiryMonitor);
            });

            whenReEstablished(asyncConnection, function(windowID) {
                broadcast(blockingConnectionReEstablishedListeners);
                (windowID == namespace.windowID ? resumePushIDExpiry : stopPushIDExpiry)(pushIdExpiryMonitor);
            });

            whenDown(asyncConnection, function(reconnectAttempts) {
                try {
                    warn(logger, 'connection to server was lost');
                    broadcast(blockingConnectionLostListeners, [ reconnectAttempts ]);
                } finally {
                    dispose();
                }
            });

            whenTrouble(asyncConnection, function() {
                warn(logger, 'connection in trouble');
                broadcast(blockingConnectionUnstableListeners);
            });

            //make some connection functionality public
            namespace.push.connection = {
                startConnection: function() {
                    startConnection(asyncConnection);
                },

                resumeConnection: function() {
                    resumeConnection(asyncConnection);
                },

                pauseConnection: function() {
                    pauseConnection(asyncConnection);
                },

                onSend: function(callback) {
                    onSend(asyncConnection, function(query) {
                        //the callback parameters: function(addParameter) {...; addParameter('A', '123'); ...}
                        callback(function(name, value) {
                            parameter(query, name, value);
                        });
                    });
                },

                onReceive: function(callback) {
                    onReceive(asyncConnection, function(response) {
                        //the callback parameters: function(addHeader, contentAsText, contentAsXML) {...}
                        callback(function(name) {
                            return getHeader(response, name);
                        }, contentAsText(response), contentAsDOM(response));
                    });
                }
            };

            info(logger, 'bridge loaded!');

            //start blocking connection only on document load
            onLoad(window, namespace.push.connection.startConnection);
        }

        Bridge();

        onKeyPress(document, function(ev) {
            var e = $event(ev);
            if (isEscKey(e)) cancelDefaultAction(e);
        });
    })(window.ice);
}
