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
 */

(function() {
    var expiryIntervalFactor = 3;
    var pushIDIntervals = [];

    function now() {
        return (new Date()).getTime();
    }

    namespace.onNotification(function(ids) {
        each(ids, function(id) {
           //update associated pushID notification interval and last notification time
            var tuple = detect(pushIDIntervals, function(entry) {
                return entry.pushid == id;
            });

            if (tuple) {
                var newInterval = now() - tuple.timestamp;
                if (tuple.interval == Number.MAX_VALUE || newInterval > tuple.interval) {
                    tuple.interval = newInterval;
                }
                tuple.timestamp = now();
            } else {
                append(pushIDIntervals, {pushid: id, interval: Number.MAX_VALUE, timestamp: now()});
            }
        });
    })

    run(Delay(function() {
        var idsToDiscard = [];
        var newPushIDIntervals = [];
        each(pushIDIntervals, function(entry) {
            if (entry.interval * expiryIntervalFactor < now() - entry.timestamp) {
                append(idsToDiscard, entry.pushid);
            } else {
                append(newPushIDIntervals, entry);
            }
        });
        pushIDIntervals = newPushIDIntervals;

        if (notEmpty(idsToDiscard)) {
            info(logger, 'PushIds ' + join(idsToDiscard, ', ') + ' are expired.');
            delistPushIDsWithBrowser(idsToDiscard);
        }
    }, 60 * 1000));//1 minute poll interval
})();