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

var send = operator();
var onSend = operator();
var onReceive = operator();
var onServerError = operator();
var whenDown = operator();
var whenTrouble = operator();
var whenStopped = operator();
var whenReEstablished = operator();
var startConnection = operator();
var resumeConnection = operator();
var pauseConnection = operator();
var controlRequest = operator();
var reconfigure = operator();
var changeHeartbeatInterval = operator();
var shutdown = operator();
var AsyncConnection;

(function() {
    var HeartbeatInterval = 'ice.push.heartbeat';
    var SequenceNumber = 'ice.push.sequence';
    var ConnectionRunning = 'ice.connection.running';
    var ConnectionLease = 'ice.connection.lease';
    var ConnectionContextPath = 'ice.connection.contextpath';
    var AcquiredMarker = ':acquired';
    var NetworkDelay = 5000;//5s of delay, possibly introduced by network

    //build up retry actions
    function timedRetryAbort(retryAction, abortAction, timeouts) {
        var index = 0;
        var errorActions = inject(timeouts, [abortAction], function(actions, interval) {
            return insert(actions, curry(runOnce, Delay(retryAction, interval)));
        });
        return function() {
            if (index < errorActions.length) {
                apply(errorActions[index], arguments);
                index++;
            }
        };
    }

    AsyncConnection = function(logger, windowID, configuration) {
        var logger = childLogger(logger, 'async-connection');
        var channel = Client(false);
        var onSendListeners = [];
        var onReceiveListeners = [];
        var onServerErrorListeners = [];
        var connectionDownListeners = [];
        var connectionTroubleListeners = [];
        var connectionStoppedListeners = [];
        var connectionReEstablished = [];
        var sequenceNo = Slot(SequenceNumber);

        var listener = object(function(method) {
            method(close, noop);
            method(abort, noop);
        });

        //clear connectionDownListeners to avoid bogus connection lost messages
        onBeforeUnload(window, function() {
            connectionDownListeners = [];
        });
        var lastSentPushIds = registeredPushIds();

        function contextPath() {
            return namespace.push.configuration.contextPath;
        }

        function askForConfiguration(query) {
            //request configuration once, but only after ice.push.browser cookie is set
            try {
                lookupCookieValue(BrowserIDName);
                parameter(query, 'ice.sendConfiguration', '');
                askForConfiguration = noop;
            } catch (ex) {
                //ice.push.browser cookie not set yet
            }
        }

        function requestForBlockingResponse() {
            try {
                debug(logger, "closing previous connection...");
                close(listener);
                setValue(contextPathSlot, contextPath());

                lastSentPushIds = registeredPushIds();
                if (isEmpty(lastSentPushIds)) {
                    stopTimeoutBombs();
                    broadcast(connectionStoppedListeners, ['connection stopped, no pushIDs registered']);
                } else {
                    debug(logger, 'connect...');
                    var uri = resolveURI(namespace.push.configuration.blockingConnectionURI);
                    listener = postAsynchronously(channel, uri, function(q) {
                        parameter(q, BrowserIDName, lookupCookieValue(BrowserIDName));
                        parameter(q, WindowID, namespace.windowID);
                        parameter(q, Account, ice.push.configuration.account);
                        parameter(q, Realm, ice.push.configuration.realm);
                        parameter(q, AccessToken, ice.push.configuration.access_token);
                        parameter(q, HeartbeatInterval, heartbeatTimeout - NetworkDelay);
                        parameter(q, SequenceNumber, getValue(sequenceNo));
                        each(lastSentPushIds, curry(parameter, q, PushID));
                        broadcast(onSendListeners, [q]);
                        askForConfiguration(q);
                    }, FormPost, $witch(function (condition) {
                        condition(OK, function(response) {
                            var sequenceNoValue = Number(getHeader(response, SequenceNumber));
                            if (sequenceNoValue) {
                                //update sequence number incremented by the server
                                setValue(sequenceNo, sequenceNoValue);
                            }
                            var reconnect = getHeader(response, 'X-Connection') != 'close';
                            var nonEmptyResponse = notEmpty(contentAsText(response));

                            if (reconnect) {
                                if (nonEmptyResponse) {
                                    broadcast(onReceiveListeners, [response]);
                                    resetEmptyResponseRetries();
                                } else {
                                    warn(logger, 'empty response received');
                                    decrementEmptyResponseRetries();
                                }

                                if (anyEmptyResponseRetriesLeft()) {
                                    resetTimeoutBomb();
                                    connect();
                                } else {
                                    info(logger, 'blocking connection stopped, too many empty responses received...');
                                }
                            } else {
                                info(logger, 'blocking connection stopped at server\'s request...');
                                var reason = getHeader(response, 'X-Connection-reason');
                                if (reason) {
                                    info(logger, reason);
                                }
                                //avoid to reconnect
                                stopTimeoutBombs();
                                broadcast(connectionStoppedListeners, ['connection stopped by server']);
                            }
                        });
                        condition(ServerInternalError, retryOnServerError);
                    }));
                }
            } catch (e) {
                error(logger, 'failed to re-initiate blocking connection', e);
            }
        }

        var connect = requestForBlockingResponse;

        //build callbacks only after 'connection' function was defined
        var heartbeatTimeout;
        var networkErrorRetryTimeouts;
        function setupNetworkErrorRetries() {
            heartbeatTimeout = attributeAsNumber(configuration, 'heartbeatTimeout', 15000);
            networkErrorRetryTimeouts = collect(split(attributeAsString(configuration, 'networkErrorRetryTimeouts', '1 1 1 2 2 3'), ' '), Number);
        }
        setupNetworkErrorRetries();

        var serverErrorRetryTimeouts;
        var retryOnServerError;
        function setupServerErrorRetries() {
            serverErrorRetryTimeouts = collect(split(attributeAsString(configuration, 'serverErrorRetryTimeouts', '1000 2000 4000'), ' '), Number);
            retryOnServerError = timedRetryAbort(connect, broadcaster(onServerErrorListeners), serverErrorRetryTimeouts);
        }
        setupServerErrorRetries();

        //count the number of consecutive empty responses
        var emptyResponseRetries;
        function resetEmptyResponseRetries() {
            emptyResponseRetries = attributeAsNumber(configuration, 'emptyResponseRetries', 3);
        }
        function decrementEmptyResponseRetries() {
            --emptyResponseRetries;
        }
        function anyEmptyResponseRetriesLeft() {
            return emptyResponseRetries > 0;
        }
        resetEmptyResponseRetries();


        var initialRetryIndex = function () {
            return 0;
        };
        var pendingRetryIndex = initialRetryIndex;
        var stopTimeoutBombs = noop;

        function chainTimeoutBombs(timeoutAction, abortAction, intervals, remainingBombsIndex) {
            var index = remainingBombsIndex();
            stopTimeoutBombs();
            function sparkTimeoutBomb() {
                var run = true;
                var timeoutBomb = runOnce(Delay(function() {
                    if (run) {
                        var retryCount = intervals.length;
                        if (index < retryCount) {
                            timeoutAction(++index, retryCount);
                            //schedule next timeout bomb
                            stopTimeoutBombs = sparkTimeoutBomb();
                        } else {
                            abortAction();
                        }
                    }
                }, intervals[index]));

                return function() {
                    run = false;
                    stop(timeoutBomb);
                }
            }
            stopTimeoutBombs = sparkTimeoutBomb();

            return function() {
                return index;
            }
        }

        function recalculateRetryIntervals() {
            return asArray(collect(networkErrorRetryTimeouts, function (factor) {
                return factor * heartbeatTimeout + NetworkDelay;
            }));
        }

        function networkErrorRetry(i, retries) {
            warn(logger, 'failed to connect ' + i + ' time' + (i > 1 ? 's' : '') + (i < retries ? ', retrying ...' : ''));
            broadcast(connectionTroubleListeners);
            connect();
        }

        function networkFailure() {
            broadcast(connectionDownListeners);
        }

        function resetTimeoutBomb() {
            pendingRetryIndex = chainTimeoutBombs(networkErrorRetry, networkFailure, recalculateRetryIntervals(), initialRetryIndex);
        }

        function adjustTimeoutBombIntervals() {
            pendingRetryIndex = chainTimeoutBombs(networkErrorRetry, networkFailure, recalculateRetryIntervals(), pendingRetryIndex);
        }

        function initializeConnection() {
            info(logger, 'initialize connection within window ' + namespace.windowID);
            resetTimeoutBomb();
            setValue(sequenceNo, Number(getValue(sequenceNo)) + 1);
            connect();
        }

        //monitor if the blocking connection needs to be started
        var pollingPeriod = 1000;

        var leaseSlot = Slot(ConnectionLease, asString((new Date).getTime()));
        var connectionSlot = Slot(ConnectionRunning);
        var contextPathSlot;

        function updateLease() {
            setValue(leaseSlot, (new Date).getTime() + pollingPeriod * 3);
        }

        function isLeaseExpired() {
            return asNumber(getValue(leaseSlot)) < (new Date).getTime();
        }

        function shouldEstablishBlockingConnection() {
            return !existsSlot(ConnectionRunning) || isEmpty(getValue(connectionSlot));
        }

        function offerCandidature() {
            setValue(connectionSlot, windowID);
        }

        function isWinningCandidate() {
            return startsWith(getValue(connectionSlot), windowID);
        }

        function markAsOwned() {
            setValue(connectionSlot, windowID + AcquiredMarker);
        }

        function isOwner() {
            return getValue(connectionSlot) == (windowID + AcquiredMarker);
        }

        function hasOwner() {
            return endsWith(getValue(connectionSlot), AcquiredMarker);
        }

        function owner() {
            var owner = getValue(connectionSlot);
            var i = indexOf(owner, AcquiredMarker);
            return i > -1 ? substring(owner, 0, i) : owner;
        }

        function nonMatchingContextPath() {
            return getValue(contextPathSlot) != contextPath();
        }

        var lastOwningWindow = '';
        var paused = false;
        var blockingConnectionMonitor = object(function(method) {
            method(stop, noop);
        });
        function createBlockingConnectionMonitor() {
            //initialize slot only after the context path was setup during page load
            contextPathSlot = Slot(ConnectionContextPath, contextPath());

            //force candidancy so that last opened window belonging to a different servlet context will own the blocking connection
            if (nonMatchingContextPath()) {
                offerCandidature();
                info(logger, 'Blocking connection cannot be shared among multiple web-contexts.\nInitiating blocking connection for "' + contextPath() + '"  web-context...');
            }

            blockingConnectionMonitor = run(Delay(function() {
                if (shouldEstablishBlockingConnection()) {
                    offerCandidature();
                    info(logger, 'blocking connection not initialized...candidate for its creation');
                } else {
                    if (isWinningCandidate()) {
                        if (!hasOwner()) {
                            markAsOwned();
                            //start blocking connection since no other window has started it
                            //but only when at least one pushId is registered
                            if (notEmpty(registeredPushIds())) {
                                initializeConnection();
                            }
                        }
                        updateLease();
                    }
                    if (isLeaseExpired()) {
                        //offer candidature with random delay to decrease the chance of collision
                        //maximum delay should be half the lease interval to avoid having the lease after it just expired
                        setTimeout(offerCandidature, 1.5 * Math.random() * pollingPeriod);
                        info(logger, 'blocking connection lease expired...candidate for its creation');
                    }
                }

                if (isOwner()) {
                    var ids = registeredPushIds();
                    if ((size(ids) != size(lastSentPushIds)) || notEmpty(complement(ids, lastSentPushIds))) {
                        //reconnect to send the current list of pushIDs
                        //abort the previous blocking connection in case is still alive
                        abort(listener);
                        connect();
                    }
                } else {
                    //ensure that only one blocking connection exists
                    stopTimeoutBombs();
                    abort(listener);
                }

                //detect when connection is owned by a different window
                var currentlyOwningWindow = getValue(connectionSlot);
                if (hasOwner()) {
                    if (lastOwningWindow != currentlyOwningWindow) {
                        lastOwningWindow = currentlyOwningWindow;
                        broadcast(connectionReEstablished, [ owner() ]);
                    }
                } else {
                    lastOwningWindow = '';
                }
            }, pollingPeriod));
        }

        return object(function(method) {
            method(onSend, function(self, callback) {
                append(onSendListeners, callback);
            });

            method(onReceive, function(self, callback) {
                append(onReceiveListeners, callback);
            });

            method(onServerError, function(self, callback) {
                append(onServerErrorListeners, callback);
            });

            method(whenDown, function(self, callback) {
                append(connectionDownListeners, callback);
            });

            method(whenTrouble, function(self, callback) {
                append(connectionTroubleListeners, callback);
            });

            method(whenStopped, function(self, callback) {
                append(connectionStoppedListeners, callback);
            });

            method(whenReEstablished, function(self, callback) {
                append(connectionReEstablished, callback);
            });

            method(startConnection, function(self) {
                createBlockingConnectionMonitor();
                info(logger, 'connection monitoring started within window ' + namespace.windowID);
                paused = false;
            });

            method(resumeConnection, function(self) {
                if (paused) {
                    connect = requestForBlockingResponse;
                    initializeConnection();
                    createBlockingConnectionMonitor();
                    paused = false;
                }
            });

            method(pauseConnection, function(self) {
                if (not(paused)) {
                    abort(listener);
                    stop(blockingConnectionMonitor);
                    stopTimeoutBombs();
                    connect = noop;
                    paused = true;
                    broadcast(connectionStoppedListeners, ['connection stopped']);
                }
            });

            method(controlRequest, function(self, parameterCallback, headerCallback, responseCallback) {
                if (paused) {
                    var uri = resolveURI(namespace.push.configuration.blockingConnectionURI);
                    postAsynchronously(channel, uri, function(q) {
                        parameter(q, WindowID, namespace.windowID);
                        each(lastSentPushIds, curry(parameter, q, PushID));
                        parameterCallback(curry(parameter, q));
                    }, function(request) {
                        FormPost(request);
                        headerCallback(curry(setHeader, request));
                    }, $witch(function (condition) {
                        condition(OK, function(response) {
                            responseCallback(curry(getHeader, response), contentAsText(response), contentAsDOM(response));
                        });
                        condition(ServerInternalError, function() {
                            throw statusText(response);
                        });
                    }));
                } else {
                    throw 'Cannot make a request while the blocking connection is running.';
                }
            });

            method(changeHeartbeatInterval, function(self, interval) {
                setupNetworkErrorRetries();
                heartbeatTimeout = interval;
                info(logger, 'heartbeat timeout changed to ' + interval + ' ms');
                adjustTimeoutBombIntervals();
            });

            method(reconfigure, function(self) {
                setupNetworkErrorRetries();
                adjustTimeoutBombIntervals();
                setupServerErrorRetries();
            });

            method(shutdown, function(self) {
                try {
                    //shutdown once
                    method(shutdown, noop);
                    connect = noop;
                    resetTimeoutBomb = noop;
                } catch (e) {
                    error(logger, 'error during shutdown', e);
                    //ignore, we really need to shutdown
                } finally {
                    broadcast(connectionStoppedListeners, ['connection stopped']);
                    onReceiveListeners = connectionDownListeners = onServerErrorListeners = connectionStoppedListeners = [];
                    abort(listener);
                    stopTimeoutBombs();
                    stop(blockingConnectionMonitor);
                    removeSlot(connectionSlot);
                }
            });
        });
    };
})();

