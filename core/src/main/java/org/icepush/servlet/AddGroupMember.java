package org.icepush.servlet;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icepush.PushConfiguration;
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
        String _groupName = request.getParameter("group");
        String _pushID = request.getParameter("id");
        String _cloudPush = request.getParameter("cloudPush");
        if (_cloudPush != null) {
            PushConfiguration _pushConfiguration = new PushConfiguration();
            _pushConfiguration.getAttributes().put("cloudPush", Boolean.valueOf(_cloudPush));
            addGroupMember(_groupName, _pushID, _pushConfiguration);
        } else {
            addGroupMember(_groupName, _pushID);
        }
        response.setContentType("text/plain");
        response.setContentLength(0);
    }

    protected void addGroupMember(
        final String groupName, final String pushID) {

        getPushContext().addGroupMember(groupName, pushID);
    }

    protected void addGroupMember(
        final String groupName, final String pushID, final PushConfiguration pushConfiguration) {

        getPushContext().addGroupMember(groupName, pushID, pushConfiguration);
    }

    protected PushContext getPushContext() {
        return pushContext;
    }
}