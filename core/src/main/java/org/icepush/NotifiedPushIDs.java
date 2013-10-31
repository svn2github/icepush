package org.icepush;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashSet;
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

    private final Set<String> pushIDSet;

    public NotifiedPushIDs(final Set<NotificationEntry> notificationEntrySet) {
        Set<String> _pushIDSet = new HashSet<String>();
        for (final NotificationEntry _notificationEntry : notificationEntrySet) {
            _pushIDSet.add(_notificationEntry.getPushID());
        }
        pushIDSet = Collections.unmodifiableSet(_pushIDSet);
    }

    public void respond(final PushResponse pushResponse)
    throws Exception {
        super.respond(pushResponse);
    }

    @Override
    public void writeTo(final Writer writer)
    throws IOException {
        writer.write("<notified-pushids>");
        boolean first = true;
        for (final String pushID : pushIDSet) {
            if (!first) {
                writer.write(' ');
            }
            writer.write(pushID);
            first = false;
        }
        writer.write("</notified-pushids>");
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Sending Notified PushIDs '" + pushIDSet + "'.");
        }
    }

    protected Set<String> getPushIDSet() {
        return pushIDSet;
    }
}
