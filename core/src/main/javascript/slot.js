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
    if (false) {
        //create slot that is visible only to the current window
        var slots = {};
        Slot = function LocalWindowSlot(name, val) {
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

        existsSlot = function (name) {
            return slots[name] != null;
        };

        removeSlot = function (name) {
            delete slots[name];
        };
    } else {
        if (window.localStorage) {
            Slot = function LocalStorageSlot(name, val) {
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

            existsSlot = function (name) {
                return window.localStorage.getItem(name) != null;
            };

            removeSlot = function (name) {
                window.localStorage.removeItem(name);
            };
        } else {
            Slot = function CookieSlot(name, val) {
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

            existsSlot = existsCookie;

            removeSlot = function (name) {
                if (existsCookie(name)) {
                    remove(lookupCookie(name));
                }
            }
        }
    }
}());