package org.icepush.util;

import static org.icesoft.util.ObjectUtilities.isNotNull;
import static org.icesoft.util.PreCondition.checkArgument;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mongodb.morphia.Datastore;

public class DatabaseBackedConcurrentMap<V extends DatabaseEntity>
implements ConcurrentMap<String, V>, Map<String, V> {
    private static final Logger LOGGER = Logger.getLogger(DatabaseBackedConcurrentMap.class.getName());

    private final ConcurrentMap<String, V> map = new ConcurrentHashMap<String, V>();

    private final Datastore datastore;
    private final Class<? extends V> valueClass;

    public DatabaseBackedConcurrentMap(final Class<? extends V> valueClass, final Datastore datastore)
    throws IllegalArgumentException {
        checkArgument(
            isNotNull(valueClass),
            "Illegal argument elementClass: '" + valueClass + "'.  Argument cannot be null."
        );
        checkArgument(
            isNotNull(datastore),
            "Illegal argument datastore: '" + datastore + "'.  Argument cannot be null."
        );
        this.valueClass = valueClass;
        this.datastore = datastore;
        populate();
    }

    public void clear()
    throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public boolean containsKey(final Object object) {
        return getMap().containsKey(object);
    }

    public boolean containsValue(final Object object)
    throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public Set<Entry<String, V>> entrySet() {
        return getMap().entrySet();
    }

    public V get(final Object object) {
        return getMap().get(object);
    }

    public boolean isEmpty()
    throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public Set<String> keySet()
    throws UnsupportedOperationException {
        return getMap().keySet();
    }

    public V put(final String key, final V value) {
        V _previousValue = getMap().put(key, value);
        getDatastore().save(value);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                Level.FINE,
                "Put Value '" + value + "' in Map and saved Entity '" + value + "' to Database."
            );
        }
        return _previousValue;
    }

    public void putAll(final Map<? extends String, ? extends V> map)
    throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public V putIfAbsent(final String key, final V value) {
        V _previousValue = getMap().putIfAbsent(key, value);
        getDatastore().save(value);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                Level.FINE,
                "Put (if absent) Value '" + value + "' to Map and saved Entity '" + value + "' to Database."
            );
        }
        return _previousValue;
    }

    public V remove(final Object object) {
        V _previousValue = getMap().remove(object);
        if (_previousValue != null) {
            V _deletedEntity =
                getDatastore().
                    findAndDelete(
                        getDatastore().createQuery(getValueClass()).field("_id").equal(_previousValue.getDatabaseID())
                    );
            if (_deletedEntity != null) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Removed Value '" + _previousValue + "' from Map and " +
                            "deleted Entity '" + _deletedEntity + "' from Database."
                    );
                }
            } else {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Removed Value '" + _previousValue + "' from Map."
                    );
                }
            }
        }
        return _previousValue;
    }

    public boolean remove(final Object key, final Object value) {
        boolean _removed = getMap().remove(key, value);
        if (_removed) {
            V _deletedEntity =
                getDatastore().
                    findAndDelete(
                        getDatastore().createQuery(getValueClass()).field("_id").equal(((V)value).getDatabaseID())
                    );
            if (_deletedEntity != null) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Removed Value '" + value + "' from Map and " +
                            "deleted Entity '" + _deletedEntity + "' from Database."
                    );
                }
            } else {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Removed Value '" + value + "' from Map."
                    );
                }
            }
        }
        return _removed;
    }

    public V replace(final String key, final V value) {
        V _previousValue = getMap().replace(key, value);
        if (_previousValue != null) {
            V _deletedEntity =
                getDatastore().
                    findAndDelete(
                        getDatastore().createQuery(getValueClass()).field("_id").equal(_previousValue.getDatabaseID())
                    );
            getDatastore().save(value);
            if (_deletedEntity != null) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Replaced Value '" + _previousValue + "' with Value '" + value + "' in Map and " +
                            "saved Entity '" + value + "' to and " +
                            "deleted Entity '" + _deletedEntity + "' from Database."
                    );
                }
            } else {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Replaced Value '" + _previousValue + "' with Value '" + value + "' in Map."
                    );
                }
            }
        } else {
            getDatastore().save(value);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Replaced Value '" + _previousValue + "' with Value '" + value + "' in Map and " +
                        "saved Entity '" + value + "' to Database."
                );
            }
        }
        return _previousValue;
    }

    public boolean replace(final String key, final V oldValue, final V newValue) {
        boolean _replaced = getMap().replace(key, oldValue, newValue);
        if (_replaced) {
            V _deletedEntity =
                getDatastore().
                    findAndDelete(
                        getDatastore().createQuery(getValueClass()).field("_id").equal(oldValue.getDatabaseID())
                    );
            getDatastore().save(newValue);
            if (_deletedEntity != null) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Replaced Value '" + oldValue + "' with Value '" + newValue + "' in Map and " +
                            "deleted Entity '" + _deletedEntity + "' from " +
                            "saved Entity '" + newValue + "' to Database."
                    );
                }
            } else {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Replaced Value '" + oldValue + "' with Value '" + newValue + "' in Map."
                    );
                }
            }
        }
        return _replaced;
    }

    public int size() {
        return getMap().size();
    }

    @Override
    public String toString() {
        StringBuilder _stringBuilder = new StringBuilder();
        _stringBuilder.append("DatabaseBackedConcurrentMap[");
        boolean _first = true;
        for (final Entry<String, V> _entry : getMap().entrySet()) {
            if (!_first) {
                _stringBuilder.append(", ");
            } else {
                _first = false;
            }
            _stringBuilder.
                append("{").
                    append("key: '").append(_entry.getKey()).append("', ").
                    append("value: '").append(_entry.getValue()).append("'").
                append("}");
        }
        _stringBuilder.append("]");
        return _stringBuilder.toString();
    }

    public Collection<V> values() {
        return getMap().values();
    }

    protected final Datastore getDatastore() {
        return datastore;
    }

    protected final ConcurrentMap<String, V> getMap() {
        return map;
    }

    protected final Class<? extends V> getValueClass() {
        return valueClass;
    }

    private void populate() {
        List<? extends V> _valueList = getDatastore().find(getValueClass()).asList();
        for (final V _value : _valueList) {
            getMap().putIfAbsent(_value.getKey(), _value);
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                Level.FINE,
                "Database-backed Map for Class '" + getValueClass() + "' " +
                    "populated with '" + entrySet() + "'."
            );
        }
    }
}
