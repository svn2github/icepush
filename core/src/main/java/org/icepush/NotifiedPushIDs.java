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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.http.PushResponse;
import org.icepush.http.PushResponseHandler;
import org.icepush.http.standard.FixedXMLContentHandler;

public class NotifiedPushIDs
extends FixedXMLContentHandler
implements PushResponseHandler {
    private static final Logger LOGGER = Logger.getLogger(NotifiedPushIDs.class.getName());

    private final Map<String, Set<String>> payloadPushIDSetMap = new HashMap<String, Set<String>>();

    private final String browserID;

    public NotifiedPushIDs(final Set<NotificationEntry> notificationEntrySet, final String browserID) {
        for (final NotificationEntry _notificationEntry : notificationEntrySet) {
            String _payload = _notificationEntry.getPayload();
            Set<String> _pushIDSet;
            if (getPayloadPushIDSetMap().containsKey(_payload)) {
                _pushIDSet = getPayloadPushIDSetMap().get(_payload);
            } else {
                _pushIDSet = new HashSet<String>();
                getPayloadPushIDSetMap().put(_payload, _pushIDSet);
            }
            _pushIDSet.add(_notificationEntry.getPushID());
        }
        this.browserID = browserID;
    }

    public void respond(final PushResponse pushResponse)
    throws Exception {
        super.respond(pushResponse);
    }

    @Override
    public void writeTo(final Writer writer)
    throws IOException {
        StringBuilder _notificationServicePayload = new StringBuilder();
        _notificationServicePayload.
            append("<notifications>");
        for (final String _payload : getPayloadPushIDSetMap().keySet()) {
            _notificationServicePayload.append("<notification push-ids=\"");
            Set<String> _pushIDSet = getPayloadPushIDSetMap().get(_payload);
            boolean _first = true;
            for (final String _pushID : _pushIDSet) {
                if (!_first) {
                    _notificationServicePayload.append(" ");
                } else {
                    _first = false;
                }
                _notificationServicePayload.append(_pushID);
            }
            _notificationServicePayload.append("\"");
            if (_payload == null || _payload.trim().length() == 0) {
                _notificationServicePayload.append(" /");
            }
            _notificationServicePayload.append(">");
            if (_payload != null && _payload.trim().length() != 0) {
                _notificationServicePayload.
                    append(_payload).append("</notification>");
            }
            if (LOGGER.isLoggable(Level.FINE)) {
                if (_payload != null && _payload.trim().length() != 0) {
                    LOGGER.log(
                        Level.FINE,
                        "Sending Notification with Payload '" + _payload + "' to " +
                            "Browser '" + getBrowserID() + "' with Push-IDs '" + _pushIDSet + "'."
                    );
                } else {
                    LOGGER.log(
                        Level.FINE,
                        "Sending Notification to " +
                            "Browser '" + getBrowserID() + "' with Push-IDs '" + _pushIDSet + "'."
                    );
                }
            }
        }
        _notificationServicePayload.
            append("</notifications>");
        writer.write(_notificationServicePayload.toString());
    }

    protected String getBrowserID() {
        return browserID;
    }

    protected Map<String, Set<String>> getPayloadPushIDSetMap() {
        return payloadPushIDSetMap;
    }
}
