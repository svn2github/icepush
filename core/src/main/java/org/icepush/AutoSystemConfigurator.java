package org.icepush;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.icepush.util.AnnotationScanner;

@WebListener
public class AutoSystemConfigurator
implements ServletContextListener {
    private static final Logger LOGGER = Logger.getLogger(AutoSystemConfigurator.class.getName());

    public void contextDestroyed(final ServletContextEvent event) {
        // Do nothing.
    }

    public void contextInitialized(final ServletContextEvent event) {
        ServletContext _servletContext = event.getServletContext();
        Set<String> _annotationSet = new HashSet<String>();
        _annotationSet.add("Lorg/icepush/ExtendedConfigurator;");
        AnnotationScanner _annotationScanner = new AnnotationScanner(_annotationSet, _servletContext);
        try {
            Set<Class> _classSet = _annotationScanner.getClassSet();
            _classSet.remove(AutoSystemConfigurator.class);
            for (final Class _class : _classSet) {
                Configurator _autoConfiguration = null;
                try {
                    _autoConfiguration = (Configurator)_class.getConstructor().newInstance();
                } catch (final NoSuchMethodException exception) {
                    LOGGER.log(Level.FINE, "Can't get constructor!", exception);
                    // Do nothing.
                } catch (final SecurityException exception) {
                    LOGGER.log(Level.FINE, "Can't get constructor!", exception);
                    // Do nothing.
                } catch (final IllegalAccessException exception) {
                    LOGGER.log(Level.FINE, "Can't create instance!", exception);
                    // Do nothing.
                } catch (final IllegalArgumentException exception) {
                    LOGGER.log(Level.FINE, "Can't create instance!", exception);
                    // Do nothing.
                } catch (final InstantiationException exception) {
                    LOGGER.log(Level.FINE, "Can't create instance!", exception);
                    // Do nothing.
                } catch (final InvocationTargetException exception) {
                    LOGGER.log(Level.FINE, "Can't create instance!", exception);
                    // Do nothing.
                } catch (final ExceptionInInitializerError error) {
                    LOGGER.log(Level.FINE, "Can't create instance!", error);
                    // Do nothing.
                }
                if (_autoConfiguration != null) {
                    _autoConfiguration.configure();
                }
            }
        } catch (final IOException exception) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "An I/O error occurred while trying to get the classes.", exception);
            }
            // Do nothing.
        }
    }
}
