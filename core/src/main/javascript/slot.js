var setValue = operator();
var getValue = operator();
var Slot;

(function () {
    if (window.localStorage) {
        Slot = function LocalStorageSlot(name) {
            return object(function(method) {
                method(getValue, function(self) {
                    var val = window.localStorage.getItem(name);
                    return val ? val : '';
                });

                method(setValue, function(self, val) {
                    window.localStorage.setItem(name, val);
                });
            });
        };
    } else {
        Slot = function CookieSlot(name) {
            return object(function(method) {
                method(getValue, function(self) {
                    try {
                        var c = lookupCookie(name);
                        value(c);
                    } catch (e) {
                        Cookie(name, '');
                    }
                });

                method(setValue, function(self, val) {
                    try {
                        var c = lookupCookie(name);
                        update(c, val);
                    } catch (e) {
                        Cookie(name, val);
                    }
                });
            });
        };
    }
}());