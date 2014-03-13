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
package org.icepush.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

public class PathDispatcher implements PseudoServlet {
    private List matchers = new ArrayList();
    private List servlets = new ArrayList();

    public void service(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String path = request.getRequestURI();
        ListIterator i = matchers.listIterator();
        while (i.hasNext()) {
            int index = i.nextIndex();
            Pattern pattern = (Pattern) i.next();
            if (pattern.matcher(path).find()) {
                PseudoServlet server = (PseudoServlet) servlets.get(index);
                server.service(request, response);
                return;
            }
        }

        response.sendError(404, "Could not find resource at " + path);
    }

    public void dispatchOn(String pathExpression, PseudoServlet toServlet) {
        matchers.add(Pattern.compile(pathExpression));
        servlets.add(toServlet);
    }

    public void shutdown() {
        Iterator i = servlets.iterator();
        while (i.hasNext()) {
            PseudoServlet servlet = (PseudoServlet) i.next();
            servlet.shutdown();
        }
    }
}
