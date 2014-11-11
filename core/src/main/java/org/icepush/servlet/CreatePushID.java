package org.icepush.servlet;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icepush.PushContext;

public class CreatePushID
extends AbstractPseudoServlet
implements PseudoServlet {
    private static final Logger LOGGER = Logger.getLogger(CreatePushID.class.getName());

    private final PushContext pushContext;

    public CreatePushID(final PushContext pushContext) {
        this.pushContext = pushContext;
    }

    public void service(final HttpServletRequest request, final HttpServletResponse response)
    throws Exception {
        response.setContentType("text/plain");
        response.getOutputStream().print(createPushID(request, response));
    }

    protected String createPushID(final HttpServletRequest request, final HttpServletResponse response) {
        return getPushContext().createPushId(request, response);
    }

    protected PushContext getPushContext() {
        return pushContext;
    }
}
