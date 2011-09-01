package org.icepush.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Remove Group Member Request
 *
 * Request-URI:
 *     /<servlet-context-path>/remove-group-member.icepush
 *
 * Entity Body:
 *     id=<push-id>&group=<group-name>
 */
public class RemoveGroupMemberRequest
extends HttpRequest {
    private static final Logger LOGGER = Logger.getLogger(RemoveGroupMemberRequest.class.getName());

    public RemoveGroupMemberRequest(final String groupName, final String pushId, final String contextURI)
    throws URISyntaxException {
        super(
            Method.POST,                                                                                       // Method
                // throws URISyntaxException
            new URI(contextURI + "/remove-group-member.icepush").normalize(),                             // Request-URI
            toForm(groupName, pushId).getBytes());                                                        // Entity-Body
    }

    private static String toForm(final String groupName, final String pushId) {
        return new StringBuilder().append("group=").append(groupName).append("&id=").append(pushId).toString();
    }
}
