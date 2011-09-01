package org.icepush.client;

public class PushClientException
extends Exception {
    public PushClientException() {
        super();
    }

    public PushClientException(final String message) {
        super(message);
    }

    public PushClientException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public PushClientException(final Throwable cause) {
        super(cause);
    }
}
