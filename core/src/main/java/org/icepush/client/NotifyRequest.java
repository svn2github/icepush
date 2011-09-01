package org.icepush.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Notify Request
 *
 * Request-URI:
 *     /<servlet-context-path>/notify.icepush
 *
 * Entity Body:
 *     group=<group-name>
 */
public class NotifyRequest
extends HttpRequest {
    private static final Logger LOGGER = Logger.getLogger(NotifyRequest.class.getName());

    public NotifyRequest(final String groupName, final String contextURI)
    throws URISyntaxException {
        super(
            Method.POST,                                                                                       // Method
            // throws URISyntaxException
            new URI(contextURI + "/notify.icepush").normalize(),                                          // Request-URI
            toForm(groupName).getBytes());                                                                // Entity-Body
    }

    private static String toForm(final String groupName) {
        return new StringBuilder().append("group=").append(groupName).toString();
    }
}
