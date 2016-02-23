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

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icepush.util.ExtensionRegistry;

public class ICEpushServlet
extends HttpServlet
implements Serializable, Servlet, ServletConfig {
    private static final Logger LOGGER = Logger.getLogger(MainServlet.class.getName());

    private PseudoServlet mainServlet;

    public void init(final ServletConfig servletConfig)
    throws ServletException {
        super.init(servletConfig);
        ServletContext servletContext = servletConfig.getServletContext();
        Class mainServletClass = (Class)ExtensionRegistry.getBestExtension(servletContext, "org.icepush.MainServlet");
        try {
            Constructor mainServletGet = mainServletClass.getDeclaredConstructor(new Class[]{ServletContext.class});
            mainServlet = (PseudoServlet)mainServletGet.newInstance(new Object[]{servletContext});
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Cannot instantiate extension org.icepush.MainServlet.", exception);
            throw new ServletException(exception);
        }
    }

    protected void service(final HttpServletRequest request, final HttpServletResponse response)
    throws IOException, ServletException {
        try {
            mainServlet.service(request, response);
        } catch (final ServletException exception) {
            throw exception;
        } catch (final IOException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public void destroy() {
        mainServlet.shutdown();
    }
}
