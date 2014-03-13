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
package org.icepush.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class HttpMessage {
    private static final Logger LOGGER = Logger.getLogger(HttpMessage.class.getName());

    private final Map<String, List<String>> headerMap = new HashMap<String, List<String>>();
    private final byte[] entityBody;

    protected HttpMessage() {
        this(new byte[0]);
    }

    protected HttpMessage(final byte[] entityBody) {
        this.entityBody = entityBody;
    }

    protected HttpMessage(final Map<String, List<String>> headerMap) {
        this(headerMap, new byte[0]);
    }

    protected HttpMessage(final Map<String, List<String>> headerMap, final byte[] entityBody) {
        this.headerMap.putAll(headerMap);
        this.entityBody = entityBody;
    }

    public void addHeader(final String fieldName, final String fieldValue) {
        if (headerMap.containsKey(fieldName)) {
            headerMap.get(fieldName).add(fieldValue);
        } else {
            List<String> _fieldValueList = new ArrayList<String>();
            _fieldValueList.add(fieldValue);
            headerMap.put(fieldName, _fieldValueList);
        }
    }

    public byte[] getEntityBody() {
        return entityBody;
    }

    public String getEntityBodyAsString() {
        return new String(entityBody);
    }

    public List<String> getHeader(final String fieldName) {
        if (headerMap.containsKey(fieldName)) {
            return Collections.unmodifiableList(headerMap.get(fieldName));
        } else {
            return null;
        }
    }

    public Map<String, List<String>> getHeaders() {
        return Collections.unmodifiableMap(headerMap);
    }

    public void setHeader(final String fieldName, final String fieldValue) {
        List<String> _fieldValueList = new ArrayList<String>();
        _fieldValueList.add(fieldValue);
        headerMap.put(fieldName, _fieldValueList);
    }
}
