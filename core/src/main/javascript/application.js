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
            return function (o) {
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

        function CREATED(response) {
            return statusCode(response) == 201;
        }

        function NOCONTENT(response) {
            return statusCode(response) == 204;
        }

        function NOTFOUND(response) {
            return statusCode(response) == 404;
        }

        //include configuration.js
        //include command.js
        //include slot.js
        //include connection.async.js
        //include inter.window.notification.js
        //include pushid.expiry.js

        var notificationListeners = [];
        namespace.onNotification = function (callback) {
            append(notificationListeners, callback);
            return removeCallbackCallback(notificationListeners, detectByReference(callback));
        };

        var receiveListeners = [];
        namespace.onBlockingConnectionReceive = function (callback) {
            append(receiveListeners, callback);
            return removeCallbackCallback(receiveListeners, detectByReference(callback));
        };

        var serverErrorListeners = [];
        namespace.onBlockingConnectionServerError = function (callback) {
            append(serverErrorListeners, callback);
            return removeCallbackCallback(serverErrorListeners, detectByReference(callback));
        };

        var blockingConnectionUnstableListeners = [];
        namespace.onBlockingConnectionUnstable = function (callback) {
            append(blockingConnectionUnstableListeners, callback);
            return removeCallbackCallback(blockingConnectionUnstableListeners, detectByReference(callback));
        };

        var blockingConnectionLostListeners = [];
        namespace.onBlockingConnectionLost = function (callback) {
            append(blockingConnectionLostListeners, callback);
            return removeCallbackCallback(blockingConnectionLostListeners, detectByReference(callback));
        };

        var blockingConnectionReEstablishedListeners = [];
        namespace.onBlockingConnectionReEstablished = function (callback) {
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
        namespace.logger = Logger(['icepush'], handler);
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
        register(commandDispatcher, 'browser', function (message) {
            Cookie(BrowserIDName, message.browser.id);
        });

        //public API
        namespace.setupPush = function(configuration) {
            Bridge(configuration);

            onKeyPress(document, function(ev) {
                var e = $event(ev);
                if (isEscKey(e)) cancelDefaultAction(e);
            });

            var apiChannel = Client(true);
            return {
                register: function (pushIds, callback) {
                    if ((typeof callback) == 'function') {
                        enlistPushIDsWithWindow(pushIds);
                        namespace.onNotification(function (ids, payload) {
                            var currentNotifications = asArray(intersect(ids, pushIds));
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

                createPushId: function createPushId(retries, callback) {
                    var uri = configuration.uri + configuration.account + '/realms/' + configuration.realm + '/push-ids?access_token=' + encodeURIComponent(configuration.access_token);
                    var body = JSON.stringify({
                        'access_token': configuration.access_token,
                        'browser':  {
                            id: browserID()
                        },
                        'op': 'create'
                    });
                    postAsynchronously(apiChannel, uri, body, JSONRequest, $witch(function (condition) {
                        condition(CREATED, function (response) {
                            if (isJSONResponse(response)) {
                                var content = contentAsText(response);
                                var result = JSON.parse(content)
                                callback(result.push_id.id);
                            } else {
                                if (retries && retries > 1) {
                                    error(namespace.logger, 'failed to set ice.push.browser cookie');
                                    return;
                                }
                                deserializeAndExecute(commandDispatcher, response);
                                retries = retries ? retries + 1 : 1;
                                createPushId(retries, callback);
                            }
                        });
                        condition(ServerInternalError, throwServerError);
                    }));
                },

                notify: function (group, options) {
                    var uri = configuration.uri + configuration.account + '/realms/' + configuration.realm + '/groups/' + group + '?access_token=' + encodeURIComponent(configuration.access_token) + '&op=push';;
                    var body = JSON.stringify({
                        'access_token': configuration.access_token,
                        'browser': {
                            'id': browserID()
                        },
                        'op': 'push'//,
                        //'push_configuration': options
                    });
                    postAsynchronously(apiChannel, uri, body, JSONRequest, $witch(function (condition) {
                        condition(ServerInternalError, throwServerError);
                    }));
                },

                addGroupMember: function (group, id, option) {
                    var uri = configuration.uri + configuration.account + '/realms/' + configuration.realm + '/groups/' + group + '/push-ids/' + id + '?access_token=' + encodeURIComponent(configuration.access_token) + '&op=add';
                    var body = JSON.stringify({
                        'access_token': configuration.access_token,
                        'browser': {
                            "id": browserID()
                        },
                        'op': 'add',
                        'push_configuration': {
                            'cloud_push': !!option
                        }
                    });
                    postAsynchronously(apiChannel, uri, body, JSONRequest, $witch(function (condition) {
                        condition(ServerInternalError, throwServerError);
                    }));
                },

                removeGroupMember: function (group, id) {
                    var uri = configuration.uri + configuration.account + '/realms/' + configuration.realm + '/groups/' + group + '/push-ids/' + id;
                    deleteAsynchronously(apiChannel, uri, function (query) {
                        addNameValue(query, "access_token", configuration.access_token);
                        addNameValue(query, "op", "delete");
                    }, JSONRequest, $witch(function (condition) {
                        condition(ServerInternalError, throwServerError);
                    }));
                },

                addNotifyBackURI: function (notifyBackURI) {
                    var uri = configuration.uri + configuration.account + '/realms/' + configuration.realm + '/browsers/' + browserID() + '/notify-back-uris/' + notifyBackURI + '?access_token=' + encodeURIComponent(configuration.access_token) + '&op=add';
                    var body = JSON.stringify({
                        'access_token': configuration.access_token,
                        'browser': {
                            'id': browserID()
                        },
                        'op': 'add'
                    });
                    postAsynchronously(apiChannel, uri, body, JSONRequest, $witch(function (condition) {
                        condition(ServerInternalError, throwServerError);
                    }));
                },

                removeNotifyBackURI: function () {
                    var uri = configuration.uri + configuration.account + '/realms/' + configuration.realm + '/browsers/' + browserID() + '/notify-back-uris';
                    deleteAsynchronously(apiChannel, uri, function (query) {
                        addNameValue(query, "access_token", configuration.access_token);
                        addNameValue(query, "op", "remove");
                    }, JSONRequest, $witch(function (condition) {
                        condition(ServerInternalError, throwServerError);
                    }));
                },

                hasNotifyBackURI: function (resultCallback) {
                    var uri = configuration.uri + configuration.account + '/realms/' + configuration.realm + '/browsers/' + browserID() + '/notify-back-uris';
                    getAsynchronously(apiChannel, uri, function (query) {
                        addNameValue(query, "access_token", configuration.access_token);
                        addNameValue(query, "op", "has");
                    }, JSONRequest, $witch(function (condition) {
                        condition(NOCONTENT, function (response) {
                            resultCallback(true);
                        });
                        condition(NOTFOUND, function (response) {
                            resultCallback(false);
                        });
                        condition(ServerInternalError, throwServerError);
                    }));
                }
            }
        };

        function Bridge(configuration) {
            var windowID = namespace.windowID;
            var logger = childLogger(namespace.logger, windowID);
            var pushIdExpiryMonitor = PushIDExpiryMonitor(logger);
            var asyncConnection = AsyncConnection(logger, windowID, configuration);

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

            register(commandDispatcher, 'notifications', function(notifications) {
                each(notifications, function(notification) {
                    var ids = collect(notification['push-ids'], function(i) {
                        return i.id;
                    });
                    notifyWindows(notificationBroadcaster, purgeNonRegisteredPushIDs(asSet(ids)), notification['payload']);
                });
            });
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

            info(logger, 'bridge loaded!');

            //start blocking connection only on document load
            onLoad(window, function(){
                startConnection(asyncConnection);
            });
        }
    })(window.ice);
}
