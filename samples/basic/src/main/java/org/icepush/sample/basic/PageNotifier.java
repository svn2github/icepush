/*
 * Version: MPL 1.1
 *
 * "The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Initial Developer of the Original Code is ICEsoft
 * Technologies Canada, Corp. Portions created by ICEsoft are Copyright (C)
 * 2004-2010 ICEsoft Technologies Canada, Corp. All Rights Reserved.
 *
 * Contributor(s): _____________________.
 *
*/
package org.icepush.sample.basic;

import org.icepush.PushContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Timer;
import java.util.TimerTask;

public class PageNotifier extends HttpServlet {
    private Timer timer;
    private PushContext pushContext;

    public void init(ServletConfig servletConfig) throws ServletException {
        timer = new Timer(true);
        pushContext = PushContext.getInstance(servletConfig.getServletContext());
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final String idA = pushContext.createPushId(request, response);
        final String idB = pushContext.createPushId(request, response);

        PrintWriter w = response.getWriter();
        w.write("<html><head><title>");
        w.write(idA + "; " + idB);
        w.write("</title>");
        w.write("<script type=\"text/javascript\" src=\"code.icepush\"></script>");
        w.write("</head><body>");

        w.write("<script type=\"text/javascript\">");
        w.write("ice.push.register(['" + idA + "', '" + idB + "'], function(pushIds) { ice.info(ice.logger, ice.push.getCurrentNotifications());document.getElementById('notifications').innerHTML = ice.push.getCurrentNotifications(); });");
        w.write("</script><h2>Basic ICEpush Test</h2><div>Current Push Notifications: <span id='notifications'></span></div>");
        w.write("</body></html>");
        response.setContentType("text/html");

        pushContext.addGroupMember("A", idA);
        pushContext.addGroupMember("A", idB);
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                pushContext.push("A");
            }
        }, 0, 5000);
    }

    public void destroy() {
        timer.cancel();
    }
}
