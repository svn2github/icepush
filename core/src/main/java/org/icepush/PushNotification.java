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

import static org.icesoft.util.ObjectUtilities.isEqualToAny;
import static org.icesoft.util.ObjectUtilities.isNotNull;
import static org.icesoft.util.PreCondition.checkArgument;
import static org.icesoft.util.StringUtilities.isNotNullAndIsNotEmpty;

import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PushNotification
extends PushConfiguration
implements Serializable {
    private static final long serialVersionUID = -3493918269014212172L;

    private static final Logger LOGGER = Logger.getLogger(PushNotification.class.getName());

    public static enum Category {
        GLOBAL("global"),
        BROWSER("browser"),
        CLOUD("cloud"),
        EMAIL("email"),
        SMS("sms");

        private final String value;

        private Category(final String value) {
            this.value = value;
        }

        public static Category fromValue(final String value) {
            return valueOf(value);
        }

        public String value() {
            return value;
        }
    }

    /** The PushNotification attribute name for detail. */
    public static final String DETAIL = "detail";

    /** The PushNotification attribute name for expireTime. */
    public static final String EXPIRE_TIME = "expireTime";

    /** The PushNotification attribute name for forced. */
    public static final String FORCED = "forced";

    /** The PushNotification attribute name for icon. */
    public static final String ICON = "icon";

    /** The PushNotification attribute name for payload. */
    public static final String PAYLOAD = "payload";

    /** The PushNotification attribute name for priority. */
    public static final String PRIORITY = "priority";

    /** The PushNotification attribute name for subject. */
    public static final String SUBJECT = "subject";

    /** The PushNotification attribute name for targetURI. */
    public static final String TARGET_URI = "targetURI";

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
        super(convertAttributeMap(attributeMap));
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
        return getDetail(Category.GLOBAL);
    }

    public String getDetail(final Category category) {
        return (String)getAttribute(getPrefixedKey(category, DETAIL));
    }

    /**
     * <p>
     *     Gets the expire time of this Push Notification.
     * </p>
     *
     * @return     The expire time.
     */
    public String getExpireTime() {
        return getExpireTime(Category.GLOBAL);
    }

    public String getExpireTime(final Category category) {
        return (String)getAttribute(getPrefixedKey(category, EXPIRE_TIME));
    }

    /**
     * <p>
     *     Gets the icon of this Push Notification.
     * </p>
     *
     * @return     The icon.
     */
    public String getIcon() {
        return getIcon(Category.GLOBAL);
    }

    public String getIcon(final Category category) {
        return (String)getAttribute(getPrefixedKey(category, ICON));
    }

    /**
     * <p>
     *     Gets the payload of this Push Notification.
     * </p>
     *
     * @return     The payload.
     */
    public String getPayload() {
        return getPayload(Category.GLOBAL);
    }

    public String getPayload(final Category category) {
        return (String)getAttribute(getPrefixedKey(category, PAYLOAD));
    }

    /**
     * <p>
     *     Gets the priority of this Push Notification.
     * </p>
     *
     * @return     The priority.
     */
    public String getPriority() {
        return getPriority(Category.GLOBAL);
    }

    public String getPriority(final Category category) {
        return (String)getAttribute(getPrefixedKey(category, PRIORITY));
    }

    /**
     * <p>
     *     Gets the subject of this Push Notification.
     * </p>
     *
     * @return     The subject.
     */
    public String getSubject() {
        return getSubject(Category.GLOBAL);
    }

    public String getSubject(final Category category) {
        return (String)getAttribute(getPrefixedKey(category, SUBJECT));
    }

    /**
     * <p>
     *     Gets the target URI of this Push Notification.
     * </p>
     *
     * @return     The target URI.
     */
    public String getTargetURI() {
        return getTargetUri(Category.GLOBAL);
    }

    public String getTargetUri(final Category category) {
        return (String)getAttribute(getPrefixedKey(category, TARGET_URI));
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
        setDetail(detail, Category.GLOBAL);
    }

    public void setDetail(final String detail, final Category category)
    throws IllegalArgumentException {
        checkArgument(
            isNotNullAndIsNotEmpty(detail),
            "Illegal argument detail: '" + detail + "'.  Argument cannot be null or empty."
        );
        putAttribute(category, DETAIL, detail);
    }

    public void setExpireTime(final String expireTime)
    throws IllegalArgumentException {
        checkArgument(
            isNotNullAndIsNotEmpty(expireTime),
            "Illegal argument expireTime: '" + expireTime + "'.  Argument cannot be null or empty."
        );
        setExpireTime(expireTime, Category.GLOBAL);
    }

    public void setExpireTime(final String expireTime, final Category category)
    throws IllegalArgumentException {
        checkArgument(
            isNotNullAndIsNotEmpty(expireTime),
            "Illegal argument expireTime: '" + expireTime + "'.  Argument cannot be null or empty."
        );
        putAttribute(category, EXPIRE_TIME, expireTime);
    }

    public void setForced(final boolean forced) {
        putAttribute(FORCED, forced);
    }

    public void setIcon(final String icon)
    throws IllegalArgumentException {
        checkArgument(
            isNotNullAndIsNotEmpty(icon),
            "Illegal argument icon: '" + icon + "'.  Argument cannot be null or empty."
        );
        setIcon(icon, Category.GLOBAL);
    }

    public void setIcon(final String icon, final Category category)
    throws IllegalArgumentException {
        checkArgument(
            isNotNullAndIsNotEmpty(icon),
            "Illegal argument icon: '" + icon + "'.  Argument cannot be null or empty."
        );
        putAttribute(category, ICON, icon);
    }

    public void setPayload(final String payload)
    throws IllegalArgumentException {
        checkArgument(
            isNotNullAndIsNotEmpty(payload),
            "Illegal argument payload: '" + payload + "'.  Argument cannot be null or empty."
        );
        setPayload(payload, Category.GLOBAL);
    }

    public void setPayload(final String payload, final Category category)
    throws IllegalArgumentException {
        checkArgument(
            isNotNullAndIsNotEmpty(payload),
            "Illegal argument payload: '" + payload + "'.  Argument cannot be null or empty."
        );
        putAttribute(category, PAYLOAD, payload);
    }

    public void setPriority(final String priority)
    throws IllegalArgumentException {
        checkArgument(
            isNotNullAndIsNotEmpty(priority),
            "Illegal argument priority: '" + priority + "'.  Argument cannot be null or empty."
        );
        setPriority(priority, Category.GLOBAL);
    }

    public void setPriority(final String priority, final Category category)
    throws IllegalArgumentException {
        checkArgument(
            isNotNullAndIsNotEmpty(priority),
            "Illegal argument priority: '" + priority + "'.  Argument cannot be null or empty."
        );
        putAttribute(category, PRIORITY, priority);
    }

    public void setSubject(final String subject)
    throws IllegalArgumentException {
        checkArgument(
            isNotNullAndIsNotEmpty(subject),
            "Illegal argument subject: '" + subject + "'.  Argument cannot be null or empty."
        );
        setSubject(subject, Category.GLOBAL);
    }

    public void setSubject(final String subject, final Category category)
    throws IllegalArgumentException {
        checkArgument(
            isNotNullAndIsNotEmpty(subject),
            "Illegal argument subject: '" + subject + "'.  Argument cannot be null or empty."
        );
        putAttribute(category, SUBJECT, subject);
    }

    public void setTargetURI(final URI targetURI)
    throws IllegalArgumentException {
        checkArgument(
            isNotNull(targetURI),
            "Illegal argument targetURI: '" + targetURI + "'.  Argument cannot be null."
        );
        setTargetUri(targetURI, Category.GLOBAL);
    }

    public void setTargetUri(final URI targetURI, final Category category)
    throws IllegalArgumentException {
        checkArgument(
            isNotNull(targetURI),
            "Illegal argument targetURI: '" + targetURI + "'.  Argument cannot be null."
        );
        putAttribute(category, TARGET_URI, targetURI);
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

    private static Map<String, Object> convertAttributeMap(final Map<String, Object> attributeMap) {
        Map<String, Object> _attributeMap;
        if (isNotNull(attributeMap)) {
            _attributeMap = new HashMap<String, Object>();
            for (final Map.Entry<String, Object> _mapEntry : attributeMap.entrySet()) {
                String _key = _mapEntry.getKey();
                if (isEqualToAny(_key, DETAIL, EXPIRE_TIME, ICON, PAYLOAD, PRIORITY, SUBJECT, TARGET_URI)) {
                    _attributeMap.put(Category.GLOBAL.value() + "$" + _key, _mapEntry.getValue());
                } else {
                    _attributeMap.put(_key, _mapEntry.getValue());
                }
            }
        } else {
            _attributeMap = null;
        }
        return _attributeMap;
    }

    private Object getAttribute(final Category category, final String key) {
        return getAttribute(getPrefixedKey(category, key));
    }

    private String getPrefixedKey(final Category category, final String key) {
        return
            new StringBuilder().
                append(category != null ? category.value() : Category.GLOBAL.value()).append("$").append(key).
                    toString();
    }

    private Object putAttribute(final Category category, final String key, final Object value) {
        return putAttribute(getPrefixedKey(category, key), value);
    }
}