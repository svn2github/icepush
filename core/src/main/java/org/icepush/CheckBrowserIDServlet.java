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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icepush.servlet.PseudoServlet;

public class CheckBrowserIDServlet implements PseudoServlet {
    private static final Logger log = Logger.getLogger(CheckBrowserIDServlet.class.getName());
    private PseudoServlet servlet;

    public CheckBrowserIDServlet(PseudoServlet servlet) {
        this.servlet = servlet;
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String browserID = Browser.getBrowserID(request);
        if (browserID != null && !"".equals(browserID)) {
            servlet.service(request, response);
        } else {
            response.setContentType("text/xml");
            response.getOutputStream().print("<browser id=\"" + Browser.generateBrowserID() + "\"/>");
            log.fine("BrowserID set through blocking connection.");
        }
    }

    public void shutdown() {
        servlet.shutdown();
    }
}
