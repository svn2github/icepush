package org.icepush.servlet;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icepush.Browser;
import org.icepush.PushContext;

public class AddNotifyBackURI
extends AbstractPseudoServlet
implements PseudoServlet {
    private static final Logger LOGGER = Logger.getLogger(AddNotifyBackURI.class.getName());

    private final PushContext pushContext;

    public AddNotifyBackURI(final PushContext pushContext) {
        this.pushContext = pushContext;
    }

    public void service(final HttpServletRequest request, final HttpServletResponse response)
    throws Exception {
        String _browserID = Browser.getBrowserID(request);
        URI _notifyBackURI = new URI(request.getParameter("notifyBackURI"));
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                Level.FINE,
                "Adding Notify-Back-URI '" + _notifyBackURI + "' to Browser '" + _browserID + "'."
            );
        }
        addNotifyBackURI(_browserID, _notifyBackURI);
        response.setContentType("text/plain");
        response.setContentLength(0);
    }

    protected void addNotifyBackURI(final String browserID, final URI notifyBackURI) {
        getPushContext().addNotifyBackURI(browserID, notifyBackURI);
    }

    protected final PushContext getPushContext() {
        return pushContext;
    }
}
