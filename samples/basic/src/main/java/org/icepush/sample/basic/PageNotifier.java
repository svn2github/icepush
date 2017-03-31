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
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                pushContext.push("A");
            }
        }, 0, 5000);
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
        w.write("var p = ice.setupPush({\n" +
                "            uri: 'http\\:\\/\\/localhost/notify/',\n" +
                "            account: 'icesoft_technologies',\n" +
                "            realm: 'icesoft.com',\n" +
                "            access_token: '12a18df7-c7a5-429e-98b8-4e23964b149c'\n" +
                "        }); p.createPushId(1, function(id) {p.register([id], function(pushIds) { ice.info(ice.logger, pushIds);document.getElementById('notifications').innerHTML = ice.push.getCurrentNotifications(); }); });");
        w.write("</script><h2>Basic ICEpush Test</h2><div>Current Push Notifications: <span id='notifications'></span></div>");
        w.write("</body></html>");
        response.setContentType("text/html");

        pushContext.addGroupMember("A", idA);
        //pushContext.addGroupMember("A", idB);
    }

    public void destroy() {
        timer.cancel();
    }
}
