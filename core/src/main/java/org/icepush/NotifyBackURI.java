package org.icepush;

import java.io.Serializable;

public class NotifyBackURI
implements Serializable {
    private final String uri;

    private long timestamp = -1L;

    public NotifyBackURI(final String uri)
    throws IllegalArgumentException {
        if (uri == null || uri.trim().length() == 0) {
            throw new IllegalArgumentException("The specified uri is null or empty.");
        }
        this.uri = uri;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getURI() {
        return uri;
    }

    public String toString() {
        return new StringBuilder().append(uri.toString()).append(" [last: ").append(timestamp).append("]").toString();
    }

    public void touch() {
        timestamp = System.currentTimeMillis();
    }
}
