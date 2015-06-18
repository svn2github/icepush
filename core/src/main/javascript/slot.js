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

var setValue = operator();
var getValue = operator();
var existsSlot;
var removeSlot;
var Slot;

(function () {
    //create slot that is visible only to the current window
    var slots = {};
    var WindowSlot = function (name, val) {
        slots[name] = val || '';

        return object(function (method) {
            method(getValue, function (self) {
                var value = slots[name];
                return value ? value : '';
            });

            method(setValue, function (self, val) {
                slots[name] = val;
            });
        });
    };

    var existsWindowSlot = function (name) {
        return slots[name] != null;
    };

    var removeWindowSlot = function (name) {
        delete slots[name];
    };

    //create slot that is visible to the entire browser (all windows)
    var BrowserSlot;
    var existsBrowserSlot;
    var removeBrowserSlot;
    if (window.localStorage) {
        BrowserSlot = function LocalStorageSlot(name, val) {
            window.localStorage.setItem(name, window.localStorage.getItem(name) || '');

            return object(function (method) {
                method(getValue, function (self) {
                    var val = window.localStorage.getItem(name);
                    return val ? val : '';
                });

                method(setValue, function (self, val) {
                    window.localStorage.setItem(name, val || '');
                });
            });
        };

        existsBrowserSlot = function (name) {
            return window.localStorage.getItem(name) != null;
        };

        removeBrowserSlot = function (name) {
            window.localStorage.removeItem(name);
        };
    } else {
        BrowserSlot = function CookieSlot(name, val) {
            var c = existsCookie(name) ? lookupCookie(name) : Cookie(name, val);

            return object(function (method) {
                method(getValue, function (self) {
                    try {
                        return value(c);
                    } catch (e) {
                        c = Cookie(name, '');
                        return '';
                    }
                });

                method(setValue, function (self, val) {
                    try {
                        update(c, val);
                    } catch (e) {
                        c = Cookie(name, val);
                    }
                });
            });
        };

        existsBrowserSlot = existsCookie;

        removeBrowserSlot = function (name) {
            if (existsCookie(name)) {
                remove(lookupCookie(name));
            }
        }
    }

    function nonSharedSlot() {
        return namespace.push && namespace.push.configuration && namespace.push.configuration.nonSharedConnection;
    }

    Slot = function (name, val) {
        return object(function (method) {
            var slot;
            var previousSharingType;
            function acquireSlot() {
                var currentSharingType = nonSharedSlot();
                var oldVal;
                //get value from the previous slot
                if (slot) {
                    oldVal = getValue(slot);
                }
                //re-create slot if type differs or slot does not exist
                if (previousSharingType != currentSharingType || !slot) {
                    slot = currentSharingType ? WindowSlot(name) : BrowserSlot(name);
                    previousSharingType = currentSharingType;
                }
                //set previous value if defined
                if (oldVal) {
                    setValue(slot, oldVal);
                }

                return slot;
            }


            method(getValue, function (self) {
                return getValue(acquireSlot());
            });

            method(setValue, function (self, val) {
                setValue(acquireSlot(), val);
            });
        });
    };

    existsSlot = function (name) {
        return nonSharedSlot() ? existsWindowSlot(name) : existsBrowserSlot(name);
    };

    removeSlot = function (name) {
        return nonSharedSlot() ? removeWindowSlot(name) : removeBrowserSlot(name);
    };
}());