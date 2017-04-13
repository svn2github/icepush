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
var reconfigure = operator();
var shutdown = operator();
var AsyncConnection;

(function() {
    var SequenceNumber = 'ice.push.sequence';
    var ConnectionRunning = 'ice.connection.running';
    var ConnectionLease = 'ice.connection.lease';
    var ConnectionContextPath = 'ice.connection.contextpath';
    var AcquiredMarker = ':acquired';
    var NetworkDelay = 5000;//5s of delay, possibly introduced by network
    var DefaultConfiguration = {
        heartbeat:{
            interval: {
                $numberLong: 6500
            }
        },
        network_error_retry_timeouts: [1, 1, 1, 2, 2, 3],
        server_error_handler: {
            delays: "1000, 2000, 4000"
        },
        response_timeout_handler: {
            retries: 3
        }
    };

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

    AsyncConnection = function(logger, windowID, mainConfiguration) {
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
        var configuration = mainConfiguration.configuration || DefaultConfiguration;
        var heartbeatTimestamp = String(new Date().getTime());

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
            return configuration.contextPath;
        }

        function requestForBlockingResponse() {
            try {
                debug(logger, "closing previous connection...");
                close(listener);

                lastSentPushIds = registeredPushIds();
                if (isEmpty(lastSentPushIds)) {
                    stopTimeoutBombs();
                    broadcast(connectionStoppedListeners, ['connection stopped, no pushIDs registered']);
                } else {
                    debug(logger, 'connect...');
                    var uri = mainConfiguration.uri + mainConfiguration.account + '/realms/' + mainConfiguration.realm + '/push-ids?access_token=' + encodeURIComponent(mainConfiguration.access_token) + '&op=listen';
                    var body = JSON.stringify({
                        'access_token': mainConfiguration.access_token,
                        "expires_in": 3600000,
                        'browser': lookupCookieValue(BrowserIDName),
                        'heartbeat': {
                            'timestamp': { "$numberLong" : heartbeatTimestamp }
                        },
                        'op': 'listen',
                        'sequence_number': {
                            "$numberLong" : getValue(sequenceNo)
                        },
                        'window': namespace.windowID,
                        'push_ids': lastSentPushIds
                    });
                    listener = postAsynchronously(channel, uri, body, JSONRequest, $witch(function (condition) {
                        condition(OK, function (response) {
                            var content = contentAsText(response);
                            var reconnect = getHeader(response, 'X-Connection') != 'close';
                            var nonEmptyResponse = notEmpty(content);

                            if (reconnect) {
                                if (nonEmptyResponse) {
                                    try {
                                        var result = JSON.parse(content);
                                        if (result.sequence_number) {
                                            //update sequence number incremented by the server
                                            setValue(sequenceNo, result.sequence_number.$numberLong);
                                        }
                                        if (result.heartbeat && result.heartbeat.timestamp) {
                                            heartbeatTimestamp = result.heartbeat.timestamp.$numberLong;
                                        }
                                    } finally {
                                        broadcast(onReceiveListeners, [response]);
                                        resetEmptyResponseRetries();
                                    }
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
        function setupNetworkErrorRetries(cfg) {
            heartbeatTimeout = cfg.heartbeat.interval.$numberLong || DefaultConfiguration.heartbeat.interval.$numberLong;
            networkErrorRetryTimeouts = cfg.network_error_retry_timeouts || DefaultConfiguration.network_error_retry_timeouts;
            emptyResponseRetries = cfg.response_timeout_handler.retries || DefaultConfiguration.response_timeout_handler.retries;
        }
        setupNetworkErrorRetries(configuration);

        var serverErrorRetryTimeouts;
        var retryOnServerError;
        function setupServerErrorRetries(cfg) {
            serverErrorRetryTimeouts = collect(split(cfg.server_error_handler && cfg.server_error_handler.delays ? cfg.server_error_handler.delays : DefaultConfiguration.server_error_retry_timeouts, ' '), Number);
            retryOnServerError = timedRetryAbort(connect, broadcaster(onServerErrorListeners), serverErrorRetryTimeouts);
        }
        setupServerErrorRetries(configuration);

        //count the number of consecutive empty responses
        var emptyResponseRetries;
        function resetEmptyResponseRetries() {
            emptyResponseRetries = configuration.response_timeout_handler.retries || DefaultConfiguration.response_timeout_handler.retries;
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

            method(reconfigure, function(self, configuration) {
                setupNetworkErrorRetries(configuration);
                adjustTimeoutBombIntervals();
                setupServerErrorRetries(configuration);
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
                    if (isOwner()) {
                        removeSlot(connectionSlot);
                    }
                }
            });
        });
    };
})();

