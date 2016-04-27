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

import static org.icesoft.util.ObjectUtilities.*;
import static org.icesoft.util.PreCondition.checkArgument;
import static org.icesoft.util.StringUtilities.isNotNullAndIsNotEmpty;

import java.io.Serializable;
import java.net.URI;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PushNotification
extends PushConfiguration
implements Serializable {
    private static final long serialVersionUID = -3493918269014212172L;

    private static final Logger LOGGER = Logger.getLogger(PushNotification.class.getName());

    /** The PushNotification attribute name for subject. */
    public static String SUBJECT = "subject";

    /** The PushNotification attribute name for detail. */
    public static String DETAIL = "detail";

    /** The PushNotification attribute name for targetURI. */
    public static String TARGET_URI = "targetURI";

    /** The PushNotification attribute name for forced. */
    public static String FORCED = "forced";

    protected PushNotification() {
        super();
    }

    /**
     * <p>
     *     Constructs a new PushNotification with the specified <code>attributes</code>.
     * </p>
     *
     * @param      attributeMap
     *                 The attributes of the new PushNotification to be constructed.
     * @see        #PushNotification(String, URI)
     * @see        #PushNotification(String, String, URI)
     * @throws     IllegalArgumentException
     *                 If the specified <code>attributeMap</code> contains <code>null</code> or empty keys or
     *                 <code>null</code> values.
     */
    public PushNotification(final Map<String, Object> attributeMap)
    throws IllegalArgumentException {
        super(attributeMap);
    }

    /**
     * <p>
     *     Constructs a new PushNotification with the specified <code>subject</code>, <code>detail</code> and
     *     <code>targetURI</code>.
     * </p>
     *
     * @param      subject
     *                 The subject of the new PushNotification to be constructed.
     * @param      detail
     *                 The detail of the new PushNotification to be constructed.
     * @param      targetURI
     *                 The targetURI of the new PushNotification to be constructed.
     * @see        #PushNotification(String, URI)
     * @see        #PushNotification(Map)
     * @throws     IllegalArgumentException
     *                 If the specified <code>subject</code> and/or <code>targetURI</code> is <code>null</code> or
     *                 empty.
     */
    public PushNotification(final String subject, final String detail, final URI targetURI)
    throws IllegalArgumentException {
        this(subject, detail, targetURI, false);
    }

    /**
     * <p>
     *     Constructs a new PushNotification with the specified <code>subject</code>, <code>detail</code>,
     *     <code>targetURI</code> and <code>forced</code>.
     * </p>
     *
     * @param      subject
     *                 The subject of the new PushNotification to be constructed.
     * @param      detail
     *                 The detail of the new PushNotification to be constructed.
     * @param      targetURI
     *                 The targetURI of the new PushNotification to be constructed.
     * @param      forced
     *                 If <code>true</code> the new PushNotification to be constructed will force a Cloud Push
     *                 Notification.  (default: <code>false</code>)
     * @see        #PushNotification(String, URI)
     * @see        #PushNotification(Map)
     * @throws     IllegalArgumentException
     *                 If the specified <code>subject</code> and/or <code>targetURI</code> is <code>null</code> or
     *                 empty.
     */
    public PushNotification(final String subject, final String detail, final URI targetURI, final boolean forced)
    throws IllegalArgumentException {
        setSubject(subject);
        setDetail(detail);
        setTargetURI(targetURI);
        setForced(forced);
    }

    /**
     * <p>
     *     Constructs a new PushNotification with the specified <code>subject</code> and <code>targetURI</code>.
     * </p>
     *
     * @param      subject
     *                 The subject of the new PushNotification to be constructed.
     * @param      targetURI
     *                 The targetURI of the new PushNotification to be constructed.
     * @see        #PushNotification(String, String, URI)
     * @see        #PushNotification(Map)
     * @throws     IllegalArgumentException
     *                 If the specified <code>subject</code> and/or <code>targetURI</code> is <code>null</code> or
     *                 empty.
     */
    public PushNotification(final String subject, final URI targetURI)
    throws IllegalArgumentException {
        this(subject, targetURI, false);
    }

    /**
     * <p>
     *     Constructs a new PushNotification with the specified <code>subject</code>, <code>targetURI</code> and
     *     <code>forced</code>.
     * </p>
     *
     * @param      subject
     *                 The subject of the new PushNotification to be constructed.
     * @param      targetURI
     *                 The targetURI of the new PushNotification to be constructed.
     * @param      forced
     *                 If <code>true</code> the new PushNotification to be constructed will force a Cloud Push
     *                 Notification.  (default: <code>false</code>)
     * @see        #PushNotification(String, String, URI)
     * @see        #PushNotification(Map)
     * @throws     IllegalArgumentException
     *                 If the specified <code>subject</code> and/or <code>targetURI</code> is <code>null</code> or
     *                 empty.
     */
    public PushNotification(final String subject, final URI targetURI, final boolean forced)
    throws IllegalArgumentException {
        setSubject(subject);
        setTargetURI(targetURI);
        setForced(forced);
    }

    /**
     * <p>
     *     Gets the detail of this Push Notification.
     * </p>
     *
     * @return     The detail.
     */
    public String getDetail() {
        return (String)getAttribute(DETAIL);
    }

    /**
     * <p>
     *     Gets the subject of this Push Notification.
     * </p>
     *
     * @return     The subject.
     */
    public String getSubject() {
        return (String)getAttribute(SUBJECT);
    }

    /**
     * <p>
     *     Gets the target URI of this Push Notification.
     * </p>
     *
     * @return     The target URI.
     */
    public String getTargetURI() {
        return (String)getAttribute(TARGET_URI);
    }

    public boolean isForced() {
        return (Boolean)getAttribute(FORCED);
    }

    public void setDetail(final String detail)
    throws IllegalArgumentException {
        checkArgument(
            isNotNullAndIsNotEmpty(detail),
            "Illegal argument detail: '" + detail + "'.  Argument cannot be null or empty."
        );
        putAttribute(DETAIL, detail);
    }

    public void setForced(final boolean forced) {
        putAttribute(FORCED, forced);
    }

    public void setSubject(final String subject)
    throws IllegalArgumentException {
        checkArgument(
            isNotNullAndIsNotEmpty(subject),
            "Illegal argument subject: '" + subject + "'.  Argument cannot be null or empty."
        );
        putAttribute(SUBJECT, subject);
    }

    public void setTargetURI(final URI targetURI) {
        checkArgument(
            isNotNull(targetURI),
            "Illegal argument targetURI: '" + targetURI + "'.  Argument cannot be null."
        );
        putAttribute(TARGET_URI, targetURI.toString());
    }

    @Override
    public String toString() {
        return
            new StringBuilder().
                append("PushNotification[").
                    append(classMembersToString()).
                append("]").
                    toString();
    }
}