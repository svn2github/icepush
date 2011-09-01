package org.icepush.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CreatePushIdRequest
extends HttpRequest {
    private static final Logger LOGGER = Logger.getLogger(CreatePushIdRequest.class.getName());

    public CreatePushIdRequest(final String contextURI)
    throws URISyntaxException {
        super(
            Method.GET,                                                                                        // Method
            // throws URISyntaxException
            new URI(contextURI + "/create-push-id.icepush").normalize());                                 // Request-URI
    }
}
