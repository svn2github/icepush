package org.icepush.servlet;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icepush.PushConfiguration;
import org.icepush.PushContext;

public class NotifyPushID
extends AbstractPseudoServlet
implements PseudoServlet {
    private static final Logger logger = Logger.getLogger(NotifyPushID.class.getName());

    private final PushContext pushContext;

    public NotifyPushID(final PushContext pushContext) {
        this.pushContext = pushContext;
    }

    public void service(final HttpServletRequest request, final HttpServletResponse response)
    throws Exception {
        String _groupName = request.getParameter("group");
        PushConfiguration _pushConfiguration = PushConfiguration.fromRequest(request);
        if (_pushConfiguration == null) {
            push(_groupName);
        } else {
            push(_groupName, _pushConfiguration);
        }
        response.setContentType("text/plain");
        response.setContentLength(0);
    }

    protected PushContext getPushContext() {
        return pushContext;
    }

    protected void push(final String groupName) {
        getPushContext().push(groupName);
    }

    protected void push(final String groupName, final PushConfiguration pushConfiguration) {
        getPushContext().push(groupName, pushConfiguration);
    }
}
