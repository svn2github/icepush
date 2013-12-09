var setValue = operator();
var getValue = operator();
var existsSlot;
var removeSlot;
var lookupSlot;
var Slot;

(function () {
    if (window.localStorage) {
        Slot = function LocalStorageSlot(name, val) {
            window.localStorage.setItem(name, window.localStorage.getItem(name) || '');

            return object(function(method) {
                method(getValue, function(self) {
                    var val = window.localStorage.getItem(name);
                    return val ? val : '';
                });

                method(setValue, function(self, val) {
                    window.localStorage.setItem(name, val || '');
                });
            });
        };

        existsSlot = function(name) {
            return window.localStorage.getItem(name) != null;
        };

        removeSlot = function(name) {
            window.localStorage.removeItem(name);
        };
    } else {
        Slot = function CookieSlot(name, val) {
            var c = existsCookie(name) ? lookupCookie(name) : Cookie(name, val);

            return object(function(method) {
                method(getValue, function(self) {
                    try {
                        return value(c);
                    } catch (e) {
                        c = Cookie(name, '');
                        return '';
                    }
                });

                method(setValue, function(self, val) {
                    try {
                        update(c, val);
                    } catch (e) {
                        c = Cookie(name, val);
                    }
                });
            });
        };

        existsSlot = existsCookie;

        removeSlot = function(name) {
            if (existsCookie(name)) {
                remove(lookupCookie(name));
            }
        }
    }
}());