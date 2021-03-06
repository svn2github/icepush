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

public class NotificationSpreading extends HttpServlet {
    private Timer timer;
    private PushContext pushContext;

    public void init(ServletConfig servletConfig) throws ServletException {
        timer = new Timer(true);
        pushContext = PushContext.getInstance(servletConfig.getServletContext());
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                pushContext.push("A", new PushConfiguration().delayed(0, 3000));
            }
        }, 0, 3000);
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
        w.write("ice.push.register(['" + idA + "'], " +
                "function(pushIds) { " +
                "ice.info(ice.logger, ice.push.getCurrentNotifications());" +
                "var n = document.getElementById('notification');" +
                "var t = document.getElementById('timestamp');" +
                "n.innerHTML = ice.push.getCurrentNotifications();" +
                "t.innerHTML = new Date();" +
                "t.style.backgroundColor = 'red';" +
                "setTimeout(function() {t.style.backgroundColor = 'white';}, 200);" +
                "});");
        w.write("</script><h2>Notification spreading</h2>" +
                "<div>This page tests the spreading of the notifications across multiple browsers. " +
                "The notifications are sent every 3 seconds, spread across browsers with a set duration of 3 seconds. " +
                "By opening 3 separate browsers it should be observed how the notification is received in each browser at intervals of 1 second.</div>" +
                "<br/><br/>" +
                "<div>Push ID: <tt id='notification'></tt></div>" +
                "<div>Notified at: <tt id='timestamp'></tt></div>");
        w.write("</body></html>");
        response.setContentType("text/html");

        pushContext.addGroupMember("A", idA);
    }

    public void destroy() {
        timer.cancel();
    }
}
