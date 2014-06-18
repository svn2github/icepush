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

import org.icepush.servlet.PseudoServlet;

import javax.servlet.http.*;
import java.util.*;

public class RemoveParameterPrefix implements PseudoServlet {
    private PseudoServlet servlet;

    public RemoveParameterPrefix(PseudoServlet servlet) {
        this.servlet = servlet;
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws Exception {
        servlet.service(new RemoveParameterPrefixRequest(request), response);
    }

    public void shutdown() {
        servlet.shutdown();
    }

    private static String extractParameterPrefix(HttpServletRequest request) {
        Enumeration e = request.getParameterNames();
        String parameterPrefix = "";
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            if (name.contains("ice.push.browser")) {
                parameterPrefix = name.substring(0, name.indexOf("ice.push.browser"));
                break;
            }
        }

        return parameterPrefix;
    }

    private static class RemoveParameterPrefixRequest extends HttpServletRequestWrapper {
        private final String parameterPrefix;

        public RemoveParameterPrefixRequest(HttpServletRequest request) {
            super(request);
            this.parameterPrefix = extractParameterPrefix(request);

        }

        public String getParameter(String name) {
            return super.getParameter(parameterPrefix + name);
        }

        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> result = new HashMap();
            int length = parameterPrefix.length();
            Iterator entries = super.getParameterMap().entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<String, String[]> next = (Map.Entry) entries.next();
                String name = next.getKey();
                String originalParameterName = name.substring(0, length);
                result.put(originalParameterName, next.getValue());
            }
            return result;
        }

        public Enumeration<String> getParameterNames() {
            ArrayList result = new ArrayList();
            int length = parameterPrefix.length();
            Enumeration entries = super.getParameterNames();
            while (entries.hasMoreElements()) {
                String name = (String) entries.nextElement();
                String originalParameterName = name.substring(0, length);
                result.add(originalParameterName);
            }
            return Collections.enumeration(result);
        }

        public String[] getParameterValues(String name) {
            return super.getParameterValues(parameterPrefix + name);
        }
    }
}
