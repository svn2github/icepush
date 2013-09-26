/*
 * Copyright 2004-2013 ICEsoft Technologies Canada Corp.
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
 *
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

        //constants
        var PushIDs = 'ice.pushids';
        var BrowserIDName = 'ice.push.browser';
        var NotifiedPushIDs = 'ice.notified.pushids';

        var handler = window.console ? ConsoleLogHandler(debug) : WindowLogHandler(debug, window.location.href);
        namespace.windowID = namespace.windowID || substring(Math.random().toString(16), 2, 7);
        namespace.logger = Logger([ 'icepush' ], handler);
        namespace.info = info;
        var pushIdentifiers = [];

        function registeredWindowPushIds() {
            return pushIdentifiers;
        }

        function registeredPushIds() {
            try {
                return split(lookupCookieValue(PushIDs), ' ');
            } catch (e) {
                return [];
            }
        }

        function enlistPushIDsWithBrowser(ids) {
            try {
                var idsCookie = lookupCookie(PushIDs);
                var registeredIDs = split(value(idsCookie), ' ');
                update(idsCookie, join(asSet(concatenate(registeredIDs, ids)), ' '));
            } catch (e) {
                Cookie(PushIDs, join(ids, ' '));
            }
        }

        function delistPushIDsWithBrowser(ids) {
            if (existsCookie(PushIDs)) {
                var idsCookie = lookupCookie(PushIDs);
                var registeredIDs = split(value(idsCookie), ' ');
                update(idsCookie, join(complement(registeredIDs, ids), ' '));
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

        onBeforeUnload(window, function() {
            delistPushIDsWithBrowser(pushIdentifiers);
            pushIdentifiers = [];
        });

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

        var commandDispatcher = CommandDispatcher();
        register(commandDispatcher, 'parsererror', ParsingError);
        register(commandDispatcher, 'macro', Macro(commandDispatcher));

        var browserID = Slot(BrowserIDName);
        register(commandDispatcher, 'browser', function(message) {
            setValue(browserID, message.getAttribute('id'));
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
                    addNameValue(query, BrowserIDName, getValue(browserID));
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

            notify: function(group, options, schedule) {
                var uri = resolveURI(namespace.push.configuration.notifyURI || 'notify.icepush');
                postAsynchronously(apiChannel, uri, function(q) {
                    addNameValue(q, BrowserIDName, getValue(browserID));
                    addNameValue(q, 'group', group);
                    if (schedule) {
                        var duration = schedule.duration ? schedule.duration : 0;
                        if (schedule.delay) {
                            addNameValue(q, 'delay', schedule.delay);
                        } else if (schedule.at) {
                            addNameValue(q, 'at', schedule.at);
                        } else {
                            addNameValue(q, 'delay', 0);
                        }
                        addNameValue(q, 'duration', duration);
                    }
                    if (options) {
                        for (var name in options) {
                            if (options.hasOwnProperty(name)) {
                                addNameValue(q, 'option', name + '=' + options[name]);
                            }
                        }
                    }
                }, FormPost, $witch(function(condition) {
                    condition(ServerInternalError, throwServerError);
                }));
            },

            addGroupMember: function(group, id) {
                var uri = resolveURI(namespace.push.configuration.addGroupMemberURI || 'add-group-member.icepush');
                postAsynchronously(apiChannel, uri, function(q) {
                    addNameValue(q, BrowserIDName, getValue(browserID));
                    addNameValue(q, 'group', group);
                    addNameValue(q, 'id', id);
                }, FormPost, $witch(function(condition) {
                    condition(ServerInternalError, throwServerError);
                }));
            },

            removeGroupMember: function(group, id) {
                var uri = resolveURI(namespace.push.configuration.removeGroupMemberURI || 'remove-group-member.icepush');
                postAsynchronously(apiChannel, uri, function(q) {
                    addNameValue(q, BrowserIDName, getValue(browserID));
                    addNameValue(q, 'group', group);
                    addNameValue(q, 'id', id);
                }, FormPost, $witch(function(condition) {
                    condition(ServerInternalError, throwServerError);
                }));
            },

            get: function(uri, parameters, responseCallback) {
                getAsynchronously(apiChannel, uri, function(query) {
                    addNameValue(query, BrowserIDName, getValue(browserID));
                    parameters(curry(addNameValue, query));
                }, noop, $witch(function(condition) {
                    condition(OK, function(response) {
                        responseCallback(statusCode(response), contentAsText(response), contentAsDOM(response));
                    });
                    condition(ServerInternalError, throwServerError);
                }));
            },

            post: function(uri, parameters, responseCallback) {
                postAsynchronously(apiChannel, uri, function(query) {
                    addNameValue(query, BrowserIDName, getValue(browserID));
                    parameters(curry(addNameValue, query));
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
                blockingConnectionURI: 'listen.icepush'
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
            var pushIdLiveliness = PushIDLiveliness(registeredWindowPushIds);
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
                var registeredIDsCookie = lookupCookie(PushIDs);
                var registeredIDs = split(value(registeredIDsCookie), ' ');
                return intersect(ids, registeredIDs);
            }

            function selectWindowNotifications(ids) {
                try {
                    var windowPushIDs = asArray(intersect(ids, pushIdentifiers));
                    if (notEmpty(windowPushIDs)) {
                        broadcast(notificationListeners, [ windowPushIDs ]);
                        debug(logger, 'picked up notifications for this window: ' + windowPushIDs);
                        return windowPushIDs;
                    }
                } catch (e) {
                    warn(logger, 'failed to listen for updates', e);
                    return [];
                }
            }

            function removeUnusedPushIDs() {
                var unresponsivePushIds = testLiveliness(pushIdLiveliness, registeredPushIds());
                //collect pushIDs that have not confirmed their notification
                var ids = [];
                for (var p in unresponsivePushIds) {
                    if (unresponsivePushIds.hasOwnProperty(p) && unresponsivePushIds[p] > 3) {
                        append(ids, p);
                    }
                }
                //remove unused pushIDs
                if (notEmpty(ids)) {
                    info(logger, 'expirying unused ids: ' + ids);
                    delistPushIDsWithBrowser(ids);
                }
            }

            //choose between localStorage or cookie based inter-window communication
            var notificationBroadcaster = window.localStorage ?
                LocalStorageNotificationBroadcaster(NotifiedPushIDs, selectWindowNotifications) : CookieBasedNotificationBroadcaster(NotifiedPushIDs, selectWindowNotifications);

            //register command that handles the noop message
            register(commandDispatcher, 'noop', removeUnusedPushIDs);
            //register command that handles the notified-pushids message
            register(commandDispatcher, 'notified-pushids', function(message) {
                var text = message.firstChild;
                if (text && !blank(text.data)) {
                    var receivedPushIDs = split(text.data, ' ');
                    debug(logger, 'received notifications: ' + receivedPushIDs);
                    notifyWindows(notificationBroadcaster, purgeNonRegisteredPushIDs(asSet(receivedPushIDs)));
                    removeUnusedPushIDs();
                } else {
                    warn(logger, "No notification was received.");
                }
            });

            function dispose() {
                try {
                    dispose = noop;
                    disposeBroadcast(notificationBroadcaster);
                } finally {
                    shutdown(asyncConnection);
                }
            }

            onUnload(window, dispose);

            onSend(asyncConnection, function(request) {
                if(sequenceNo){
                    //send current sequence number
                    setHeader(request, 'ice.push.sequence', sequenceNo);
                }
                if (browserID) {
                    setHeader(request, 'ice.push.browser', getValue(browserID));
                }
                if (heartbeatTimestamp) {
                    setHeader(request, 'ice.push.heartbeatTimestamp', heartbeatTimestamp);
                }
            });

            onReceive(asyncConnection, function(response) {
                if (isXMLResponse(response)) {
                    deserializeAndExecute(commandDispatcher, contentAsDOM(response).documentElement);
                } else {
                    var mimeType = getHeader(response, 'Content-Type');
                    warn(logger, 'unknown content in response - ' + mimeType + ', expected text/xml');
                }

                //update sequence number incremented by the server
                sequenceNo = Number(getHeader(response, 'ice.push.sequence'));
                if (hasHeader(response, 'ice.push.heartbeatTimestamp')) {
                    heartbeatTimestamp = Number(getHeader(response, 'ice.push.heartbeatTimestamp'));
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
                    onSend(asyncConnection, function(request) {
                        //the callback parameters: function(addHeader) {...; addHeader('A', '123'); ...}
                        callback(function(name, value) {
                            setHeader(request, name, value);
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
            onLoad(window, curry(startConnection, asyncConnection));
        }

        Bridge();

        onKeyPress(document, function(ev) {
            var e = $event(ev);
            if (isEscKey(e)) cancelDefaultAction(e);
        });
    })(window.ice);
}
