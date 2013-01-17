/*
 * Copyright 2004-2013 ICEsoft Technologies Canada Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS
 * IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

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
