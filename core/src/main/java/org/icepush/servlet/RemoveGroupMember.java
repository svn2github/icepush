package org.icepush.servlet;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icepush.PushContext;

public class RemoveGroupMember
extends AbstractPseudoServlet
implements PseudoServlet {
    private static final Logger LOGGER = Logger.getLogger(RemoveGroupMember.class.getName());

    private final PushContext pushContext;

    public RemoveGroupMember(final PushContext pushContext) {
        this.pushContext = pushContext;
    }

    public void service(final HttpServletRequest request, final HttpServletResponse response)
    throws Exception {
        removeGroupMember(request.getParameter("group"), request.getParameter("id"));
        response.setContentType("text/plain");
        response.setContentLength(0);
    }

    protected PushContext getPushContext() {
        return pushContext;
    }

    protected void removeGroupMember(final String groupName, final String pushID) {
        getPushContext().removeGroupMember(groupName, pushID);
    }
}