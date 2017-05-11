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
import java.util.Collections;
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

    private final Set<NotificationEntry> notificationEntrySet = new HashSet<NotificationEntry>();

    private final String browserID;

    public NotifiedPushIDs(final Set<NotificationEntry> notificationEntrySet, final String browserID) {
        this.notificationEntrySet.addAll(notificationEntrySet);
        this.browserID = browserID;
    }

    public final Set<NotificationEntry> getNotificationEntrySet() {
        return Collections.unmodifiableSet(getModifiableNotificationEntrySet());
    }

    public void respond(final PushResponse pushResponse)
    throws Exception {
        super.respond(pushResponse);
    }

    @Override
    public String toString() {
        return
            new StringBuilder().
                append("NotifiedPushIDs[").
                    append(classMembersToString()).
                append("]").
                    toString();
    }

    @Override
    public void writeTo(final Writer writer)
    throws IOException {
        Map<String, Set<String>> _payloadPushIDSetMap = new HashMap<String, Set<String>>();
        for (final NotificationEntry _notificationEntry : getNotificationEntrySet()) {
            String _payload = _notificationEntry.getPayload();
            Set<String> _pushIDSet;
            if (_payloadPushIDSetMap.containsKey(_payload)) {
                _pushIDSet = _payloadPushIDSetMap.get(_payload);
            } else {
                _pushIDSet = new HashSet<String>();
                _payloadPushIDSetMap.put(_payload, _pushIDSet);
            }
            _pushIDSet.add(_notificationEntry.getPushID());
        }
        StringBuilder _notifications = new StringBuilder();
        _notifications.
            append("<notifications>");
        for (final String _payload : _payloadPushIDSetMap.keySet()) {
            _notifications.append("<notification push-ids=\"");
            Set<String> _pushIDSet = _payloadPushIDSetMap.get(_payload);
            boolean _first = true;
            for (final String _pushID : _pushIDSet) {
                if (!_first) {
                    _notifications.append(" ");
                } else {
                    _first = false;
                }
                _notifications.append(_pushID);
            }
            _notifications.append("\"");
            if (_payload == null || _payload.trim().length() == 0) {
                _notifications.append(" /");
            }
            _notifications.append(">");
            if (_payload != null && _payload.trim().length() != 0) {
                _notifications.
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
        _notifications.
            append("</notifications>");
        writer.write(_notifications.toString());
    }

    protected String classMembersToString() {
        return
            new StringBuilder().
                append("browserID: '").append(getBrowserID()).append("', ").
                append("notificationEntrySet: '").append(getModifiableNotificationEntrySet()).append("'").
                    toString();
    }

    protected final String getBrowserID() {
        return browserID;
    }

    protected final Set<NotificationEntry> getModifiableNotificationEntrySet() {
        return notificationEntrySet;
    }
}
