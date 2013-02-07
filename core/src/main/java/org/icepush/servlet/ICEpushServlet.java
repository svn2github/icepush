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
package org.icepush.servlet;

import org.icepush.util.ExtensionRegistry;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ICEpushServlet extends HttpServlet {
    private static final Logger Log = Logger.getLogger(MainServlet.class.getName());
    private PseudoServlet mainServlet;

    public void init(final ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        ServletContext servletContext = servletConfig.getServletContext();
        Class mainServletClass = (Class) ExtensionRegistry.getBestExtension(servletContext, "org.icepush.MainServlet");
        try {
            Method mainServletGet = mainServletClass.getMethod("getInstance",
                new Class[]{ServletContext.class});
            mainServlet = (PseudoServlet) mainServletGet
                .invoke(null, servletContext);
        } catch (Exception e) {
            Log.log(Level.SEVERE, "Cannot instantiate extension org.icepush.MainServlet.", e);
            throw new ServletException(e);
        }
    }

    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            mainServlet.service(request, response);
        } catch (ServletException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void destroy() {
        mainServlet.shutdown();
    }
}
