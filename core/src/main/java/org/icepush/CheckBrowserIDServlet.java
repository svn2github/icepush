package org.icepush;

import org.icepush.servlet.PseudoServlet;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Logger;

public class CheckBrowserIDServlet implements PseudoServlet {
    private static final Logger log = Logger.getLogger(CheckBrowserIDServlet.class.getName());
    private static final String BrowserIDCookieName = "ice.push.browser";
    private PseudoServlet servlet;

    public CheckBrowserIDServlet(PseudoServlet servlet) {
        this.servlet = servlet;
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String browserIDParameter = request.getParameter(BrowserIDCookieName);
        if (browserIDParameter != null && !"".equals(browserIDParameter)) {
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
