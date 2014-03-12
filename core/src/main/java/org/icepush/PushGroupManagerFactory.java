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
 *
 */
package org.icepush;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.icepush.util.AnnotationScanner;

public class PushGroupManagerFactory {
    private static final Logger LOGGER = Logger.getLogger(PushGroupManagerFactory.class.getName());

    public static PushGroupManager newPushGroupManager(
        final ServletContext servletContext, final Configuration configuration) {

        return newPushGroupManager(servletContext, null, configuration);
    }

    public static PushGroupManager newPushGroupManager(
        final ServletContext servletContext, final ScheduledThreadPoolExecutor executor,
        final Configuration configuration) {

        String _pushGroupManagerClassName = (String)servletContext.getAttribute("org.icepush.PushGroupManager");
        if (_pushGroupManagerClassName == null) {
            _pushGroupManagerClassName = configuration.getAttribute("pushGroupManager", null);
        }
        if (_pushGroupManagerClassName == null || _pushGroupManagerClassName.trim().length() == 0) {
            LOGGER.log(Level.FINE, "Using annotation scanner to find @ExtendedPushGroupManager.");
            Set<String> _annotationSet = new HashSet<String>();
            _annotationSet.add("Lorg/icepush/ExtendedPushGroupManager;");
            AnnotationScanner _annotationScanner = new AnnotationScanner(_annotationSet, servletContext);
            try {
                // throws IOException
                Set<Class> _classSet = _annotationScanner.getClassSet();
                _classSet.remove(PushGroupManagerFactory.class);
                for (final Class _class : _classSet) {
                    try {
                        LOGGER.log(Level.FINE, "Found class: '" + _class + "'");
                        if (executor == null) {
                            PushGroupManager _pushGroupManager =
                                (PushGroupManager)
                                    _class.
                                        // throws NoSuchMethodException, SecurityException
                                        getConstructor(ServletContext.class).
                                        // throws
                                        //     IllegalAccessException, IllegalArgumentException, InstantiationException,
                                        //     InvocationTargetException, ExceptionInInitializerError
                                        newInstance(servletContext);
                            LOGGER.log(Level.FINE, "Using class: '" + _class + "'");
                            return _pushGroupManager;
                        } else {
                            PushGroupManager _pushGroupManager =
                                (PushGroupManager)
                                    _class.
                                        // throws NoSuchMethodException, SecurityException
                                        getConstructor(ServletContext.class, ScheduledThreadPoolExecutor.class).
                                        // throws
                                        //     IllegalAccessException, IllegalArgumentException, InstantiationException,
                                        //     InvocationTargetException, ExceptionInInitializerError
                                        newInstance(servletContext, executor);
                            LOGGER.log(Level.FINE, "Using class: '" + _class + "'");
                            return _pushGroupManager;
                        }
                    } catch (NoSuchMethodException exception) {
                        LOGGER.log(Level.FINE, "Can't get constructor!", exception);
                        // Do nothing.
                    } catch (SecurityException exception) {
                        LOGGER.log(Level.FINE, "Can't get constructor!", exception);
                        // Do nothing.
                    } catch (IllegalAccessException exception) {
                        LOGGER.log(Level.FINE, "Can't create instance!", exception);
                        // Do nothing.
                    } catch (IllegalArgumentException exception) {
                        LOGGER.log(Level.FINE, "Can't create instance!", exception);
                        // Do nothing.
                    } catch (InstantiationException exception) {
                        LOGGER.log(Level.FINE, "Can't create instance!", exception);
                        // Do nothing.
                    } catch (InvocationTargetException exception) {
                        LOGGER.log(Level.FINE, "Can't create instance!", exception);
                        // Do nothing.
                    } catch (ExceptionInInitializerError error) {
                        LOGGER.log(Level.FINE, "Can't create instance!", error);
                        // Do nothing.
                    }
                }
            } catch (IOException exception) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(
                        Level.FINE,
                        "An I/O error occurred while trying to get the classes.",
                        exception);
                }
                // Do nothing.
            }
        } else {
            LOGGER.log(Level.FINE, "PushGroupManager: " + _pushGroupManagerClassName);
            try {
                return
                    (PushGroupManager)
                        // throws ClassNotFoundException, ExceptionInInitializerError
                        Class.forName(_pushGroupManagerClassName).
                            // throws NoSuchMethodException, SecurityException
                            getConstructor(ServletContext.class).
                            // throws
                            //     IllegalAccessException, IllegalArgumentException, InstantiationException,
                            //     InvocationTargetException, ExceptionInInitializerError
                            newInstance(servletContext);
            } catch (ClassNotFoundException exception) {
                LOGGER.log(Level.FINE, "Can't find class!", exception);
                // Do nothing.
            } catch (ExceptionInInitializerError error) {
                LOGGER.log(Level.FINE, "Can't find class or can't create instance!", error);
                // Do nothing.
            } catch (NoSuchMethodException exception) {
                LOGGER.log(Level.FINE, "Can't get constructor!", exception);
                // Do nothing.
            } catch (SecurityException exception) {
                LOGGER.log(Level.FINE, "Can't get constructor!", exception);
                // Do nothing.
            } catch (IllegalAccessException exception) {
                LOGGER.log(Level.FINE, "Can't create instance!", exception);
                // Do nothing.
            } catch (IllegalArgumentException exception) {
                LOGGER.log(Level.FINE, "Can't create instance!", exception);
                // Do nothing.
            } catch (InstantiationException exception) {
                LOGGER.log(Level.FINE, "Can't create instance!", exception);
                // Do nothing.
            } catch (InvocationTargetException exception) {
                LOGGER.log(Level.FINE, "Can't create instance!", exception);
                // Do nothing.
            }
        }
        LOGGER.log(Level.FINE, "Falling back to LocalPushGroupManager.");
        return new LocalPushGroupManager(servletContext);
    }
}
