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

public abstract class Configuration {

    public abstract String getName();

    public abstract Configuration getChild(String child) throws ConfigurationException;

    public abstract Configuration[] getChildren(String name) throws ConfigurationException;

    public abstract String getAttribute(String paramName) throws ConfigurationException;

    public abstract String getValue() throws ConfigurationException;

    public int getAttributeAsInteger(String name) throws ConfigurationException {
        return Integer.parseInt(getAttribute(name));
    }

    public long getAttributeAsLong(String name) throws ConfigurationException {
        return Long.parseLong(getAttribute(name));
    }

    public float getAttributeAsFloat(String name) throws ConfigurationException {
        return Float.parseFloat(getAttribute(name));
    }

    public double getAttributeAsDouble(String name) throws ConfigurationException {
        return Double.parseDouble(getAttribute(name));
    }

    public boolean getAttributeAsBoolean(String name) throws ConfigurationException {
        return Boolean.valueOf(getAttribute(name)).booleanValue();
    }

    public String getAttribute(String name, String defaultValue) {
        try {
            return getAttribute(name);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public int getAttributeAsInteger(String name, int defaultValue) {
        try {
            return getAttributeAsInteger(name);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public long getAttributeAsLong(String name, long defaultValue) {
        try {
            return getAttributeAsLong(name);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public float getAttributeAsFloat(String name, float defaultValue) {
        try {
            return getAttributeAsFloat(name);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public double getAttributeAsDouble(String name, double defaultValue) {
        try {
            return getAttributeAsDouble(name);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public boolean getAttributeAsBoolean(String name, boolean defaultValue) {
        try {
            return getAttributeAsBoolean(name);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public int getValueAsInteger() throws ConfigurationException {
        return Integer.parseInt(getValue());
    }

    public float getValueAsFloat() throws ConfigurationException {
        return Float.parseFloat(getValue());
    }

    public double getValueAsDouble() throws ConfigurationException {
        return Double.parseDouble(getValue());
    }

    public boolean getValueAsBoolean() throws ConfigurationException {
        return Boolean.valueOf(getValue()).booleanValue();
    }

    public long getValueAsLong() throws ConfigurationException {
        return Long.parseLong(getValue());
    }

    public String getValue(String defaultValue) {
        try {
            return getValue();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public int getValueAsInteger(int defaultValue) {
        try {
            return getValueAsInteger();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public long getValueAsLong(long defaultValue) {
        try {
            return getValueAsLong();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public float getValueAsFloat(float defaultValue) {
        try {
            return getValueAsFloat();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public double getValueAsDouble(double defaultValue) {
        try {
            return getValueAsDouble();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public boolean getValueAsBoolean(boolean defaultValue) {
        try {
            return getValueAsBoolean();
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    public void logBoolean(String name, boolean currValue, boolean defaultValue, StringBuilder info) {
        if (currValue == defaultValue) {
            info.append(name).append(": ").append(defaultValue).append(" [default]\n");
        } else {
            info.append(name).append(": ").append(currValue).append("\n");
        }
    }
    
    public void logString(String name, String currValue, String defaultValue, StringBuilder info) {
        if (currValue == defaultValue) {
            info.append(name).append(": ").append(defaultValue).append(" [default]\n");
        } else {
            info.append(name).append(": ").append(currValue).append("\n");
        }
    }
    
    public void logLong(String name, long currValue, long defaultValue, StringBuilder info) {
        if (currValue == defaultValue) {
            info.append(name).append(" = ").append(defaultValue).append(" [default]\n");
        } else {
            info.append(name).append(" = ").append(currValue).append("\n");

        }
    }

}
