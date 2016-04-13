package org.icepush.servlet;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icepush.Browser;
import org.icepush.PushContext;

public class RemoveNotifyBackURI
extends AbstractPseudoServlet
implements PseudoServlet {
    private static final Logger LOGGER = Logger.getLogger(RemoveNotifyBackURI.class.getName());

    private final PushContext pushContext;

    public RemoveNotifyBackURI(final PushContext pushContext) {
        this.pushContext = pushContext;
    }

    public void service(final HttpServletRequest request, final HttpServletResponse response)
    throws Exception {
        String _browserID = Browser.getBrowserID(request);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                Level.FINE, 
                "Removing Notify-Back-URI from Browser '" + _browserID + "'."
            );
        }
        removeNotifyBackURI(_browserID);
        response.setContentType("text/plain");
        response.setContentLength(0);
    }

    protected final PushContext getPushContext() {
        return pushContext;
    }

    protected void removeNotifyBackURI(final String browserID) {
        getPushContext().removeNotifyBackURI(browserID);
    }
}
