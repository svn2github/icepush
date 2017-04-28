/*
 * Copyright 2004-2014 ICEsoft Technologies Canada Corp.
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

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.http.PushResponse;
import org.icepush.http.PushResponseHandler;
import org.icepush.http.standard.FixedXMLContentHandler;

public class Noop
extends FixedXMLContentHandler
implements PushResponseHandler {
    private static final Logger LOGGER = Logger.getLogger(Noop.class.getName());

    private final String reason;

    public Noop(final String reason) {
        this.reason = reason;
    }

    public void respond(final PushResponse pushResponse)
    throws Exception {
        pushResponse.setHeader("X-Connection-reason", getReason());
        super.respond(pushResponse);
    }

    @Override
    public void writeTo(final Writer writer)
    throws IOException {
        writer.write("<noop/>");
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Sending NoOp.");
        }
    }

    @Override
    public String toString() {
        return
            new StringBuilder().
                append("Noop[").
                    append(classMembersToString()).
                append("]").
                    toString();
    }

    protected String classMembersToString() {
        return
            new StringBuilder().
                append("reason: '").append(getReason()).append("'").
                    toString();
    }

    protected String getReason() {
        return reason;
    }
}
