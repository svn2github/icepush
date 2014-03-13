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
package org.icepush.sample.basic;

import org.icepush.PushConfiguration;
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

public class NotificationWindowOverlapping extends HttpServlet {
    private PushContext pushContext;

    public void init(ServletConfig servletConfig) throws ServletException {
        pushContext = PushContext.getInstance(servletConfig.getServletContext());
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final String idA = pushContext.createPushId(request, response);

        PrintWriter w = response.getWriter();
        w.write("<html><head><title>");
        w.write(idA);
        w.write("</title>");
        w.write("<script type=\"text/javascript\" src=\"code.icepush\"></script>");
        w.write("</head><body>");

        w.write("<script type=\"text/javascript\">");
        w.write("window.onload = function() { document.getElementById('notifications').appendChild(document.createTextNode('(page loaded at ' + new Date() + ')')); }; ");
        w.write("ice.push.register(['" + idA + "'], " +
                "function(pushIds) { " +
                "ice.info(ice.logger, ice.push.getCurrentNotifications());" +
                "var ns = document.getElementById('notifications');" +
                "ns.appendChild(document.createElement('div')).appendChild(document.createTextNode('>> ' + new Date()));" +
                "});");
        w.write("</script><h2>Notification window overlapping</h2>" +
                "<div>This page tests the notification coalescing executed by the server when duration windows for two or more " +
                "notifications overlap. When the page is loaded there are three notifications sent: " +
                "<ul>" +
                "<li>delay= 3 seconds, duration= 5 seconds</li>" +
                "<li>delay= 4 seconds, duration= 4 seconds</li>" +
                "<li>delay= 10 seconds, duration= 2 seconds</li>" +
                "</ul> The first and second notification should be coalesced so in the page should be oberseved that only " +
                "two notifications are received, one after 3 seconds and the second one after 10 seconds." +
                "</div>" +
                "<br/><br/>" +
                "<div id=\"notifications\">Notifications </div>");
        w.write("</body></html>");
        response.setContentType("text/html");

        pushContext.addGroupMember("B", idA);

        pushContext.push("B", new PushConfiguration().delayed(3000, 5000));
        pushContext.push("B", new PushConfiguration().delayed(4000, 4000));
        pushContext.push("B", new PushConfiguration().delayed(10000, 2000));
    }

    public void destroy() {
    }
}
