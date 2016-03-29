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

import static org.icesoft.util.PreCondition.checkArgument;
import static org.icesoft.util.StringUtilities.isNotNullAndIsNotEmpty;

import java.io.Serializable;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icepush.util.DatabaseEntity;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Entity(value = "notify_back_uris")
public class NotifyBackURI
implements DatabaseEntity, Serializable {
    private static final long serialVersionUID = 6137651045332272628L;

    private static final Logger LOGGER = Logger.getLogger(NotifyBackURI.class.getName());

    @Id
    private String databaseID;

    private long timestamp = -1L;

    private String browserID;
    private String uri;

    public NotifyBackURI() {
        // Do nothing.
    }

    protected NotifyBackURI(final String uri)
    throws IllegalArgumentException {
        checkArgument(
            isNotNullAndIsNotEmpty(uri), "Illegal argument uri: '" + uri + "'.  Argument cannot be null or empty."
        );
        this.uri = uri;
        this.databaseID = getURI();
    }

    @Override
    public boolean equals(final Object object) {
        return
            object instanceof NotifyBackURI &&
                ((NotifyBackURI)object).getBrowserID().equals(getBrowserID()) &&
                ((NotifyBackURI)object).getTimestamp() != getTimestamp() &&
                ((NotifyBackURI)object).getURI().equals(getURI());
    }

    public String getDatabaseID() {
        return databaseID;
    }

    public String getKey() {
        return getURI();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getURI() {
        return uri;
    }

    public void save() {
        ConcurrentMap<String, NotifyBackURI> _notifyBackURIMap =
            (ConcurrentMap<String, NotifyBackURI>)PushInternalContext.getInstance().getAttribute("notifyBackURIMap");
        if (_notifyBackURIMap.containsKey(getKey())) {
            _notifyBackURIMap.put(getKey(), this);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                    Level.FINE,
                    "Saved Notify-Back-URI '" + this + "' to Database."
                );
            }
        }
    }

    public String toString() {
        return
            new StringBuilder().
                append("NotifyBackURI[").
                    append(classMembersToString()).
                append("]").
                    toString();
    }

    public void touch() {
        timestamp = System.currentTimeMillis();
        save();
    }

    protected String classMembersToString() {
        return
            new StringBuilder().
                append("uri: '").append(uri).append("', ").
                append("timestamp: '").append(timestamp).append("'").
                    toString();
    }

    protected String getBrowserID() {
        return browserID;
    }

    protected boolean setBrowserID(final String browserID) {
        boolean _modified;
        if ((this.browserID == null && browserID != null) ||
            (this.browserID != null && !this.browserID.equals(browserID))) {

            this.browserID = browserID;
            _modified = true;
            save();
        } else {
            _modified = false;
        }
        return _modified;
    }
}
