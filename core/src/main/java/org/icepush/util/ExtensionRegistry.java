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
package org.icepush.util;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.*;

public class ExtensionRegistry implements ServletContextListener {
    private static final String NAME = ExtensionRegistry.class.getName();

    public void contextInitialized(ServletContextEvent servletContextEvent) {
        addExtensionsMapIfNeeded(servletContextEvent.getServletContext());
    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {
    }

    private static void addExtensionsMapIfNeeded(ServletContext context) {
        if (context.getAttribute(NAME) == null) {
            context.setAttribute(NAME, new HashMap());
        }
    }

    /**
     * Add an extension under the specified name with the specified "quality"
     *
     * @param context
     * @param quality
     * @param name
     * @param extension
     */
    public static void addExtension(ServletContext context, int quality, String name, Object extension) {
        addExtensionsMapIfNeeded(context);
        Map extensions = (Map) context.getAttribute(NAME);
        ArrayList namedExtensions = (ArrayList) extensions.get(name);
        if (namedExtensions == null) {
            namedExtensions = new ArrayList();
            extensions.put(name, namedExtensions);
        }

        namedExtensions.add(new ExtensionEntry(quality, extension));
    }

    /**
     * Return the extension of the specified name with the highest "quality".
     *
     * @param context
     * @param name
     * @return
     */
    public static Object getBestExtension(ServletContext context, String name) {
        Map extensions = (Map) context.getAttribute(NAME);
        ArrayList namedExtensions = (ArrayList) extensions.get(name);
        Collections.sort(namedExtensions);
        return namedExtensions == null ? null : ((ExtensionEntry) namedExtensions.get(namedExtensions.size() - 1)).extension;
    }

    /**
     * Return an array of extensions ordered by the "quality" attribute.
     *
     * @param context
     * @param name
     * @return
     */
    public static Object[] getExtensions(ServletContext context, String name) {
        Map extensions = (Map) context.getAttribute(NAME);
        ArrayList<ExtensionEntry> namedExtensions = (ArrayList) extensions.get(name);
        if (null == namedExtensions)  {
            return null;
        }
        int size = namedExtensions.size();
        Collections.sort(namedExtensions);
        Iterator it = namedExtensions.iterator();
        Object[] result = new Object[size];
        for (int i = 0; i < size; i++) {
            result[i] = ((ExtensionEntry) it.next()).extension;
        }
        return result;
    }

    private static class ExtensionEntry implements Comparable {
        private int quality;
        private Object extension;

        private ExtensionEntry(int quality, Object extension) {
            this.quality = quality;
            this.extension = extension;
        }

        public int compareTo(Object o) {
            return quality - ((ExtensionEntry) o).quality;
        }
    }
}
