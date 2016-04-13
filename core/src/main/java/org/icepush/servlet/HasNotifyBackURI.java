package org.icepush.servlet;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icepush.Browser;
import org.icepush.PushContext;

public class HasNotifyBackURI
extends AbstractPseudoServlet
implements PseudoServlet {
    private static final Logger LOGGER = Logger.getLogger(HasNotifyBackURI.class.getName());

    private final PushContext pushContext;

    public HasNotifyBackURI(final PushContext pushContext) {
        this.pushContext = pushContext;
    }

    public void service(final HttpServletRequest request, final HttpServletResponse response)
    throws Exception {
        String _browserID = Browser.getBrowserID(request);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                Level.FINE,
                "Checking if Browser '" + _browserID + "' has an associated Notify-Back-URI."
            );
        }
        response.setContentType("text/plain");
        response.getWriter().print(hasNotifyBackURI(_browserID));
    }

    protected final PushContext getPushContext() {
        return pushContext;
    }

    protected boolean hasNotifyBackURI(final String browserID) {
        return getPushContext().hasNotifyBackURI(browserID);
    }
}
