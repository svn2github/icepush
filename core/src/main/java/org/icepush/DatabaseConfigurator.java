package org.icepush;

import static org.icesoft.util.StringUtilities.isNotNullAndIsNotEmpty;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.icesoft.util.Configuration;
import org.icesoft.util.SystemConfiguration;
import org.icesoft.util.servlet.ExtensionRegistry;
import org.icesoft.util.servlet.ServletContextConfiguration;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

public class DatabaseConfigurator
implements ServletContextListener {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConfigurator.class.getName());

    private static final class Property {
        private static final class Name {
            private static final String DB_ENABLED = "org.icepush.dbEnabled";
            private static final String DB_DEBUG_ENABLED = "org.icepush.dbDebugEnabled";
            private static final String DB_HOST = "org.icepush.dbHost";
            private static final String DB_NAME = "org.icepush.dbName";
            private static final String DB_PORT = "org.icepush.dbPort";
            private static final String DB_TRACE_ENABLED = "org.icepush.dbTraceEnabled";
            private static final String DB_URI = "org.icepush.dbURI";
        }
        private static final class DefaultValue {
            private static final boolean DB_ENABLED = false;
            private static final boolean DB_DEBUG_ENABLED = false;
            private static final String DB_HOST = "localhost";
            private static final String DB_NAME = "icesoft_technologies";
            private static final int DB_PORT = 27017;
            private static final boolean DB_TRACE_ENABLED = false;
            private static final String DB_URI = null;
        }
    }

    private Configuration configuration;

    public void contextDestroyed(final ServletContextEvent event) {
        if (isDBEnabled()) {
            String _dbHost = getDBHost();
            int _dbPort = getDBPort();
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Trying to disconnect from database at '" + _dbHost + ":" + _dbPort + "'."
                );
            }
            getMongoClient().close();
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(
                    Level.INFO,
                    "Disconnected from database at '" + _dbHost + ":" + _dbPort + "'."
                );
            }
        }
    }

    public void contextInitialized(final ServletContextEvent event) {
        ServletContext _servletContext = event.getServletContext();
        try {
            for (final Constructor<?> _constructor :
                    Class.forName("com.icesoft.notify.util.aws.S3Configuration").getDeclaredConstructors()) {

                LOGGER.info("[Jack] Constructor: " + _constructor);
                for (final Type _genericParameterType : _constructor.getGenericParameterTypes()) {
                    LOGGER.info("[Jack] Generic Parameter Type: " + _genericParameterType);
                }
                for (final Class<?> _parameterType : _constructor.getParameterTypes()) {
                    LOGGER.info("[Jack] Parameter Type: " + _parameterType);
                }
            }
            Class<?>[] _parameterTypes = new Class<?>[4];
            _parameterTypes[0] = String.class;
            _parameterTypes[1] = String.class;
            _parameterTypes[2] = Set.class;
            _parameterTypes[3] = Configuration.class;
            Object[] _arguments = new Object[4];
            _arguments[0] =
                System.getenv("ENVIRONMENT_NAME").equals("local") ?
                    "http://aws:4572" : null;
            _arguments[1] =
                System.getenv("SECRETS_BUCKET_NAME");
            _arguments[2] =
                new HashSet<String>(
                    Arrays.asList(
                        "common.config"
                    )
                );
            _arguments[3] =
                new SystemConfiguration(
                    new ServletContextConfiguration(
                        _servletContext
                    )
                );
            setConfiguration(
                (Configuration)
                    Class.forName("com.icesoft.notify.util.aws.S3Configuration").
                        getDeclaredConstructor(_parameterTypes).
                        newInstance(_arguments)
            );
        } catch (final ClassNotFoundException exception) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(
                    Level.WARNING,
                    "Class 'S3Configuration' not found."
                );
            }
            setConfiguration(
                new SystemConfiguration(
                    new ServletContextConfiguration(
                        _servletContext
                    )
                )
            );
        } catch (final ExceptionInInitializerError error) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(
                    Level.WARNING,
                    "Constructor 'S3Configuration(String, String, Set<String>, Configuration)' failed.  " +
                        "(Message: '" + error.getException().getMessage() + "')",
                    error.getException()
                );
            }
            setConfiguration(
                new SystemConfiguration(
                    new ServletContextConfiguration(
                        _servletContext
                    )
                )
            );
        } catch (final IllegalAccessException exception) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(
                    Level.WARNING,
                    "Constructor 'S3Configuration(String, String, Set<String>, Configuration)' is inaccessible."
                );
            }
            setConfiguration(
                new SystemConfiguration(
                    new ServletContextConfiguration(
                        _servletContext
                    )
                )
            );
        } catch (final InstantiationException exception) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(
                    Level.WARNING,
                    "Class 'S3Configuration' is an abstract class."
                );
            }
            setConfiguration(
                new SystemConfiguration(
                    new ServletContextConfiguration(
                        _servletContext
                    )
                )
            );
        } catch (final InvocationTargetException exception) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(
                    Level.WARNING,
                    "Constructor 'S3Configuration(String, String, Set<String>, Configuration)' threw an exception.  " +
                        "(Message: '" + exception.getTargetException().getMessage() + "')",
                    exception.getTargetException()
                );
            }
            setConfiguration(
                new SystemConfiguration(
                    new ServletContextConfiguration(
                        _servletContext
                    )
                )
            );
        } catch (final NoSuchMethodException exception) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(
                    Level.WARNING,
                    "Constructor 'S3Configuration(String, String, Set<String>, Configuration)' not found."
                );
            }
            setConfiguration(
                new SystemConfiguration(
                    new ServletContextConfiguration(
                        _servletContext
                    )
                )
            );
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                Level.FINE,
                "\r\n" +
                "\r\n" +
                "Database configuration: \r\n" +
                "-    " + Property.Name.DB_ENABLED + " = " +
                    isDBEnabled() + (isDBEnabledDefault() ? " [default]" : "") + "\r\n" +
                "-    " + Property.Name.DB_DEBUG_ENABLED + " = " +
                    isDBDebugEnabled() + (isDBDebugEnabledDefault() ? " [default]" : "") + "\r\n" +
                "-    " + Property.Name.DB_TRACE_ENABLED + " = " +
                    isDBTraceEnabled() + (isDBTraceEnabledDefault() ? " [default]" : "") + "\r\n" +
                "-    " + Property.Name.DB_HOST + " = " +
                    getDBHost() + (isDBHostDefault() ? " [default]" : "") + "\r\n" +
                "-    " + Property.Name.DB_PORT + " = " +
                    getDBPort() + (isDBPortDefault() ? " [default]" : "") + "\r\n" +
                "-    " + Property.Name.DB_NAME + " = " +
                    getDBName() + (isDBNameDefault() ? " [default]" : "") + "\r\n" +
                "-    " + Property.Name.DB_URI + " = " +
                    getDBURI() + (isDBURIDefault()? " [default]" : "") + "\r\n"
            );
        }
        if (isDBEnabled()) {
            System.setProperty("DEBUG.MONGO", Boolean.toString(isDBDebugEnabled()));
            System.setProperty("DB.TRACE", Boolean.toString(isDBTraceEnabled()));
            if (isNotNullAndIsNotEmpty(getDBURI())) {
                MongoClientURI _mongoClientURI = new MongoClientURI(getDBURI());
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Trying to connect to database at '" + _mongoClientURI.getHosts() + "'."
                    );
                }
                setMongoClient(new MongoClient(_mongoClientURI));
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.log(
                        Level.INFO,
                        "Connected to database at '" + _mongoClientURI.getHosts() + "'."
                    );
                }
            } else {
                String _dbHost = getDBHost();
                int _dbPort = getDBPort();
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "Trying to connect to database at '" + _dbHost + ":" + _dbPort + "'."
                    );
                }
                setMongoClient(new MongoClient(_dbHost, _dbPort));
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.log(
                        Level.INFO,
                        "Connected to database at '" + _dbHost + ":" + _dbPort + "'."
                    );
                }
            }
            setDatastore(new Morphia().createDatastore(getMongoClient(), getDBName()));
            try {
                PushInternalContext.getInstance().setAttribute(
                    PushGroupManager.class.getName(),
                    ((Class)ExtensionRegistry.getBestExtension(PushGroupManager.class.getName(), _servletContext)).
                        getMethod("getInstance", new Class[]{ServletContext.class}).invoke(null, _servletContext)
                );
            } catch (final NoSuchMethodException exception) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING, "Unable to get instance of Push Group Manager.", exception);
                }
            } catch (final IllegalAccessException exception) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING, "Unable to get instance of Push Group Manager.", exception);
                }
            } catch (final InvocationTargetException exception) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING, "Unable to get instance of Push Group Manager.", exception);
                }
            }
        }
    }

    protected final Configuration getConfiguration() {
        return configuration;
    }

    protected final Datastore getDatastore() {
        return (Datastore)PushInternalContext.getInstance().getAttribute(Datastore.class.getName());
    }

    protected final String getDBHost() {
        return
            getConfiguration().
                getAttribute(Property.Name.DB_HOST, Property.DefaultValue.DB_HOST).
                    trim();
    }

    protected final String getDBName() {
        return
            getConfiguration().
                getAttribute(Property.Name.DB_NAME, Property.DefaultValue.DB_NAME);
    }

    protected final int getDBPort() {
        return
            getConfiguration().
                getAttributeAsInteger(Property.Name.DB_PORT, Property.DefaultValue.DB_PORT);
    }

    protected final String getDBURI() {
        return
            getConfiguration().
                getAttribute(Property.Name.DB_URI, Property.DefaultValue.DB_URI).
                    trim();
    }

    protected final MongoClient getMongoClient() {
        return (MongoClient)PushInternalContext.getInstance().getAttribute(MongoClient.class.getName());
    }

    protected final boolean isDBEnabled() {
        return
            getConfiguration().
                getAttributeAsBoolean(Property.Name.DB_ENABLED, Property.DefaultValue.DB_ENABLED);
    }

    protected final boolean isDBEnabledDefault() {
        return isDBEnabled() == Property.DefaultValue.DB_ENABLED;
    }

    protected final boolean isDBDebugEnabled() {
        return
            getConfiguration().
                getAttributeAsBoolean(Property.Name.DB_DEBUG_ENABLED, Property.DefaultValue.DB_DEBUG_ENABLED);
    }

    protected final boolean isDBDebugEnabledDefault() {
        return isDBDebugEnabled() == Property.DefaultValue.DB_DEBUG_ENABLED;
    }

    protected final boolean isDBHostDefault() {
        return getDBHost().equals(Property.DefaultValue.DB_HOST);
    }

    protected final boolean isDBNameDefault() {
        return getDBName().equals(Property.DefaultValue.DB_NAME);
    }

    protected final boolean isDBPortDefault() {
        return getDBPort() == Property.DefaultValue.DB_PORT;
    }

    protected final boolean isDBTraceEnabled() {
        return
            getConfiguration().
                getAttributeAsBoolean(Property.Name.DB_TRACE_ENABLED, Property.DefaultValue.DB_TRACE_ENABLED);
    }

    protected final boolean isDBTraceEnabledDefault() {
        return isDBTraceEnabled() == Property.DefaultValue.DB_TRACE_ENABLED;
    }

    protected final boolean isDBURIDefault() {
        return getDBURI() == Property.DefaultValue.DB_URI;
    }

    private void setConfiguration(final Configuration configuration) {
        this.configuration = configuration;
    }

    private void setDatastore(final Datastore datastore) {
        if (datastore != null) {
            PushInternalContext.getInstance().setAttribute(Datastore.class.getName(), datastore);
        } else {
            PushInternalContext.getInstance().removeAttribute(Datastore.class.getName());
        }
    }

    private void setMongoClient(final MongoClient mongoClient) {
        if (mongoClient != null) {
            PushInternalContext.getInstance().setAttribute(MongoClient.class.getName(), mongoClient);
        } else {
            PushInternalContext.getInstance().removeAttribute(MongoClient.class.getName());
        }
    }
}
