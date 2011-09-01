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

if (!window.ice) {
    window.ice = new Object;
}

if (!window.ice.icepush) {
    (function(namespace) {
        window.ice.icepush = true;
        //include functional.js
        //include oo.js
        //include collection.js
        //include string.js
        //include window.js
        //include logger.js
        //include cookie.js
        //include delay.js
        //include element.js
        //include event.js
        //include http.js
        //include configuration.js
        //include command.js
        //include connection.async.js
        //include inter.window.notification.js

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
        var BrowserIDCookieName = 'ice.push.browser';
        var NotifiedPushIDs = 'ice.notified.pushids';

        var handler = window.console && window.console.firebug ? FirebugLogHandler(debug) : WindowLogHandler(debug, window.location.href);
        namespace.windowID = namespace.windowID || substring(Math.random().toString(16), 2, 7);
        namespace.logger = Logger([ 'icepush' ], handler);
        namespace.info = info;
        var pushIdentifiers = [];

        function enlistPushIDsWithBrowser(ids) {
            try {
                var idsCookie = lookupCookie(PushIDs);
                var registeredIDs = split(value(idsCookie), ' ');
                update(idsCookie, join(concatenate(registeredIDs, ids), ' '));
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

            createPushId: function() {
                var id;
                postSynchronously(apiChannel, namespace.push.configuration.createPushIdURI || 'create-push-id.icepush', noop, FormPost, $witch(function (condition) {
                    condition(OK, function(response) {
                        id = contentAsText(response);
                    });
                    condition(ServerInternalError, throwServerError);
                }));
                return id;
            },

            notify: function(group) {
                postAsynchronously(apiChannel, namespace.push.configuration.notifyURI || 'notify.icepush', function(q) {
                    addNameValue(q, 'group', group);
                }, FormPost, $witch(function(condition) {
                    condition(ServerInternalError, throwServerError);
                }));
            },

            addGroupMember: function(group, id) {
                postAsynchronously(apiChannel, namespace.push.configuration.addGroupMemberURI || 'add-group-member.icepush', function(q) {
                    addNameValue(q, 'group', group);
                    addNameValue(q, 'id', id);
                }, FormPost, $witch(function(condition) {
                    condition(ServerInternalError, throwServerError);
                }));
            },

            removeGroupMember: function(group, id) {
                postAsynchronously(apiChannel, namespace.push.configuration.removeGroupMemberURI || 'remove-group-member.icepush', function(q) {
                    addNameValue(q, 'group', group);
                    addNameValue(q, 'id', id);
                }, FormPost, $witch(function(condition) {
                    condition(ServerInternalError, throwServerError);
                }));
            },

            get: function(uri, parameters, responseCallback) {
                getAsynchronously(apiChannel, uri, function(query) {
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
            var publicConfiguration = namespace.push.configuration;
            var configurationElement = document.documentElement;//documentElement is used as a noop config. element
            var configuration = XMLDynamicConfiguration(function() {
                return configurationElement;
            });
            var commandDispatcher = CommandDispatcher();
            var asyncConnection = AsyncConnection(logger, windowID, configuration);

            register(commandDispatcher, 'noop', NoopCommand);
            register(commandDispatcher, 'parsererror', ParsingError);
            register(commandDispatcher, 'macro', Macro(commandDispatcher));
            register(commandDispatcher, 'configuration', function(message) {
                configurationElement = message;
                //update public values
                publicConfiguration.contextPath = attributeAsString(configuration, 'contextPath', publicConfiguration.contextPath);
                publicConfiguration.blockingConnectionURI = attributeAsString(configuration, 'blockingConnectionURI', publicConfiguration.blockingConnectionURI || 'listen.icepush');
                changeHeartbeatInterval(asyncConnection, attributeAsNumber(configuration, 'heartbeatTimeout', 50000));
            });
            register(commandDispatcher, 'browser', function(message) {
                document.cookie = BrowserIDCookieName + '=' + message.getAttribute('id');
            });

            //purge discarded pushIDs from the notification list
            function purgeUnusedPushIDs(ids) {
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

            //choose between localStorage or cookie based inter-window communication
            var notificationBroadcaster = window.localStorage ?
                    LocalStorageNotificationBroadcaster(NotifiedPushIDs, selectWindowNotifications) : CookieBasedNotificationBroadcaster(NotifiedPushIDs, selectWindowNotifications);

            //register command that handles the notified-pushids message
            register(commandDispatcher, 'notified-pushids', function(message) {
                var text = message.firstChild;
                if (text && !blank(text.data)) {
                    var receivedPushIDs = split(text.data, ' ');
                    debug(logger, 'received notifications: ' + receivedPushIDs);
                    notifyWindows(notificationBroadcaster, purgeUnusedPushIDs(asSet(receivedPushIDs)));
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

            onReceive(asyncConnection, function(response) {
                var mimeType = getHeader(response, 'Content-Type');
                if (mimeType && startsWith(mimeType, 'text/xml')) {
                    deserializeAndExecute(commandDispatcher, contentAsDOM(response).documentElement);
                } else {
                    warn(logger, 'unknown content in response - ' + mimeType + ', expected text/xml');
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

            info(logger, 'bridge loaded!');
        }

        onLoad(window, Bridge);

        onKeyPress(document, function(ev) {
            var e = $event(ev);
            if (isEscKey(e)) cancelDefaultAction(e);
        });
    })(window.ice);
}
