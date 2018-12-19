package org.icepush.util;

import static org.icesoft.util.PreCondition.checkIfIsGreaterThan;
import static org.icesoft.util.PreCondition.checkIfIsNotNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.PushInternalContext;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;

public class DatabaseBackedQueue<E extends DatabaseEntity>
implements Collection<E>, Iterable<E>, Queue<E> {
    private static final Logger LOGGER = Logger.getLogger(DatabaseBackedQueue.class.getName());

    private final Lock putLock = new ReentrantLock();
    private final Lock takeLock = new ReentrantLock();

    private final Datastore datastore;
    private final Class<? extends E> elementClass;
    private final Queue<TimestampedElementContainer<? extends E>> timestampedElementContainerQueue;

    public DatabaseBackedQueue(final Class<? extends E> elementClass, final Datastore datastore)
    throws IllegalArgumentException {
        checkIfIsNotNull(
            elementClass,
            "Illegal argument elementClass: '" + elementClass + "'.  Argument cannot be null."
        );
        checkIfIsNotNull(
            datastore,
            "Illegal argument datastore: '" + datastore + "'.  Argument cannot be null."
        );
        this.timestampedElementContainerQueue =
            new LinkedBlockingQueue<TimestampedElementContainer<? extends E>>();
        this.elementClass = elementClass;
        this.datastore = datastore;
        populate();
    }

    public DatabaseBackedQueue(final int capacity, final Class<? extends E> elementClass, final Datastore datastore)
    throws IllegalArgumentException {
        checkIfIsGreaterThan(
            capacity,
            0,
            "Illegal argument capacity: '" + capacity + "'.  Argument cannot be equal to or lesser than 0."
        );
        checkIfIsNotNull(
            elementClass,
            "Illegal argument elementClass: '" + elementClass + "'.  Argument cannot be null."
        );
        checkIfIsNotNull(
            datastore,
            "Illegal argument datastore: '" + datastore + "'.  Argument cannot be null."
        );
        this.timestampedElementContainerQueue =
            new LinkedBlockingQueue<TimestampedElementContainer<? extends E>>(capacity);
        this.elementClass = elementClass;
        this.datastore = datastore;
        populate();
    }

    public boolean add(final E element)
    throws ClassCastException, IllegalArgumentException, IllegalStateException, NullPointerException {
        checkIfIsNotNull(
            element,
            "Illegal argument element: '" + element + "'.  Argument cannot be null."
        );
        if (!getElementClass().isInstance(element)) {
            throw
                new IllegalArgumentException(
                    "Illegal argument element: '" + element + "'.  " +
                        "Argument is not instance of '" + getElementClass() + "'."
                );
        }
        TimestampedElementContainer<E> _timestampedElementContainer =
            new TimestampedElementContainer<E>(element, element.getClass());
        // throws ClassCastException, IllegalArgumentException, IllegalStateException, NullPointerException
        boolean _modified = getTimestampedElementContainerQueue().add(_timestampedElementContainer);
        if (_modified) {
            getDatastore().save(_timestampedElementContainer);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Added Element '" + element + "' to Queue and saved Entity '" + element + "' to Database."
                );
            }
        }
        return _modified;
    }

    public boolean addAll(final Collection<? extends E> elementCollection)
    throws ClassCastException, IllegalArgumentException, IllegalStateException, NullPointerException {
        boolean _modified = false;
        for (final E _element : elementCollection) {
            // throws ClassCastException, IllegalArgumentException, IllegalStateException, NullPointerException
            _modified |= add(_element);
        }
        return _modified;
    }

    public void clear()
    throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public boolean contains(final Object object) {
        Iterator<? extends E> _elementQueueIterator = iterator();
        // This Queue implementation does not support elements to be null.
        if (object != null) {
            while (_elementQueueIterator.hasNext()) {
                if (object.equals(_elementQueueIterator.next())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean containsAll(final Collection<?> objectCollection)
    throws ClassCastException, NullPointerException {
        // throws ClassCastException, NullPointerException
        return getTimestampedElementContainerQueue().containsAll(objectCollection);
    }

    public E element()
    throws NoSuchElementException {
        return getTimestampedElementContainerQueue().element().getElement();
    }

    public boolean isEmpty() {
        return getTimestampedElementContainerQueue().isEmpty();
    }

    public Iterator<E> iterator() {
        return
            new Iterator<E>() {
                private Iterator<TimestampedElementContainer<? extends E>> delegateIterator =
                    getTimestampedElementContainerQueue().iterator();

                public boolean hasNext() {
                    return getDelegateIterator().hasNext();
                }

                public E next()
                throws NoSuchElementException {
                    return getDelegateIterator().next().getElement();
                }

                public void remove()
                throws IllegalStateException {
                    getDelegateIterator().remove();
                }

                protected Iterator<TimestampedElementContainer<? extends E>> getDelegateIterator() {
                    return delegateIterator;
                }
            };
    }

    public boolean offer(final E element)
    throws ClassCastException, IllegalArgumentException, NullPointerException {
        checkIfIsNotNull(
            element,
            "Illegal argument element: '" + element + "'.  Argument cannot be null."
        );
        if (!getElementClass().isInstance(element)) {
            throw
                new IllegalArgumentException(
                    "Illegal argument element: '" + element + "'.  " +
                        "Argument is not instance of '" + getElementClass() + "'."
                );
        }
        getPutLock().lock();
        try {
            TimestampedElementContainer<E> _timestampedElementContainer =
                new TimestampedElementContainer<E>(element, element.getClass());
            // throws ClassCastException, IllegalArgumentException, NullPointerException
            boolean _modified = getTimestampedElementContainerQueue().offer(_timestampedElementContainer);
            if (_modified) {
                getDatastore().save(_timestampedElementContainer);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Added Element '" + element + "' to Queue and saved Entity '" + element + "' to Database."
                    );
                }
            }
            return _modified;
        } finally {
            getPutLock().unlock();
        }
    }

    public E peek() {
        getTakeLock().lock();
        try {
            return getTimestampedElementContainerQueue().peek().getElement();
        } finally {
            getTakeLock().unlock();
        }
    }

    public E poll() {
        getTakeLock().lock();
        try {
            TimestampedElementContainer<? extends E> _timestampedElementContainer =
                getTimestampedElementContainerQueue().poll();
            if (_timestampedElementContainer != null) {
                getDatastore().
                    findAndDelete(
                        getDatastore().
                            createQuery(TimestampedElementContainer.class).
                            field("_id").
                            equal(_timestampedElementContainer.getDatabaseID())
                    );
                E _deletedEntity =
                    getDatastore().
                        findAndDelete(
                            getDatastore().
                                createQuery(getElementClass()).
                                field("_id").
                                equal(_timestampedElementContainer.getElement().getDatabaseID())
                        );
                if (_deletedEntity != null) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(
                            Level.FINE,
                            "Polled Element '" + _timestampedElementContainer.getElement() + "' from Queue and " +
                                "deleted Entity '" + _deletedEntity + "' from Database."
                        );
                    }
                } else {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(
                            Level.FINE,
                            "Polled Element '" + _timestampedElementContainer.getElement() + "' from Queue."
                        );
                    }
                }
                return _timestampedElementContainer.getElement();
            } else {
                return null;
            }
        } finally {
            getTakeLock().unlock();
        }
    }

    public E remove()
    throws NoSuchElementException {
        // throws NoSuchElementException
        TimestampedElementContainer<? extends E> _timestampedElementContainer =
            getTimestampedElementContainerQueue().remove();
        if (_timestampedElementContainer != null) {
            getDatastore().
                findAndDelete(
                    getDatastore().
                        createQuery(TimestampedElementContainer.class).
                        field("_id").
                        equal(_timestampedElementContainer.getDatabaseID())
                );
            E _deletedEntity =
                getDatastore().
                    findAndDelete(
                        getDatastore().
                            createQuery(getElementClass()).
                            field("_id").
                            equal(_timestampedElementContainer.getElement().getDatabaseID())
                    );
            if (_deletedEntity != null) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Removed Element '" + _timestampedElementContainer.getElement() + "' from Queue and " +
                            "deleted Entity '" + _deletedEntity + "' from Database."
                    );
                }
            } else {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Removed Element '" + _timestampedElementContainer.getElement() + "' from Queue."
                    );
                }
            }
            return _timestampedElementContainer.getElement();
        } else {
            return null;
        }
    }

    public boolean remove(final Object object) {
        Iterator<? extends E> _elementQueueIterator = iterator();
        // This Queue implementation does not support elements to be null.
        if (object != null) {
            while (_elementQueueIterator.hasNext()) {
                E _element = _elementQueueIterator.next();
                if (object.equals(_element)) {
                    _elementQueueIterator.remove();
                    getDatastore().
                        findAndDelete(
                            getDatastore().
                                createQuery(TimestampedElementContainer.class).
                                field("_id").
                                equal(_element.getDatabaseID())
                        );
                    E _deletedEntity =
                        getDatastore().
                            findAndDelete(
                                getDatastore().
                                    createQuery(getElementClass()).
                                    field("_id").
                                    equal(_element.getDatabaseID())
                            );
                    if (_deletedEntity != null) {
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.log(
                                Level.FINE,
                                "Removed Element '" + _element + "' from Queue and " +
                                    "deleted Entity '" + _deletedEntity + "' from Database."
                            );
                        }
                    } else {
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.log(
                                Level.FINE,
                                "Removed Element '" + _element + "' from Queue."
                            );
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public boolean removeAll(final Collection<?> objectCollection)
    throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection<?> objects)
    throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public int size() {
        return getTimestampedElementContainerQueue().size();
    }

    public Object[] toArray()
    throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public <T> T[] toArray(T[] array) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        StringBuilder _stringBuilder = new StringBuilder();
        _stringBuilder.append("DatabaseBackedQueue[");
        boolean _first = true;
        for (final TimestampedElementContainer<? extends E> _timestampedElementContainer :
                getTimestampedElementContainerQueue()) {

            if (!_first) {
                _stringBuilder.append(", ");
            } else {
                _first = false;
            }
            _stringBuilder.
                append("{").
                    append("element: '").append(_timestampedElementContainer.getElement()).append("'").
                append("}");
        }
        _stringBuilder.append("]");
        return _stringBuilder.toString();
    }

    protected final Datastore getDatastore() {
        return datastore;
    }

    protected final Class<? extends E> getElementClass() {
        return elementClass;
    }

    protected final Lock getPutLock() {
        return putLock;
    }

    protected final Queue<TimestampedElementContainer<? extends E>> getTimestampedElementContainerQueue() {
        return timestampedElementContainerQueue;
    }

    protected final Lock getTakeLock() {
        return takeLock;
    }

    private void populate() {
        List<TimestampedElementContainer> _timestampedElementContainerList =
            getDatastore().find(TimestampedElementContainer.class).asList();
        TreeSet<TimestampedElementContainer<E>> _orderedTimestampedElementContainerSet =
            new TreeSet<TimestampedElementContainer<E>>();
        for (final TimestampedElementContainer _timestampedElementContainer : _timestampedElementContainerList) {
            if (getElementClass().isInstance(_timestampedElementContainer.getElement())) {
                _orderedTimestampedElementContainerSet.add(_timestampedElementContainer);
            }
        }
        for (final TimestampedElementContainer<E> _timestampedElementContainer : _orderedTimestampedElementContainerSet) {
            getTimestampedElementContainerQueue().add(_timestampedElementContainer);
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                Level.FINE,
                "Database-backed Map for Class '" + getElementClass() + "' " +
                    "populated with '" + getTimestampedElementContainerQueue() + "'."
            );
        }
    }

    @Entity(value = "timestamped_element_containers")
    public static class TimestampedElementContainer<E extends DatabaseEntity>
    implements Comparable<TimestampedElementContainer<E>>, DatabaseEntity, Serializable {
        @Id
        private String databaseID;

        @Reference
        private E element;

        private Class elementClass;

        private long timestamp;

        public TimestampedElementContainer() {
            // Do nothing.
        }

        protected TimestampedElementContainer(final E element, final Class elementClass) {
            this.element = element;
            this.elementClass = elementClass;
            this.timestamp = System.currentTimeMillis();
            this.databaseID = this.element.getDatabaseID();
        }

        public int compareTo(final TimestampedElementContainer<E> timestampedElementContainer) {
            return getTimestamp().compareTo(timestampedElementContainer.getTimestamp());
        }

        @Override
        public boolean equals(final Object object) {
            return
                (
                    object instanceof TimestampedElementContainer &&
                        ((TimestampedElementContainer)object).getElement().equals(getElement())
                ) ||
                (getElement().equals(object));
        }

        public String getDatabaseID() {
            return databaseID;
        }

        public String getKey() {
            return getDatabaseID();
        }

        @Override
        public int hashCode() {
            return getElement().hashCode();
        }

        public void save() {
//            Datastore _datastore =
//                ((Datastore)PushInternalContext.getInstance().getAttribute(Datastore.class.getName()));
//            if (_datastore != null) {
//                _datastore.save(this);
//            }
        }

        @Override
        public String toString() {
            return
                new StringBuilder().
                    append("TimestampedElementContainer[").
                        append(classMembersToString()).
                    append("]").
                        toString();
        }

        protected String classMembersToString() {
            return
                new StringBuilder().
                    append("element: '").append(getElement()).append("', ").
                    append("elementClass: '").append(getElementClass()).append("', ").
                    append("timestamp: '").append(getTimestamp()).append("'").
                        toString();
        }

        protected E getElement() {
            return element;
        }

        protected Class getElementClass() {
            return elementClass;
        }

        protected Long getTimestamp() {
            return timestamp;
        }
    }
}
