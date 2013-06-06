window.ice = window.ice ? window.ice : {};
window.ice.lib = {};
ice.module = function module(definitions) {
    var context = {};
    function defineVariable(name, variable) {
        if (context[name]) {
            throw 'variable "' + name + '" already defined';
        }
        context[name] = variable;
        return variable;
    }
    definitions(defineVariable);
    return context;
};
ice.importFrom = function importFrom(moduleName) {
    var context = window;
    var atoms = moduleName.split('.');
    for (var i = 0, size = atoms.length; i < size; i++) {
        context = context[atoms[i]];
    }
    var code = [];
    for (var p in context) {
        if (context.hasOwnProperty(p)) {
            code.push('var ' + p + '=' + moduleName + '["' + p + '"]');
        }
    }
    return code.join(';')
};
ice.evaluate = eval;
ice.lib.oo = ice.module(function(exportAs) {
    function isArray(a) {
        return a && !!a.push;
    }
    function isString(s) {
        return typeof s == 'string';
    }
    function isNumber(s) {
        return typeof s == 'number';
    }
    function isBoolean(s) {
        return typeof s == 'boolean';
    }
    function isIndexed(s) {
        return typeof s.length == 'number';
    }
    function isObject(o) {
        return o.instanceTag == o;
    }
    var uid = (function() {
        var id = 0;
        return function() {
            return id++;
        };
    })();
    function operationNotSupported() {
        throw 'operation not supported';
    }
    function operator(defaultOperation) {
        return function() {
            var args = arguments;
            var instance = arguments[0];
            if (instance.instanceTag && instance.instanceTag == instance) {
                var method = instance(arguments.callee);
                if (method) {
                    return method.apply(method, args);
                } else {
                    operationNotSupported();
                }
            } else {
                return defaultOperation ? defaultOperation.apply(defaultOperation, args) : operationNotSupported();
            }
        };
    }
    var asString = operator(String);
    var asNumber = operator(Number);
    var hash = operator(function(o) {
        var s;
        if (isString(o)) {
            s = o;
        } else if (isNumber(o)) {
            return Math.abs(Math.round(o));
        } else {
            s = o.toString();
        }
        var h = 0;
        for (var i = 0, l = s.length; i < l; i++) {
            var c = parseInt(s[i], 36);
            if (!isNaN(c)) {
                h = c + (h << 6) + (h << 16) - h;
            }
        }
        return Math.abs(h);
    });
    var equal = operator(function(a, b) {
        return a == b;
    });
    function object(definition) {
        var operators = [];
        var methods = [];
        var unknown = null;
        var id = uid();
        operators.push(hash);
        methods.push(function(self) {
            return id;
        });
        operators.push(equal);
        methods.push(function(self, other) {
            return self == other;
        });
        operators.push(asString);
        methods.push(function(self) {
            return 'Object:' + id.toString(16);
        });
        definition(function(operator, method) {
            var size = operators.length;
            for (var i = 0; i < size; i++) {
                if (operators[i] == operator) {
                    methods[i] = method;
                    return;
                }
            }
            operators.push(operator);
            methods.push(method);
        }, function(method) {
            unknown = method;
        });
        function self(operator) {
            var size = operators.length;
            for (var i = 0; i < size; i++) {
                if (operators[i] == operator) {
                    return methods[i];
                }
            }
            return unknown;
        }
        return self.instanceTag = self;
    }
    function objectWithAncestors() {
        var definition = arguments[0];
        var args = arguments;
        var o = object(definition);
        function self(operator) {
            var method = o(operator);
            if (method) {
                return method;
            } else {
                var size = args.length;
                for (var i = 1; i < size; i++) {
                    var ancestor = args[i];
                    var overriddenMethod = ancestor(operator);
                    if (overriddenMethod) {
                        return overriddenMethod;
                    }
                }
                return null;
            }
        }
        return self.instanceTag = self;
    }
    exportAs('isArray', isArray);
    exportAs('isString', isString);
    exportAs('isNumber', isNumber);
    exportAs('isBoolean', isBoolean);
    exportAs('isIndexed', isIndexed);
    exportAs('isObject', isObject);
    exportAs('asString', asString);
    exportAs('asNumber', asNumber);
    exportAs('hash', hash);
    exportAs('equal', equal);
    exportAs('operationNotSupported', operationNotSupported);
    exportAs('operator', operator);
    exportAs('object', object);
    exportAs('objectWithAncestors', objectWithAncestors);
});
ice.lib.functional = ice.module(function(exportAs) {
    function apply(fun, arguments) {
        return fun.apply(fun, arguments);
    }
    function withArguments() {
        var args = arguments;
        return function(fun) {
            apply(fun, args);
        };
    }
    function curry() {
        var args = arguments;
        return function() {
            var curriedArguments = [];
            var fun = args[0];
            for (var i = 1; i < args.length; i++) curriedArguments.push(args[i]);
            for (var j = 0; j < arguments.length; j++) curriedArguments.push(arguments[j]);
            return apply(fun, curriedArguments);
        };
    }
    function $witch(tests, defaultRun) {
        return function(val) {
            var args = arguments;
            var conditions = [];
            var runs = [];
            tests(function(condition, run) {
                conditions.push(condition);
                runs.push(run);
            });
            var size = conditions.length;
            for (var i = 0; i < size; i++) {
                if (apply(conditions[i], args)) {
                    return apply(runs[i], args);
                }
            }
            if (defaultRun) apply(defaultRun, args);
        };
    }
    function identity(arg) {
        return arg;
    }
    function negate(b) {
        return !b;
    }
    function greater(a, b) {
        return a > b;
    }
    function less(a, b) {
        return a < b;
    }
    function not(a) {
        return !a;
    }
    function multiply(a, b) {
        return a * b;
    }
    function plus(a, b) {
        return a + b;
    }
    function max(a, b) {
        return a > b ? a : b;
    }
    function increment(value, step) {
        return value + (step ? step : 1);
    }
    function decrement(value, step) {
        return value - (step ? step : 1);
    }
    function any() {
        return true;
    }
    function none() {
        return false;
    }
    function noop() {
    }
    exportAs('apply', apply);
    exportAs('withArguments', withArguments);
    exportAs('curry', curry);
    exportAs('$witch', $witch);
    exportAs('identity', identity);
    exportAs('negate', negate);
    exportAs('greater', greater);
    exportAs('less', less);
    exportAs('not', not);
    exportAs('multiply', multiply);
    exportAs('plus', plus);
    exportAs('max', max);
    exportAs('increment', increment);
    exportAs('decrement', decrement);
    exportAs('any', any);
    exportAs('none', none);
    exportAs('noop', noop);
});
ice.lib.delay = ice.module(function(exportAs) {
    eval(ice.importFrom('ice.lib.oo'));
    var run = operator();
    var runOnce = operator();
    var stop = operator();
    function Delay(f, milliseconds) {
        return object(function(method) {
            var id = null;
            var canceled = false;
            method(run, function(self, times) {
                if (id || canceled) return;
                var call = times ? function() {
                    try {
                        f();
                    } finally {
                        if (--times < 1) stop(self);
                    }
                } : f;
                id = setInterval(call, milliseconds);
                return self;
            });
            method(runOnce, function(self) {
                return run(self, 1);
            });
            method(stop, function(self) {
                if (id) {
                    clearInterval(id);
                    id = null;
                } else {
                    canceled = true;
                }
            });
        });
    }
    exportAs('run', run);
    exportAs('runOnce', runOnce);
    exportAs('stop', stop);
    exportAs('Delay', Delay);
});
ice.lib.string = ice.module(function(exportAs) {
    function indexOf(s, substring) {
        var index = s.indexOf(substring);
        if (index >= 0) {
            return index;
        } else {
            throw '"' + s + '" does not contain "' + substring + '"';
        }
    }
    function lastIndexOf(s, substring) {
        var index = s.lastIndexOf(substring);
        if (index >= 0) {
            return index;
        } else {
            throw 'string "' + s + '" does not contain "' + substring + '"';
        }
    }
    function startsWith(s, pattern) {
        return s.indexOf(pattern) == 0;
    }
    function endsWith(s, pattern) {
        return s.lastIndexOf(pattern) == s.length - pattern.length;
    }
    function containsSubstring(s, substring) {
        return s.indexOf(substring) >= 0;
    }
    function blank(s) {
        return /^\s*$/.test(s);
    }
    function split(s, separator) {
        return s.length == 0 ? [] : s.split(separator);
    }
    function replace(s, regex, replace) {
        return s.replace(regex, replace);
    }
    function toLowerCase(s) {
        return s.toLowerCase();
    }
    function toUpperCase(s) {
        return s.toUpperCase();
    }
    function substring(s, from, to) {
        return s.substring(from, to);
    }
    function trim(s) {
        s = s.replace(/^\s+/, '');
        for (var i = s.length - 1; i >= 0; i--) {
            if (/\S/.test(s.charAt(i))) {
                s = s.substring(0, i + 1);
                break;
            }
        }
        return s;
    }
    var asNumber = Number;
    function asBoolean(s) {
        return 'true' == s || 'any' == s;
    }
    function asRegexp(s) {
        return new RegExp(s);
    }
    exportAs('indexOf', indexOf);
    exportAs('lastIndexOf', lastIndexOf);
    exportAs('startsWith', startsWith);
    exportAs('endsWith', endsWith);
    exportAs('containsSubstring', containsSubstring);
    exportAs('blank', blank);
    exportAs('split', split);
    exportAs('replace', replace);
    exportAs('toLowerCase', toLowerCase);
    exportAs('toUpperCase', toUpperCase);
    exportAs('substring', substring);
    exportAs('trim', trim);
    exportAs('asNumber', asNumber);
    exportAs('asBoolean', asBoolean);
    exportAs('asRegexp', asRegexp);
});
ice.lib.collection = ice.module(function(exportAs) {
    eval(ice.importFrom('ice.lib.functional'));
    eval(ice.importFrom('ice.lib.oo'));
    var indexOf = operator($witch(function(condition) {
        condition(isString, function(items, item) {
            return items.indexOf(item);
        });
        condition(isArray, function(items, item) {
            for (var i = 0, size = items.length; i < size; i++) {
                if (items[i] == item) {
                    return i;
                }
            }
            return -1;
        });
        condition(any, operationNotSupported);
    }));
    var concatenate = operator(function(items, other) {
        return items.concat(other);
    });
    var append = operator(function(items, item) {
        if (isArray(items)) {
            items.push(item);
            return items;
        } else {
            operationNotSupported();
        }
    });
    var insert = operator($witch(function(condition) {
        condition(isArray, function(items, item) {
            items.unshift(item);
            return items;
        });
        condition(any, operationNotSupported);
    }));
    var each = operator(function(items, iterator) {
        var size = items.length;
        for (var i = 0; i < size; i++) iterator(items[i], i);
    });
    var inject = operator(function(items, initialValue, injector) {
        var tally = initialValue;
        var size = items.length;
        for (var i = 0; i < size; i++) {
            tally = injector(tally, items[i]);
        }
        return tally;
    });
    var select = operator($witch(function(condition) {
        condition(isArray, function(items, selector) {
            return inject(items, [], function(tally, item) {
                return selector(item) ? append(tally, item) : tally;
            });
        });
        condition(isString, function(items, selector) {
            return inject(items, '', function(tally, item) {
                return selector(item) ? concatenate(tally, item) : tally;
            });
        });
        condition(isIndexed, function(items, selector) {
            return Stream(function(cellConstructor) {
                function selectingStream(start, end) {
                    if (start > end) return null;
                    var item = items[start];
                    return selector(item) ?
                        function() {
                            return cellConstructor(item, selectingStream(start + 1, end));
                        } : selectingStream(start + 1, end);
                }
                return selectingStream(0, items.length - 1);
            });
        });
    }));
    var detect = operator(function(items, iterator, notDetectedThunk) {
        var size = items.length;
        for (var i = 0; i < size; i++) {
            var element = items[i];
            if (iterator(element, i)) {
                return element;
            }
        }
        return notDetectedThunk ? notDetectedThunk(items) : null;
    });
    var contains = operator($witch(function(condition) {
        condition(isString, function(items, item) {
            return items.indexOf(item) > -1;
        });
        condition(isArray, function(items, item) {
            var size = items.length;
            for (var i = 0; i < size; i++) {
                if (equal(items[i], item)) {
                    return true;
                }
            }
            return false;
        });
        condition(any, operationNotSupported);
    }));
    var size = operator(function(items) {
        return items.length;
    });
    var empty = operator(function(items) {
        items.length = 0;
    });
    var isEmpty = operator(function(items) {
        return items.length == 0;
    });
    var notEmpty = function(items) {
        return !isEmpty(items);
    };
    var collect = operator($witch(function(condition) {
        condition(isString, function(items, collector) {
            return inject(items, '', function(tally, item) {
                return concatenate(tally, collector(item));
            });
        });
        condition(isArray, function(items, collector) {
            return inject(items, [], function(tally, item) {
                return append(tally, collector(item));
            });
        });
        condition(isIndexed, function(items, collector) {
            return Stream(function(cellConstructor) {
                function collectingStream(start, end) {
                    if (start > end) return null;
                    return function() {
                        return cellConstructor(collector(items[start], start), collectingStream(start + 1, end));
                    };
                }
                return collectingStream(0, items.length - 1);
            });
        });
    }));
    var sort = operator(function(items, sorter) {
        return copy(items).sort(function(a, b) {
            return sorter(a, b) ? -1 : 1;
        });
    });
    var reverse = operator(function(items) {
        return copy(items).reverse();
    });
    var copy = operator(function(items) {
        return inject(items, [], curry(append));
    });
    var join = operator(function(items, separator) {
        return items.join(separator);
    });
    var inspect = operator();
    var reject = function(items, rejector) {
        return select(items, function(i) {
            return !rejector(i);
        });
    };
    var intersect = operator(function(items, other) {
        return select(items, curry(contains, other));
    });
    var complement = operator(function(items, other) {
        return reject(items, curry(contains, other));
    });
    var broadcast = function(items, args) {
        args = args || [];
        each(items, function(i) {
            apply(i, args);
        });
    };
    var broadcaster = function(items) {
        return function() {
            var args = arguments;
            each(items, function(i) {
                apply(i, args);
            });
        };
    };
    var asArray = function(items) {
        return inject(items, [], append);
    };
    var asSet = function(items) {
        return inject(items, [], function(set, item) {
            if (not(contains(set, item))) {
                append(set, item);
            }
            return set;
        });
    };
    var key = operator();
    var value = operator();
    function Cell(k, v) {
        return object(function(method) {
            method(key, function(self) {
                return k;
            });
            method(value, function(self) {
                return v;
            });
            method(asString, function(self) {
                return 'Cell[' + asString(k) + ': ' + asString(v) + ']';
            });
        });
    }
    function Stream(streamDefinition) {
        var stream = streamDefinition(Cell);
        return object(function(method) {
            method(each, function(self, iterator) {
                var cursor = stream;
                while (cursor != null) {
                    var cell = cursor();
                    iterator(key(cell));
                    cursor = value(cell);
                }
            });
            method(inject, function(self, initialValue, injector) {
                var tally = initialValue;
                var cursor = stream;
                while (cursor != null) {
                    var cell = cursor();
                    tally = injector(tally, key(cell));
                    cursor = value(cell);
                }
                return tally;
            });
            method(join, function(self, separator) {
                var tally;
                var cursor = stream;
                while (cursor != null) {
                    var cell = cursor();
                    var itemAsString = asString(key(cell));
                    tally = tally ? tally + separator + itemAsString : itemAsString;
                    cursor = value(cell);
                }
                return tally;
            });
            method(collect, function(self, collector) {
                return Stream(function(cellConstructor) {
                    function collectingStream(stream) {
                        if (!stream) return null;
                        var cell = stream();
                        return function() {
                            return cellConstructor(collector(key(cell)), collectingStream(value(cell)));
                        };
                    }
                    return collectingStream(stream);
                });
            });
            method(contains, function(self, item) {
                var cursor = stream;
                while (cursor != null) {
                    var cell = cursor();
                    if (item == key(cell)) return true;
                    cursor = value(cell);
                }
                return false;
            });
            method(size, function(self) {
                var cursor = stream;
                var i = 0;
                while (cursor != null) {
                    i++;
                    cursor = value(cursor());
                }
                return i;
            });
            method(select, function(self, selector) {
                return Stream(function(cellConstructor) {
                    function select(stream) {
                        if (!stream) return null;
                        var cell = stream();
                        var k = key(cell);
                        var v = value(cell);
                        return selector(k) ? function() {
                            return cellConstructor(k, select(v));
                        } : select(v);
                    }
                    return select(stream);
                });
            });
            method(detect, function(self, detector, notDetectedThunk) {
                var cursor = stream;
                var result;
                while (cursor != null) {
                    var cell = cursor();
                    var k = key(cell);
                    if (detector(k)) {
                        result = k;
                        break;
                    }
                    cursor = value(cell);
                }
                if (result) {
                    return result;
                } else {
                    return notDetectedThunk ? notDetectedThunk(self) : null;
                }
            });
            method(isEmpty, function(self) {
                return stream == null;
            });
            method(copy, function(self) {
                return Stream(streamDefinition);
            });
            method(asString, function(self) {
                return 'Stream[' + join(self, ', ') + ']';
            });
        });
    }
    exportAs('indexOf', indexOf);
    exportAs('concatenate', concatenate);
    exportAs('append', append);
    exportAs('insert', insert);
    exportAs('each', each);
    exportAs('inject', inject);
    exportAs('select', select);
    exportAs('detect', detect);
    exportAs('contains', contains);
    exportAs('size', size);
    exportAs('empty', empty);
    exportAs('isEmpty', isEmpty);
    exportAs('notEmpty', notEmpty);
    exportAs('collect', collect);
    exportAs('sort', sort);
    exportAs('reverse', reverse);
    exportAs('copy', copy);
    exportAs('join', join);
    exportAs('inspect', inspect);
    exportAs('reject', reject);
    exportAs('intersect', intersect);
    exportAs('complement', complement);
    exportAs('broadcast', broadcast);
    exportAs('broadcaster', broadcaster);
    exportAs('asArray', asArray);
    exportAs('asSet', asSet);
    exportAs('key', key);
    exportAs('value', value);
    exportAs('Cell', Cell);
    exportAs('Stream', Stream);
});
ice.lib.configuration = ice.module(function(exportAs) {
    eval(ice.importFrom('ice.lib.oo'));
    eval(ice.importFrom('ice.lib.string'));
    eval(ice.importFrom('ice.lib.collection'));
    var attributeAsString = operator();
    var attributeAsBoolean = operator();
    var attributeAsNumber = operator();
    var valueAsStrings = operator();
    var valueAsBooleans = operator();
    var valueAsNumbers = operator();
    var childConfiguration = operator();
    function XMLDynamicConfiguration(lookupElement) {
        function asBoolean(s) {
            return 'true' == toLowerCase(s);
        }
        function lookupAttribute(name) {
            var a = lookupElement().getAttribute(name);
            if (a) {
                return a;
            } else {
                throw 'unknown attribute: ' + name;
            }
        }
        function lookupValues(name) {
            return collect(asArray(lookupElement().getElementsByTagName(name)), function(e) {
                var valueNode = e.firstChild;
                return valueNode ? valueNode.nodeValue : '';
            });
        }
        return object(function(method) {
            method(attributeAsString, function(self, name, defaultValue) {
                try {
                    return lookupAttribute(name);
                } catch (e) {
                    if (isString(defaultValue)) {
                        return defaultValue;
                    } else {
                        throw e;
                    }
                }
            });
            method(attributeAsNumber, function(self, name, defaultValue) {
                try {
                    return Number(lookupAttribute(name));
                } catch (e) {
                    if (isNumber(defaultValue)) {
                        return defaultValue;
                    } else {
                        throw e;
                    }
                }
            });
            method(attributeAsBoolean, function(self, name, defaultValue) {
                try {
                    return asBoolean(lookupAttribute(name));
                } catch (e) {
                    if (isBoolean(defaultValue)) {
                        return defaultValue;
                    } else {
                        throw e;
                    }
                }
            });
            method(childConfiguration, function(self, name) {
                var elements = lookupElement().getElementsByTagName(name);
                if (isEmpty(elements)) {
                    throw 'unknown configuration: ' + name;
                } else {
                    return XMLDynamicConfiguration(function() {
                        return lookupElement().getElementsByTagName(name)[0];
                    });
                }
            });
            method(valueAsStrings, function(self, name, defaultValues) {
                var values = lookupValues(name);
                return isEmpty(values) && defaultValues ? defaultValues : values;
            });
            method(valueAsNumbers, function(self, name, defaultValues) {
                var values = lookupValues(name);
                return isEmpty(values) && defaultValues ? defaultValues : collect(values, Number);
            });
            method(valueAsBooleans, function(self, name, defaultValues) {
                var values = lookupValues(name);
                return isEmpty(values) && defaultValues ? defaultValues : collect(values, asBoolean);
            });
        });
    }
    exportAs('attributeAsString', attributeAsString);
    exportAs('attributeAsBoolean', attributeAsBoolean);
    exportAs('attributeAsNumber', attributeAsNumber);
    exportAs('valueAsStrings', valueAsStrings);
    exportAs('valueAsBooleans', valueAsBooleans);
    exportAs('valueAsNumbers', valueAsNumbers);
    exportAs('childConfiguration', childConfiguration);
    exportAs('XMLDynamicConfiguration', XMLDynamicConfiguration);
});
ice.lib.window = ice.module(function(exportAs) {
    eval(ice.importFrom('ice.lib.functional'));
    function registerListener(eventType, obj, listener) {
        if (obj.addEventListener) {
            obj.addEventListener(eventType, listener, false);
        } else {
            obj.attachEvent('on' + eventType, listener);
        }
    }
    var onLoad = curry(registerListener, 'load');
    var onUnload = curry(registerListener, 'unload');
    var onBeforeUnload = curry(registerListener, 'beforeunload');
    var onResize = curry(registerListener, 'resize');
    var onKeyPress = curry(registerListener, 'keypress');
    var onKeyUp = curry(registerListener, 'keyup');
    window.width = function() {
        return window.innerWidth ? window.innerWidth : (document.documentElement && document.documentElement.clientWidth) ? document.documentElement.clientWidth : document.body.clientWidth;
    };
    window.height = function() {
        return window.innerHeight ? window.innerHeight : (document.documentElement && document.documentElement.clientHeight) ? document.documentElement.clientHeight : document.body.clientHeight;
    };
    exportAs('registerListener', registerListener);
    exportAs('onLoad', onLoad);
    exportAs('onUnload', onUnload);
    exportAs('onBeforeUnload', onBeforeUnload);
    exportAs('onResize', onResize);
    exportAs('onKeyPress', onKeyPress);
    exportAs('onKeyUp', onKeyUp);
});
ice.lib.cookie = ice.module(function(exportAs) {
    eval(ice.importFrom('ice.lib.oo'));
    eval(ice.importFrom('ice.lib.string'));
    function lookupCookieValue(name) {
        var tupleString = detect(split(asString(document.cookie), '; '), function(tuple) {
            return startsWith(tuple, name);
        }, function() {
            throw 'Cannot find value for cookie: ' + name;
        });
        return decodeURIComponent(contains(tupleString, '=') ? split(tupleString, '=')[1] : '');
    }
    function lookupCookie(name, failThunk) {
        try {
            return Cookie(name, lookupCookieValue(name));
        } catch (e) {
            if (failThunk) {
                return failThunk();
            } else {
                throw e;
            }
        }
    }
    function existsCookie(name) {
        var exists = true;
        lookupCookie(name, function() {
            exists = false;
        });
        return exists;
    }
    var update = operator();
    var remove = operator();
    function Cookie(name, val, path) {
        val = val || '';
        path = path || '/';
        document.cookie = name + '=' + encodeURIComponent(val) + '; path=' + path;
        return object(function(method) {
            method(value, function(self) {
                return lookupCookieValue(name);
            });
            method(update, function(self, val) {
                document.cookie = name + '=' + encodeURIComponent(val) + '; path=' + path;
                return self;
            });
            method(remove, function(self) {
                var date = new Date();
                date.setTime(date.getTime() - 24 * 60 * 60 * 1000);
                document.cookie = name + '=; expires=' + date.toGMTString() + '; path=' + path;
            });
            method(asString, function(self) {
                return 'Cookie[' + name + ', ' + value(self) + ', ' + path + ']';
            });
        });
    }
    exportAs('lookupCookieValue', lookupCookieValue);
    exportAs('lookupCookie', lookupCookie);
    exportAs('existsCookie', existsCookie);
    exportAs('update', update);
    exportAs('remove', remove);
    exportAs('Cookie', Cookie);
});
ice.lib.query = ice.module(function(exportAs) {
    eval(ice.importFrom('ice.lib.functional'));
    eval(ice.importFrom('ice.lib.oo'));
    eval(ice.importFrom('ice.lib.collection'));
    var asURIEncodedString = operator();
    var serializeOn = operator();
    function Parameter(name, value) {
        return objectWithAncestors(function(method) {
            method(asURIEncodedString, function(self) {
                return encodeURIComponent(name) + '=' + encodeURIComponent(value);
            });
            method(serializeOn, function(self, query) {
                addParameter(query, self);
            });
        }, Cell(name, value));
    }
    var addParameter = operator();
    var addNameValue = operator();
    var queryParameters = operator();
    var addQuery = operator();
    var appendToURI = operator();
    function Query() {
        var parameters = [];
        return object(function(method) {
            method(queryParameters, function(self) {
                return parameters;
            });
            method(addParameter, function(self, parameter) {
                append(parameters, parameter);
                return self;
            });
            method(addNameValue, function(self, name, value) {
                append(parameters, Parameter(name, value));
                return self;
            });
            method(addQuery, function(self, appended) {
                serializeOn(appended, self);
                return self;
            });
            method(serializeOn, function(self, query) {
                each(parameters, curry(addParameter, query));
            });
            method(asURIEncodedString, function(self) {
                return join(collect(parameters, asURIEncodedString), '&');
            });
            method(appendToURI, function(self, uri) {
                if (not(isEmpty(parameters))) {
                    return uri + (contains(uri, '?') ? '&' : '?') + asURIEncodedString(self);
                } else {
                    return uri;
                }
            });
            method(asString, function(self) {
                return inject(parameters, '', function(tally, p) {
                    return tally + '|' + key(p) + '=' + value(p) + '|\n';
                });
            });
        });
    }
    exportAs('asURIEncodedString', asURIEncodedString);
    exportAs('serializeOn', serializeOn);
    exportAs('Parameter', Parameter);
    exportAs('Query', Query);
    exportAs('addParameter', addParameter);
    exportAs('addNameValue', addNameValue);
    exportAs('queryParameters', queryParameters);
    exportAs('addQuery', addQuery);
    exportAs('appendToURI', appendToURI);
});
ice.lib.http = ice.module(function(exportAs) {
    eval(ice.importFrom('ice.lib.functional'));
    eval(ice.importFrom('ice.lib.oo'));
    eval(ice.importFrom('ice.lib.collection'));
    eval(ice.importFrom('ice.lib.query'));
    var getSynchronously = operator();
    var getAsynchronously = operator();
    var postSynchronously = operator();
    var postAsynchronously = operator();
    var Client = exportAs('Client', function(autoclose) {
        var newNativeRequest;
        if (window.XMLHttpRequest) {
            newNativeRequest = function() {
                return new XMLHttpRequest();
            };
        } else if (window.ActiveXObject) {
            newNativeRequest = function() {
                return new window.ActiveXObject('Microsoft.XMLHTTP');
            };
        } else {
            throw 'cannot create XMLHttpRequest';
        }
        function withNewQuery(setup) {
            var query = Query();
            setup(query);
            return query;
        }
        var autoClose = autoclose ? close : noop;
        return object(function(method) {
            method(getAsynchronously, function(self, uri, setupQuery, setupRequest, onResponse) {
                var nativeRequestResponse = newNativeRequest();
                var request = RequestProxy(nativeRequestResponse);
                var response = ResponseProxy(nativeRequestResponse);
                nativeRequestResponse.open('GET', appendToURI(withNewQuery(setupQuery), uri), true);
                setupRequest(request);
                nativeRequestResponse.onreadystatechange = function() {
                    if (nativeRequestResponse.readyState == 4) {
                        onResponse(response, request);
                        autoClose(request);
                    }
                };
                nativeRequestResponse.send('');
                return request;
            });
            method(getSynchronously, function(self, uri, setupQuery, setupRequest, onResponse) {
                var nativeRequestResponse = newNativeRequest();
                var request = RequestProxy(nativeRequestResponse);
                var response = ResponseProxy(nativeRequestResponse);
                nativeRequestResponse.open('GET', appendToURI(withNewQuery(setupQuery), uri), false);
                setupRequest(request);
                nativeRequestResponse.send('');
                onResponse(response, request);
                autoClose(request);
            });
            method(postAsynchronously, function(self, uri, setupQuery, setupRequest, onResponse) {
                var nativeRequestResponse = newNativeRequest();
                var request = RequestProxy(nativeRequestResponse);
                var response = ResponseProxy(nativeRequestResponse);
                nativeRequestResponse.open('POST', uri, true);
                setupRequest(request);
                nativeRequestResponse.onreadystatechange = function() {
                    if (nativeRequestResponse.readyState == 4) {
                        onResponse(response, request);
                        autoClose(request);
                    }
                };
                nativeRequestResponse.send(asURIEncodedString(withNewQuery(setupQuery)));
                return request;
            });
            method(postSynchronously, function(self, uri, setupQuery, setupRequest, onResponse) {
                var nativeRequestResponse = newNativeRequest();
                var request = RequestProxy(nativeRequestResponse);
                var response = ResponseProxy(nativeRequestResponse);
                nativeRequestResponse.open('POST', uri, false);
                setupRequest(request);
                nativeRequestResponse.send(asURIEncodedString(withNewQuery(setupQuery)));
                onResponse(response, request);
                autoClose(request);
            });
        });
    });
    var close = operator();
    var abort = operator();
    var setHeader = operator();
    var onResponse = operator();
    function RequestProxy(nativeRequestResponse) {
        return object(function(method) {
            method(setHeader, function(self, name, value) {
                nativeRequestResponse.setRequestHeader(name, value);
            });
            method(close, function(self) {
                nativeRequestResponse.onreadystatechange = noop;
            });
            method(abort, function(self) {
                nativeRequestResponse.onreadystatechange = noop;
                nativeRequestResponse.abort();
                method(abort, noop);
            });
        });
    }
    var statusCode = operator();
    var statusText = operator();
    var getHeader = operator();
    var getAllHeaders = operator();
    var hasHeader = operator();
    var contentAsText = operator();
    var contentAsDOM = operator();
    function ResponseProxy(nativeRequestResponse) {
        return object(function(method) {
            method(statusCode, function() {
                try {
                    return nativeRequestResponse.status;
                } catch (e) {
                    return 0;
                }
            });
            method(statusText, function(self) {
                try {
                    return nativeRequestResponse.statusText;
                } catch (e) {
                    return '';
                }
            });
            method(hasHeader, function(self, name) {
                try {
                    var header = nativeRequestResponse.getResponseHeader(name);
                    return header && header != '';
                } catch (e) {
                    return false;
                }
            });
            method(getHeader, function(self, name) {
                try {
                    return nativeRequestResponse.getResponseHeader(name);
                } catch (e) {
                    return null;
                }
            });
            method(getAllHeaders, function(self, name) {
                try {
                    return collect(reject(split(nativeRequestResponse.getAllResponseHeaders(), '\n'), isEmpty), function(pair) {
                        var nameValue = split(pair, ': ')
                        return Cell(nameValue[0], nameValue[1]);
                    });
                } catch (e) {
                    return [];
                }
            });
            method(contentAsText, function(self) {
                try {
                    return nativeRequestResponse.responseText;
                } catch (e) {
                    return '';
                }
            });
            method(contentAsDOM, function(self) {
                return nativeRequestResponse.responseXML;
            });
            method(asString, function(self) {
                return inject(getAllHeaders(self), 'HTTP Response\n', function(result, header) {
                    return result + key(header) + ': ' + value(header) + '\n';
                }) + contentAsText(self);
            });
        });
    }
    function OK(response) {
        return statusCode(response) == 200;
    }
    function NotFound(response) {
        return statusCode(response) == 404;
    }
    function ServerInternalError(response) {
        var code = statusCode(response);
        return code >= 500 && code < 600;
    }
    function FormPost(request) {
        setHeader(request, 'Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
    }
    exportAs('getSynchronously', getSynchronously);
    exportAs('getAsynchronously', getAsynchronously);
    exportAs('postSynchronously', postSynchronously);
    exportAs('postAsynchronously', postAsynchronously);
    exportAs('close', close);
    exportAs('abort', abort);
    exportAs('setHeader', setHeader);
    exportAs('onResponse', onResponse);
    exportAs('statusCode', statusCode);
    exportAs('statusText', statusText);
    exportAs('getHeader', getHeader);
    exportAs('getAllHeaders', getAllHeaders);
    exportAs('hasHeader', hasHeader);
    exportAs('contentAsText', contentAsText);
    exportAs('contentAsDOM', contentAsDOM);
    exportAs('OK', OK);
    exportAs('NotFound', NotFound);
    exportAs('ServerInternalError', ServerInternalError);
    exportAs('FormPost', FormPost);
});
ice.lib.hashtable = ice.module(function(define) {
    eval(ice.importFrom('ice.lib.functional'));
    eval(ice.importFrom('ice.lib.oo'));
    eval(ice.importFrom('ice.lib.collection'));
    var at = operator();
    var putAt = operator();
    var removeAt = operator();
    var removeInArray = Array.prototype.splice ? function(array, index) {
        array.splice(index, 1);
    } : function(array, index) {
        if (index == array.length - 1) {
            array.length = index;
        } else {
            var rightSlice = array.slice(index + 1);
            array.length = index;
            for (var i = 0, l = rightSlice.length; i < l; ++i) {
                array[index + i] = rightSlice[i];
            }
        }
    };
    function atPrimitive(buckets, bucketCount, k, notFoundThunk) {
        var index = hash(k) % bucketCount;
        var bucket = buckets[index];
        if (bucket) {
            for (var i = 0, l = bucket.length; i < l; i++) {
                var entry = bucket[i];
                if (equal(entry.key, k)) {
                    return entry.value;
                }
            }
            if (notFoundThunk) notFoundThunk();
            return null;
        } else {
            if (notFoundThunk) notFoundThunk();
            return null;
        }
    }
    function putAtPrimitive(buckets, bucketCount, k, v) {
        var index = hash(k) % bucketCount;
        var bucket = buckets[index];
        if (bucket) {
            for (var i = 0, l = bucket.length; i < l; i++) {
                var entry = bucket[i];
                if (equal(entry.key, k)) {
                    var oldValue = entry.value;
                    entry.value = v;
                    return oldValue;
                }
            }
            bucket.push({ key:k, value: v });
            return null;
        } else {
            bucket = [
                {
                    key:k,
                    value: v
                }
            ];
            buckets[index] = bucket;
            return null;
        }
    }
    function removeAtPrimitive(buckets, bucketCount, k) {
        var index = hash(k) % bucketCount;
        var bucket = buckets[index];
        if (bucket) {
            for (var i = 0, l = bucket.length; i < l; i++) {
                var entry = bucket[i];
                if (equal(entry.key, k)) {
                    removeInArray(bucket, i);
                    if (bucket.length == 0) {
                        removeInArray(buckets, index);
                    }
                    return entry.value;
                }
            }
            return null;
        } else {
            return null;
        }
    }
    function injectPrimitive(buckets, initialValue, iterator) {
        var tally = initialValue;
        for (var i = 0, lbs = buckets.length; i < lbs; i++) {
            var bucket = buckets[i];
            if (bucket) {
                for (var j = 0, lb = bucket.length; j < lb; j++) {
                    var entry = bucket[j];
                    if (entry) {
                        tally = iterator(tally, entry.key, entry.value);
                    }
                }
            }
        }
        return tally;
    }
    var internalBuckets = operator();
    var internalBucketCount = operator();
    function HashTable() {
        var buckets = [];
        var bucketCount = 5000;
        return object(function(method) {
            method(at, function(self, k, notFoundThunk) {
                return atPrimitive(buckets, bucketCount, k, notFoundThunk);
            });
            method(putAt, function(self, k, v) {
                return putAtPrimitive(buckets, bucketCount, k, v);
            });
            method(removeAt, function(self, k) {
                return removeAtPrimitive(buckets, bucketCount, k);
            });
            method(each, function(iterator) {
                injectPrimitive(buckets, null, function(tally, k, v) {
                    iterator(k, v);
                });
            });
        });
    }
    function HashSet(list) {
        var buckets = [];
        var bucketCount = 5000;
        var present = new Object;
        if (list) {
            each(list, function(k) {
                putAtPrimitive(buckets, bucketCount, k, present);
            });
        }
        return object(function(method) {
            method(append, function(self, k) {
                putAtPrimitive(buckets, bucketCount, k, present);
            });
            method(each, function(self, iterator) {
                injectPrimitive(buckets, null, function(t, k, v) {
                    iterator(k);
                });
            });
            method(contains, function(self, k) {
                return !!atPrimitive(buckets, bucketCount, k);
            });
            method(complement, function(self, other) {
                var result = [];
                var c;
                try {
                    var othersInternalBuckets = internalBuckets(other);
                    var othersInternalBucketCount = internalBucketCount(other);
                    c = function(items, k) {
                        return !!atPrimitive(othersInternalBuckets, othersInternalBucketCount, k);
                    };
                } catch (e) {
                    c = contains;
                }
                return injectPrimitive(buckets, result, function(tally, k, v) {
                    if (!c(other, k)) {
                        result.push(k);
                    }
                    return tally;
                });
            });
            method(asString, function(self) {
                return 'HashSet[' + join(injectPrimitive(buckets, [], function(tally, k, v) {
                    tally.push(k);
                    return tally;
                }), ',') + ']';
            });
            method(internalBuckets, function(self) {
                return buckets;
            });
            method(internalBucketCount, function(self) {
                return bucketCount;
            });
        });
    }
    define('at', at);
    define('putAt', putAt);
    define('removeAt', removeAt);
    define('HashTable', HashTable);
    define('HashSet', HashSet);
});
ice.lib.logger = ice.module(function(exportAs) {
    eval(ice.importFrom('ice.lib.functional'));
    eval(ice.importFrom('ice.lib.oo'));
    eval(ice.importFrom('ice.lib.collection'));
    eval(ice.importFrom('ice.lib.window'));
    var debug = operator();
    var info = operator();
    var warn = operator();
    var error = operator();
    var childLogger = operator();
    var log = operator();
    var threshold = operator();
    var enable = operator();
    var disable = operator();
    var toggle = operator();
    function Logger(category, handler) {
        return object(function(method) {
            each([debug, info, warn, error], function(priorityOperator) {
                method(priorityOperator, function(self, message, exception) {
                    log(handler, priorityOperator, category, message, exception);
                });
            });
            method(childLogger, function(self, categoryName, newHandler) {
                return Logger(append(copy(category), categoryName), newHandler || handler);
            });
            method(asString, function(self) {
                return 'Logger[' + join(category, '.') + ']';
            });
        });
    }
    function ConsoleLogHandler(priority) {
        function formatOutput(category, message) {
            return join(['[', join(category, '.'), '] ', message], '');
        }
        var ieConsole = !window.console.debug;
        var debugPrimitive = ieConsole ?
            function(self, category, message, exception) {
                exception ? console.log(formatOutput(category, message), '\n', exception) : console.log(formatOutput(category, message));
            } :
            function(self, category, message, exception) {
                exception ? console.debug(formatOutput(category, message), exception) : console.debug(formatOutput(category, message));
            };
        var infoPrimitive = ieConsole ?
            function(self, category, message, exception) {
                exception ? console.info(formatOutput(category, message), '\n', exception) : console.info(formatOutput(category, message));
            } :
            function(self, category, message, exception) {
                exception ? console.info(formatOutput(category, message), exception) : console.info(formatOutput(category, message));
            };
        var warnPrimitive = ieConsole ?
            function(self, category, message, exception) {
                exception ? console.warn(formatOutput(category, message), '\n', exception) : console.warn(formatOutput(category, message));
            } :
            function(self, category, message, exception) {
                exception ? console.warn(formatOutput(category, message), exception) : console.warn(formatOutput(category, message));
            };
        var errorPrimitive = ieConsole ?
            function(self, category, message, exception) {
                exception ? console.error(formatOutput(category, message), '\n', exception) : console.error(formatOutput(category, message));
            } :
            function(self, category, message, exception) {
                exception ? console.error(formatOutput(category, message), exception) : console.error(formatOutput(category, message));
            };
        var handlers = [
            Cell(debug, object(function(method) {
                method(debug, debugPrimitive);
                method(info, infoPrimitive);
                method(warn, warnPrimitive);
                method(error, errorPrimitive);
            })),
            Cell(info, object(function(method) {
                method(debug, noop);
                method(info, infoPrimitive);
                method(warn, warnPrimitive);
                method(error, errorPrimitive);
            })),
            Cell(warn, object(function(method) {
                method(debug, noop);
                method(info, noop);
                method(warn, warnPrimitive);
                method(error, errorPrimitive);
            })),
            Cell(error, object(function(method) {
                method(debug, noop);
                method(info, noop);
                method(warn, noop);
                method(error, errorPrimitive);
            }))
        ];
        var handler;
        function selectHandler(p) {
            handler = value(detect(handlers, function(cell) {
                return key(cell) == p;
            }));
        }
        selectHandler(priority || debug);
        return object(function (method) {
            method(threshold, function(self, priority) {
                selectHandler(priority);
            });
            method(log, function(self, operation, category, message, exception) {
                operation(handler, category, message, exception);
            });
        });
    }
    var FirebugLogHandler = ConsoleLogHandler;
    function WindowLogHandler(thresholdPriority, name) {
        var lineOptions = [25, 50, 100, 200, 400];
        var numberOfLines = lineOptions[3];
        var categoryMatcher = /.*/;
        var closeOnExit = true;
        var logContainer;
        var logEntry = noop;
        function trimLines() {
            var nodes = logContainer.childNodes;
            var trim = size(nodes) - numberOfLines;
            if (trim > 0) {
                each(copy(nodes), function(node, index) {
                    if (index < trim) logContainer.removeChild(node);
                });
            }
        }
        function trimAllLines() {
            each(copy(logContainer.childNodes), function(node) {
                logContainer.removeChild(node);
            });
        }
        function toggle() {
            var disabled = logEntry == noop;
            logEntry = disabled ? displayEntry : noop;
            return !disabled;
        }
        function displayEntry(priorityName, colorName, category, message, exception) {
            var categoryName = join(category, '.');
            if (categoryMatcher.test(categoryName)) {
                var elementDocument = logContainer.ownerDocument;
                var timestamp = new Date();
                var completeMessage = join(['[', categoryName, '] : ', message, (exception ? join(['\n', exception.name, ' <', exception.message, '>'], '') : '')], '');
                each(split(completeMessage, '\n'), function(line) {
                    if (/(\w+)/.test(line)) {
                        var eventNode = elementDocument.createElement('div');
                        eventNode.style.padding = '3px';
                        eventNode.style.color = colorName;
                        eventNode.setAttribute("title", timestamp + ' | ' + priorityName)
                        logContainer.appendChild(eventNode).appendChild(elementDocument.createTextNode(line));
                    }
                });
                logContainer.scrollTop = logContainer.scrollHeight;
            }
            trimLines();
        }
        function showWindow() {
            var logWindow = window.open('', '_blank', 'scrollbars=1,width=800,height=680');
            try {
                var windowDocument = logWindow.document;
                var documentBody = windowDocument.body;
                each(copy(documentBody.childNodes), function(e) {
                    windowDocument.body.removeChild(e);
                });
                documentBody.appendChild(windowDocument.createTextNode(' Close on exit '));
                var closeOnExitCheckbox = windowDocument.createElement('input');
                closeOnExitCheckbox.style.margin = '2px';
                closeOnExitCheckbox.setAttribute('type', 'checkbox');
                closeOnExitCheckbox.defaultChecked = true;
                closeOnExitCheckbox.checked = true;
                closeOnExitCheckbox.onclick = function() {
                    closeOnExit = closeOnExitCheckbox.checked;
                };
                documentBody.appendChild(closeOnExitCheckbox);
                documentBody.appendChild(windowDocument.createTextNode(' Lines '));
                var lineCountDropDown = windowDocument.createElement('select');
                lineCountDropDown.style.margin = '2px';
                each(lineOptions, function(count, index) {
                    var option = lineCountDropDown.appendChild(windowDocument.createElement('option'));
                    if (numberOfLines == count) lineCountDropDown.selectedIndex = index;
                    option.appendChild(windowDocument.createTextNode(asString(count)));
                });
                documentBody.appendChild(lineCountDropDown);
                documentBody.appendChild(windowDocument.createTextNode(' Category '));
                var categoryInputText = windowDocument.createElement('input');
                categoryInputText.style.margin = '2px';
                categoryInputText.setAttribute('type', 'text');
                categoryInputText.setAttribute('value', categoryMatcher.source);
                categoryInputText.onchange = function() {
                    categoryMatcher = new RegExp(categoryInputText.value);
                };
                documentBody.appendChild(categoryInputText);
                documentBody.appendChild(windowDocument.createTextNode(' Level '));
                var levelDropDown = windowDocument.createElement('select');
                levelDropDown.style.margin = '2px';
                var levels = [Cell('debug', debug), Cell('info', info), Cell('warn', warn), Cell('error', error)];
                each(levels, function(priority, index) {
                    var option = levelDropDown.appendChild(windowDocument.createElement('option'));
                    if (thresholdPriority == value(priority)) levelDropDown.selectedIndex = index;
                    option.appendChild(windowDocument.createTextNode(key(priority)));
                });
                levelDropDown.onchange = function(event) {
                    thresholdPriority = value(levels[levelDropDown.selectedIndex]);
                };
                documentBody.appendChild(levelDropDown);
                var startStopButton = windowDocument.createElement('input');
                startStopButton.style.margin = '2px';
                startStopButton.setAttribute('type', 'button');
                startStopButton.setAttribute('value', 'Stop');
                startStopButton.onclick = function() {
                    startStopButton.setAttribute('value', toggle() ? 'Stop' : 'Start');
                };
                documentBody.appendChild(startStopButton);
                var clearButton = windowDocument.createElement('input');
                clearButton.style.margin = '2px';
                clearButton.setAttribute('type', 'button');
                clearButton.setAttribute('value', 'Clear');
                documentBody.appendChild(clearButton);
                logContainer = documentBody.appendChild(windowDocument.createElement('pre'));
                logContainer.id = 'log-window';
                var logContainerStyle = logContainer.style;
                logContainerStyle.width = '100%';
                logContainerStyle.minHeight = '0';
                logContainerStyle.maxHeight = '550px';
                logContainerStyle.borderWidth = '1px';
                logContainerStyle.borderStyle = 'solid';
                logContainerStyle.borderColor = '#999';
                logContainerStyle.backgroundColor = '#ddd';
                logContainerStyle.overflow = 'scroll';
                lineCountDropDown.onchange = function(event) {
                    numberOfLines = lineOptions[lineCountDropDown.selectedIndex];
                    trimLines();
                };
                clearButton.onclick = trimAllLines;
                onUnload(window, function() {
                    if (closeOnExit) {
                        logEntry = noop;
                        logWindow.close();
                    }
                });
            } catch (e) {
                logWindow.close();
            }
        }
        onKeyUp(document, function(evt) {
            var event = $event(evt, document.documentElement);
            if (keyCode(event) == 84 && isCtrlPressed(event) && isShiftPressed(event)) {
                showWindow();
                logEntry = displayEntry;
            }
        });
        return object(function(method) {
            method(threshold, function(self, priority) {
                thresholdPriority = priority;
            });
            method(log, function(self, operation, category, message, exception) {
                operation(self, category, message, exception);
            });
            method(debug, function(self, category, message, exception) {
                logEntry('debug', '#333', category, message, exception);
            });
            method(info, function(self, category, message, exception) {
                logEntry('info', 'green', category, message, exception);
            });
            method(warn, function(self, category, message, exception) {
                logEntry('warn', 'orange', category, message, exception);
            });
            method(error, function(self, category, message, exception) {
                logEntry('error', 'red', category, message, exception);
            });
        });
    }
    exportAs('debug', debug);
    exportAs('info', info);
    exportAs('warn', warn);
    exportAs('error', error);
    exportAs('childLogger', childLogger);
    exportAs('log', log);
    exportAs('threshold', threshold);
    exportAs('enable', enable);
    exportAs('disable', disable);
    exportAs('toggle', toggle);
    exportAs('Logger', Logger);
    exportAs('ConsoleLogHandler', ConsoleLogHandler);
    exportAs('WindowLogHandler', WindowLogHandler);
});
ice.lib.element = ice.module(function(exportAs) {
    eval(ice.importFrom('ice.lib.string'));
    eval(ice.importFrom('ice.lib.collection'));
    eval(ice.importFrom('ice.lib.query'));
    function identifier(element) {
        return element ? element.id : null;
    }
    function tag(element) {
        return toLowerCase(element.nodeName);
    }
    function property(element, name) {
        return element[name];
    }
    function parents(element) {
        return Stream(function(cellConstructor) {
            function parentStream(e) {
                if (e == null || e == document) return null;
                return function() {
                    return cellConstructor(e, parentStream(e.parentNode));
                };
            }
            return parentStream(element.parentNode);
        });
    }
    function enclosingForm(element) {
        return element.form || detect(parents(element), function(e) {
            return tag(e) == 'form';
        }, function() {
            throw 'cannot find enclosing form';
        });
    }
    function enclosingBridge(element) {
        return property(detect(parents(element), function(e) {
            return property(e, 'bridge') != null;
        }, function() {
            throw 'cannot find enclosing bridge';
        }), 'bridge');
    }
    function serializeElementOn(element, query) {
        var tagName = tag(element);
        switch (tagName) {
            case 'a':
                var name = element.name || element.id;
                if (name) addNameValue(query, name, name);
                break;
            case 'input':
                switch (element.type) {
                    case 'image':
                    case 'submit':
                    case 'button':
                        addNameValue(query, element.name, element.value);
                        break;
                }
                break;
            case 'button':
                if (element.type == 'submit') addNameValue(query, element.name, element.value);
                break;
            default:
        }
    }
    function $elementWithID(id) {
        return document.getElementById(id);
    }
    exportAs('identifier', identifier);
    exportAs('tag', tag);
    exportAs('property', property);
    exportAs('parents', parents);
    exportAs('enclosingForm', enclosingForm);
    exportAs('enclosingBridge', enclosingBridge);
    exportAs('serializeElementOn', serializeElementOn);
    exportAs('$elementWithID', $elementWithID);
});
ice.lib.event = ice.module(function(exportAs) {
    eval(ice.importFrom('ice.lib.functional'));
    eval(ice.importFrom('ice.lib.oo'));
    eval(ice.importFrom('ice.lib.collection'));
    eval(ice.importFrom('ice.lib.query'));
    eval(ice.importFrom('ice.lib.element'));
    var cancel = operator();
    var cancelBubbling = operator();
    var cancelDefaultAction = operator();
    var isKeyEvent = operator();
    var isMouseEvent = operator();
    var capturedBy = operator();
    var triggeredBy = operator();
    var serializeEventOn = operator();
    var type = operator();
    var yes = any;
    var no = none;
    function isIEEvent(event) {
        return event.srcElement;
    }
    function Event(event, capturingElement) {
        return object(function (method) {
            method(cancel, function (self) {
                cancelBubbling(self);
                cancelDefaultAction(self);
            });
            method(isKeyEvent, no);
            method(isMouseEvent, no);
            method(type, function (self) {
                return event.type;
            });
            method(triggeredBy, function (self) {
                return capturingElement;
            });
            method(capturedBy, function (self) {
                return capturingElement;
            });
            method(serializeEventOn, function (self, query) {
                serializeElementOn(capturingElement, query);
                addNameValue(query, 'ice.event.target', identifier(triggeredBy(self)));
                addNameValue(query, 'ice.event.captured', identifier(capturedBy(self)));
                addNameValue(query, 'ice.event.type', 'on' + type(self));
            });
            method(serializeOn, curry(serializeEventOn));
        });
    }
    function IEEvent(event, capturingElement) {
        return objectWithAncestors(function (method) {
            method(triggeredBy, function (self) {
                return event.srcElement ? event.srcElement : null;
            });
            method(cancelBubbling, function (self) {
                event.cancelBubble = true;
            });
            method(cancelDefaultAction, function (self) {
                event.returnValue = false;
            });
            method(asString, function (self) {
                return 'IEEvent[' + type(self) + ']';
            });
        }, Event(event, capturingElement));
    }
    function NetscapeEvent(event, capturingElement) {
        return objectWithAncestors(function (method) {
            method(triggeredBy, function (self) {
                return event.target ? event.target : null;
            });
            method(cancelBubbling, function (self) {
                try {
                    event.stopPropagation();
                } catch (e) {
                }
            });
            method(cancelDefaultAction, function (self) {
                try {
                    event.preventDefault();
                } catch (e) {
                }
            });
            method(asString, function (self) {
                return 'NetscapeEvent[' + type(self) + ']';
            });
        }, Event(event, capturingElement));
    }
    var isAltPressed = operator();
    var isCtrlPressed = operator();
    var isShiftPressed = operator();
    var isMetaPressed = operator();
    var serializeKeyOrMouseEventOn = operator();
    function KeyOrMouseEvent(event) {
        return object(function (method) {
            method(isAltPressed, function (self) {
                return event.altKey;
            });
            method(isCtrlPressed, function (self) {
                return event.ctrlKey;
            });
            method(isShiftPressed, function (self) {
                return event.shiftKey;
            });
            method(isMetaPressed, function (self) {
                return event.metaKey;
            });
            method(serializeKeyOrMouseEventOn, function (self, query) {
                addNameValue(query, 'ice.event.alt', isAltPressed(self));
                addNameValue(query, 'ice.event.ctrl', isCtrlPressed(self));
                addNameValue(query, 'ice.event.shift', isShiftPressed(self));
                addNameValue(query, 'ice.event.meta', isMetaPressed(self));
            });
        });
    }
    var isLeftButton = operator();
    var isRightButton = operator();
    var positionX = operator();
    var positionY = operator();
    var serializeMouseEventOn = operator();
    function MouseEvent(event) {
        return objectWithAncestors(function (method) {
            method(isMouseEvent, yes);
            method(serializeMouseEventOn, function (self, query) {
                serializeKeyOrMouseEventOn(self, query);
                addNameValue(query, 'ice.event.x', positionX(self));
                addNameValue(query, 'ice.event.y', positionY(self));
                addNameValue(query, 'ice.event.left', isLeftButton(self));
                addNameValue(query, 'ice.event.right', isRightButton(self));
            });
        }, KeyOrMouseEvent(event));
    }
    function MouseEventTrait(method) {
        method(serializeOn, function (self, query) {
            serializeEventOn(self, query);
            serializeMouseEventOn(self, query);
        });
    }
    function IEMouseEvent(event, capturingElement) {
        return objectWithAncestors(function (method) {
            MouseEventTrait(method);
            method(positionX, function (self) {
                return event.clientX + (document.documentElement.scrollLeft || document.body.scrollLeft);
            });
            method(positionY, function (self) {
                return event.clientY + (document.documentElement.scrollTop || document.body.scrollTop);
            });
            method(isLeftButton, function (self) {
                return event.button == 1;
            });
            method(isRightButton, function (self) {
                return event.button == 2;
            });
            method(asString, function (self) {
                return 'IEMouseEvent[' + type(self) + ']';
            });
        }, MouseEvent(event), IEEvent(event, capturingElement));
    }
    function NetscapeMouseEvent(event, capturingElement) {
        return objectWithAncestors(function (method) {
            MouseEventTrait(method);
            method(positionX, function (self) {
                return event.pageX;
            });
            method(positionY, function (self) {
                return event.pageY;
            });
            method(isLeftButton, function (self) {
                return event.which == 1;
            });
            method(isRightButton, function (self) {
                return event.which == 2;
            });
            method(asString, function (self) {
                return 'NetscapeMouseEvent[' + type(self) + ']';
            });
        }, MouseEvent(event), NetscapeEvent(event, capturingElement));
    }
    var keyCharacter = operator();
    var keyCode = operator();
    var serializeKeyEventOn = operator();
    function KeyEvent(event) {
        return objectWithAncestors(function (method) {
            method(isKeyEvent, yes);
            method(keyCharacter, function (self) {
                return String.fromCharCode(keyCode(self));
            });
            method(serializeKeyEventOn, function (self, query) {
                serializeKeyOrMouseEventOn(self, query);
                addNameValue(query, 'ice.event.keycode', keyCode(self));
            });
        }, KeyOrMouseEvent(event));
    }
    function KeyEventTrait(method) {
        method(serializeOn, function (self, query) {
            serializeEventOn(self, query);
            serializeKeyEventOn(self, query);
        });
    }
    function IEKeyEvent(event, capturingElement) {
        return objectWithAncestors(function (method) {
            KeyEventTrait(method);
            method(keyCode, function (self) {
                return event.keyCode;
            });
            method(asString, function (self) {
                return 'IEKeyEvent[' + type(self) + ']';
            });
        }, KeyEvent(event), IEEvent(event, capturingElement));
    }
    function NetscapeKeyEvent(event, capturingElement) {
        return objectWithAncestors(function (method) {
            KeyEventTrait(method);
            method(keyCode, function (self) {
                return event.which == 0 ? event.keyCode : event.which;
            });
            method(asString, function (self) {
                return 'NetscapeKeyEvent[' + type(self) + ']';
            });
        }, KeyEvent(event), NetscapeEvent(event, capturingElement));
    }
    function isEnterKey(event) {
        return keyCode(event) == 13;
    }
    function isEscKey(event) {
        return keyCode(event) == 27;
    }
    function UnknownEvent(capturingElement) {
        return objectWithAncestors(function (method) {
            method(cancelBubbling, noop);
            method(cancelDefaultAction, noop);
            method(type, function (self) {
                return 'unknown';
            });
            method(asString, function (self) {
                return 'UnkownEvent[]';
            });
        }, Event(null, capturingElement));
    }
    var MouseListenerNames = [ 'onclick', 'ondblclick', 'onmousedown', 'onmousemove', 'onmouseout', 'onmouseover', 'onmouseup' ];
    var KeyListenerNames = [ 'onkeydown', 'onkeypress', 'onkeyup', 'onhelp' ];
    function $event(e, element) {
        var capturedEvent = e || window.event;
        if (capturedEvent && capturedEvent.type) {
            var eventType = 'on' + capturedEvent.type;
            if (contains(KeyListenerNames, eventType)) {
                return isIEEvent(capturedEvent) ? IEKeyEvent(capturedEvent, element) : NetscapeKeyEvent(capturedEvent, element);
            } else if (contains(MouseListenerNames, eventType)) {
                return isIEEvent(capturedEvent) ? IEMouseEvent(capturedEvent, element) : NetscapeMouseEvent(capturedEvent, element);
            } else {
                return isIEEvent(capturedEvent) ? IEEvent(capturedEvent, element) : NetscapeEvent(capturedEvent, element);
            }
        } else {
            return UnknownEvent(element);
        }
    }
    exportAs('cancel', cancel);
    exportAs('cancelBubbling', cancelBubbling);
    exportAs('cancelDefaultAction', cancelDefaultAction);
    exportAs('isKeyEvent', isKeyEvent);
    exportAs('isMouseEvent', isMouseEvent);
    exportAs('capturedBy', capturedBy);
    exportAs('triggeredBy', triggeredBy);
    exportAs('serializeEventOn', serializeEventOn);
    exportAs('type', type);
    exportAs('isAltPressed', isAltPressed);
    exportAs('isCtrlPressed', isCtrlPressed);
    exportAs('isShiftPressed', isShiftPressed);
    exportAs('isMetaPressed', isMetaPressed);
    exportAs('isLeftButton', isLeftButton);
    exportAs('isRightButton', isRightButton);
    exportAs('positionX', positionX);
    exportAs('positionY', positionY);
    exportAs('keyCharacter', keyCharacter);
    exportAs('keyCode', keyCode);
    exportAs('isEnterKey', isEnterKey);
    exportAs('isEscKey', isEscKey);
    exportAs('$event', $event);
});
