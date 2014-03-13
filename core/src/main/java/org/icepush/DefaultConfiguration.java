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
package org.icepush;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class DefaultConfiguration
extends Configuration {
    private final static Logger LOGGER = Logger.getLogger(DefaultConfiguration.class.getName());

    private final Map<String, String> defaultParameterMap = new HashMap<String, String>();

    private final Configuration configuration;

    public DefaultConfiguration(final Configuration configuration) {
        this.configuration = configuration;
    }

    public String getAttribute(final String name)
    throws ConfigurationException {
        try {
            return configuration.getAttribute(name);
        } catch (ConfigurationException exception) {
            if (defaultParameterMap.containsKey(name)) {
                return defaultParameterMap.get(name);
            } else {
                throw exception;
            }
        }
    }

    public Configuration getChild(final String child)
    throws ConfigurationException {
        return new DefaultConfiguration(configuration.getChild(child));
    }

    public Configuration[] getChildren(final String name)
    throws ConfigurationException {
        Configuration[] children = configuration.getChildren(name);
        Configuration[] wrappedChildren = new Configuration[children.length];
        for (int i = 0; i < children.length; i++) {
            wrappedChildren[i] = new DefaultConfiguration(children[i]);
        }
        return wrappedChildren;
    }

    public String getName() {
        return configuration.getName();
    }

    public String getValue()
    throws ConfigurationException {
        return configuration.getValue();
    }

    public void setAttributeDefault(final String name, final boolean value) {
        setAttributeDefault(name, Boolean.toString(value));
    }

    public void setAttributeDefault(final String name, final double value) {
        setAttributeDefault(name, Double.toString(value));
    }

    public void setAttributeDefault(final String name, final float value) {
        setAttributeDefault(name, Float.toString(value));
    }

    public void setAttributeDefault(final String name, final int value) {
        setAttributeDefault(name, Integer.toString(value));
    }

    public void setAttributeDefault(final String name, final long value) {
        setAttributeDefault(name, Long.toString(value));
    }

    public void setAttributeDefault(final String name, final String value) {
        defaultParameterMap.put(name, value);
    }
}
