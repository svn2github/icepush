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
