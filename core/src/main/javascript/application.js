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
        var ieGreaterThan8 = !window.attachEvent;
        function useLocalStorage() {
            return window.localStorage && firefoxGreaterThan3point6 && ieGreaterThan8;
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
        };

        var receiveListeners = [];
        namespace.onBlockingConnectionReceive = function(callback) {
            append(receiveListeners, callback);
        };

        var serverErrorListeners = [];
        namespace.onBlockingConnectionServerError = function(callback) {
            append(serverErrorListeners, callback);
        };

        var blockingConnectionUnstableListeners = [];
        namespace.onBlockingConnectionUnstable = function(callback) {
            append(blockingConnectionUnstableListeners, callback);
        };

        var blockingConnectionLostListeners = [];
        namespace.onBlockingConnectionLost = function(callback) {
            append(blockingConnectionLostListeners, callback);
        };

        var blockingConnectionReEstablishedListeners = [];
        namespace.onBlockingConnectionReEstablished = function(callback) {
            append(blockingConnectionReEstablishedListeners, callback);
        };

        //constants
        var PushID = 'ice.pushid';
        var PushIDs = 'ice.pushids';
        var BrowserIDName = 'ice.push.browser';
        var WindowID = 'ice.push.window';
        var Account = "ice.push.account";
        var Realm = "ice.push.realm";
        var AccessToken = "ice.push.access_token";
        var NotifiedPushIDs = 'ice.notified.pushids';
        var HeartbeatTimestamp = 'ice.push.heartbeatTimestamp';
        var SequenceNumber = 'ice.push.sequence';

        var handler = window.console ? ConsoleLogHandler(debug) : WindowLogHandler(debug, window.location.href);
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
            if (!lookupCookieValue(BrowserIDName)) {
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

        function resolveURI(path) {
            ///just use the path if it is an absolute URL
            if (contains(path, "http://") || contains(path, "https://")) {
                return path;
            }

            var contextPath = namespace.push.configuration.contextPath;

            if (startsWith(path, contextPath)) {
                return path;
            }

            if (contextPath) {
                if (endsWith(contextPath, '/')) {
                    contextPath = substring(contextPath, 0, size(contextPath) - 1);
                }
            } else {
                var pathName = window.location.pathname;
                try {
                    var i = lastIndexOf(pathName, '/');
                    contextPath = substring(pathName, 0, i);
                } catch (e) {
                    contextPath = pathName;
                }
            }

            return contextPath + '/' + path;
        }

        function isXMLResponse(response) {
            var mimeType = getHeader(response, 'Content-Type');
            return mimeType && startsWith(mimeType, 'text/xml');
        }

        function parameter(query, name, value) {
            var prefix = namespace.push.configuration.parameterPrefix;
            if (prefix) {
                name = prefix + name;
            }
            addNameValue(query, name, value);
        }

        var commandDispatcher = CommandDispatcher();
        register(commandDispatcher, 'parsererror', ParsingError);
        register(commandDispatcher, 'macro', Macro(commandDispatcher));

        register(commandDispatcher, 'browser', function(message) {
            Cookie(BrowserIDName, message.getAttribute('id'));
        });

        var currentNotifications = [];
        var apiChannel = Client(true);
        //public API
        namespace.uriextension = '';
        namespace.push = {
            register: function(pushIds, callback) {
                if ((typeof callback) == 'function') {
                    enlistPushIDsWithWindow(pushIds);
                    namespace.onNotification(function(ids) {
                        currentNotifications = asArray(intersect(ids, pushIds));
                        if (notEmpty(currentNotifications)) {
                            try {
                                callback(currentNotifications);
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

            getCurrentNotifications: function() {
                return currentNotifications;
            },

            createPushId: function(retries) {
                var id;
                var uri = resolveURI(namespace.push.configuration.createPushIdURI || 'create-push-id.icepush');
                postSynchronously(apiChannel, uri, function (query) {
                    parameter(query, BrowserIDName, lookupCookieValue(BrowserIDName));
                    parameter(query, Account, ice.push.configuration.account);
                    parameter(query, Realm, ice.push.configuration.realm);
                    parameter(query, AccessToken, ice.push.configuration.access_token);
                }, FormPost, $witch(function (condition) {
                    condition(OK, function(response) {
                        if (isXMLResponse(response)) {
                            if (retries && retries > 1) {
                                error(namespace.logger, 'failed to set ice.push.browser cookie');
                                return;
                            }
                            deserializeAndExecute(commandDispatcher, contentAsDOM(response).documentElement);
                            retries = retries ? retries + 1 : 1;
                            id = namespace.push.createPushId(retries);
                        } else {
                            id = contentAsText(response);
                        }
                    });
                    condition(ServerInternalError, throwServerError);
                }));
                return id;
            },

            notify: function(group, options) {
                var uri = resolveURI(namespace.push.configuration.notifyURI || 'notify.icepush');
                postAsynchronously(apiChannel, uri, function(q) {
                    parameter(q, BrowserIDName, lookupCookieValue(BrowserIDName));
                    parameter(q, Account, ice.push.configuration.account);
                    parameter(q, Realm, ice.push.configuration.realm);
                    parameter(q, AccessToken, ice.push.configuration.access_token);
                    parameter(q, 'group', group);
                    if (options) {
                        //provide default values if missing
                        if (!options.duration) {
                            options.duration = 0;
                        }
                        if (!options.delay) {
                            options.delay = 0;
                        }
                        for (var name in options) {
                            if (options.hasOwnProperty(name)) {
                                var value = options[name];
                                if (name == 'delay') {
                                    parameter(q, 'delay', value);
                                } else if (name == 'at') {
                                    parameter(q, 'at', value);
                                } else if (name == 'duration') {
                                    parameter(q, 'duration', value);
                                } else {
                                    parameter(q, 'option', name + '=' + value);
                                }
                            }
                        }
                    }
                }, FormPost, $witch(function(condition) {
                    condition(ServerInternalError, throwServerError);
                }));
            },

            addGroupMember: function(group, id, options) {
                var uri = resolveURI(namespace.push.configuration.addGroupMemberURI || 'add-group-member.icepush');
                postAsynchronously(apiChannel, uri, function(q) {
                    parameter(q, BrowserIDName, lookupCookieValue(BrowserIDName));
                    parameter(q, Account, ice.push.configuration.account);
                    parameter(q, Realm, ice.push.configuration.realm);
                    parameter(q, AccessToken, ice.push.configuration.access_token);
                    parameter(q, 'group', group);
                    parameter(q, 'id', id);
                    if (options) {
                        for (var name in options) {
                            if (options.hasOwnProperty(name)) {
                                parameter(q, name, options[name]);
                            }
                        }
                    }
                }, FormPost, $witch(function(condition) {
                    condition(ServerInternalError, throwServerError);
                }));
            },

            removeGroupMember: function(group, id) {
                var uri = resolveURI(namespace.push.configuration.removeGroupMemberURI || 'remove-group-member.icepush');
                postAsynchronously(apiChannel, uri, function(q) {
                    parameter(q, BrowserIDName, lookupCookieValue(BrowserIDName));
                    parameter(q, Account, ice.push.configuration.account);
                    parameter(q, Realm, ice.push.configuration.realm);
                    parameter(q, AccessToken, ice.push.configuration.access_token);
                    parameter(q, 'group', group);
                    parameter(q, 'id', id);
                }, FormPost, $witch(function(condition) {
                    condition(ServerInternalError, throwServerError);
                }));
            },

            get: function(uri, parameters, responseCallback) {
                getAsynchronously(apiChannel, uri, function(query) {
                    parameter(query, BrowserIDName, lookupCookieValue(BrowserIDName));
                    parameter(query, Account, ice.push.configuration.account);
                    parameter(query, Realm, ice.push.configuration.realm);
                    parameter(query, AccessToken, ice.push.configuration.access_token);
                    parameters(curry(parameter, query));
                }, noop, $witch(function(condition) {
                    condition(OK, function(response) {
                        responseCallback(statusCode(response), contentAsText(response), contentAsDOM(response));
                    });
                    condition(ServerInternalError, throwServerError);
                }));
            },

            post: function(uri, parameters, responseCallback) {
                postAsynchronously(apiChannel, uri, function(query) {
                    parameter(query, BrowserIDName, lookupCookieValue(BrowserIDName));
                    parameter(query, Account, ice.push.configuration.account);
                    parameter(query, Realm, ice.push.configuration.realm);
                    parameter(query, AccessToken, ice.push.configuration.access_token);
                    parameters(curry(parameter, query));
                }, FormPost, $witch(function(condition) {
                    condition(OK, function(response) {
                        responseCallback(statusCode(response), contentAsText(response), contentAsDOM(response));
                    });
                    condition(ServerInternalError, throwServerError);
                }));
            },

            //todo: move this utility function into the JSP integration project
            searchAndEvaluateScripts: function(element) {
                each(element.getElementsByTagName('script'), function(script) {
                    var newScript = document.createElement('script');
                    newScript.setAttribute('type', 'text/javascript');

                    if (script.src) {
                        newScript.src = script.src;
                    } else {
                        newScript.text = script.text;
                    }

                    element.appendChild(newScript);
                });
            },

            configuration: {
                contextPath: '.',
                blockingConnectionURI: 'listen.icepush',
                account: '',
                realm: '',
                access_token: ''
            }
        };

        function Bridge() {
            var windowID = namespace.windowID;
            var logger = childLogger(namespace.logger, windowID);
            var sequenceNo = 0;
            var heartbeatTimestamp;

            var publicConfiguration = namespace.push.configuration;
            var configurationElement = document.documentElement;//documentElement is used as a noop config. element
            var configuration = XMLDynamicConfiguration(function() {
                return configurationElement;
            });
            var pushIdExpiryMonitor = PushIDExpiryMonitor(logger);
            var asyncConnection = AsyncConnection(logger, windowID, configuration);

            register(commandDispatcher, 'configuration', function(message) {
                configurationElement = message;
                //update public values
                publicConfiguration.contextPath = attributeAsString(configuration, 'contextPath', publicConfiguration.contextPath);
                publicConfiguration.blockingConnectionURI = attributeAsString(configuration, 'blockingConnectionURI', publicConfiguration.blockingConnectionURI || 'listen.icepush');
                changeHeartbeatInterval(asyncConnection, attributeAsNumber(configuration, 'heartbeatTimeout', 15000));
            });
            register(commandDispatcher, 'back-off', function(message) {
                debug(logger, 'received back-off');
                var delay = asNumber(message.getAttribute('delay'));
                try {
                    pauseConnection(asyncConnection);
                } finally {
                    runOnce(Delay(function() {
                        resumeConnection(asyncConnection);
                    }, delay));
                }
            });


            //purge discarded pushIDs from the notification list
            function purgeNonRegisteredPushIDs(ids) {
                var registeredIDs = split(getValue(pushIDsSlot), ' ');
                return intersect(ids, registeredIDs);
            }

            function selectWindowNotifications(ids) {
                try {
                    var windowPushIDs = asArray(intersect(ids, pushIdentifiers));
                    if (notEmpty(windowPushIDs)) {
                        broadcast(notificationListeners, [ windowPushIDs ]);
                        debug(logger, 'picked up notifications for this window: ' + windowPushIDs);
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

            //register command that handles the noop message
            register(commandDispatcher, 'noop', noop);
            //register command that handles the notified-pushids message
            register(commandDispatcher, 'notified-pushids', function(message) {
                var text = message.firstChild;
                if (text && !blank(text.data)) {
                    var receivedPushIDs = split(text.data, ' ');
                    debug(logger, 'received notifications: ' + receivedPushIDs);
                    notifyWindows(notificationBroadcaster, purgeNonRegisteredPushIDs(asSet(receivedPushIDs)));
                } else {
                    warn(logger, "No notification was received.");
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
                if(sequenceNo){
                    //send current sequence number
                    parameter(query, SequenceNumber, sequenceNo);
                }
                if (heartbeatTimestamp) {
                    parameter(query, HeartbeatTimestamp, heartbeatTimestamp);
                }
            });

            onReceive(asyncConnection, function(response) {
                if (isXMLResponse(response)) {
                    var message = contentAsDOM(response).documentElement;
                    deserializeAndExecute(commandDispatcher, message);
                    broadcast(receiveListeners, [ message ]);
                } else {
                    var mimeType = getHeader(response, 'Content-Type');
                    warn(logger, 'unknown content in response - ' + mimeType + ', expected text/xml');
                    dispose();
                }

                //update sequence number incremented by the server
                sequenceNo = Number(getHeader(response, SequenceNumber));
                if (hasHeader(response, HeartbeatTimestamp)) {
                    heartbeatTimestamp = Number(getHeader(response, HeartbeatTimestamp));
                }
            });

            onServerError(asyncConnection, function(response) {
                try {
                    warn(logger, 'server side error');
                    broadcast(serverErrorListeners, [ statusCode(response), contentAsText(response), contentAsDOM(response) ]);
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
                    sequenceNo++;
                    resumeConnection(asyncConnection);
                },

                pauseConnection: function() {
                    pauseConnection(asyncConnection);
                },

                changeHeartbeatInterval: function(interval) {
                    changeHeartbeatInterval(asyncConnection, interval);
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
                },

                controlRequest: function(addParameter, addHeader, responseCallback) {
                    controlRequest(asyncConnection, addParameter, addHeader, responseCallback);
                }
            };

            //make public park push ID feature
            var deviceURI;
            namespace.push.parkInactivePushIds = function(url) {
                deviceURI = url;
                namespace.push.connection.pauseConnection();
                namespace.push.connection.resumeConnection();
            };

            namespace.push.connection.onSend(function(header) {
                if (deviceURI) {
                    header('ice.parkids', 'true');
                    header('ice.notifyBack', deviceURI)
                }
            });

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
