package org.icepush.servlet;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icepush.PushContext;

public class AddGroupMember
extends AbstractPseudoServlet
implements PseudoServlet {
    private static final Logger LOGGER = Logger.getLogger(AddGroupMember.class.getName());

    private final PushContext pushContext;

    public AddGroupMember(final PushContext pushContext) {
        this.pushContext = pushContext;
    }

    public void service(final HttpServletRequest request, final HttpServletResponse response)
    throws Exception {
        addGroupMember(request.getParameter("group"), request.getParameter("id"));
        response.setContentType("text/plain");
        response.setContentLength(0);
    }

    protected void addGroupMember(
        final String groupName, final String pushID) {

        getPushContext().addGroupMember(groupName, pushID);
    }

    protected PushContext getPushContext() {
        return pushContext;
    }
}