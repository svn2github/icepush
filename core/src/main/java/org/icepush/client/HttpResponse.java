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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class HttpResponse
extends HttpMessage {
    private static final Logger LOGGER = Logger.getLogger(HttpResponse.class.getName());

    public static final class StatusCode {
        public static final int OK = 200;

        private StatusCode() { /* Do nothing. */ }
    }

    private final int statusCode;
    private final String reasonPhrase;

    public HttpResponse(
        final int statusCode, final String reasonPhrase, final Map<String, List<String>> headerMap,
        final byte[] entityBody) {

        super(Collections.unmodifiableMap(headerMap), entityBody);
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }

    public void addHeader(final String fieldName, final String fieldValue)
    throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public Document getEntityBodyAsDocument() {
        byte[] _entityBody = getEntityBody();
        if (_entityBody.length > 0) {
            try {
                // throws FactoryConfigurationError, ParserConfigurationException, IOException, SAXException
                return
                    DocumentBuilderFactory.newInstance().newDocumentBuilder().
                        parse(new ByteArrayInputStream(_entityBody));
            } catch (FactoryConfigurationError error) {
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(
                        Level.FINEST,
                        "An error occurred while trying to get a new DocumentBuilderFactory instance: " +
                            "'The implementation is not available or cannot be instantiated.'",
                        error);
                }
            } catch (ParserConfigurationException exception) {
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(
                        Level.FINEST,
                        "An error occurred while trying to get a new DocumentBuilder instance: " +
                            "'A DocumentBuilder cannot be created which satisfies the configuration requested.'",
                        exception);
                }
            } catch (IOException exception) {
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(
                        Level.FINEST,
                        "An I/O error occurred while trying to parse the Entity-Body: " +
                            "'" + new String(_entityBody) + "'",
                        exception);
                }
            } catch (SAXException exception) {
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.log(
                        Level.FINEST,
                        "A parse error occurred while trying to parse the Entity-Body: " +
                            "'" + new String(_entityBody) + "'",
                        exception);
                }
            }
        }
        return null;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setHeader(final String fieldName, final String fieldValue)
    throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public String toString() {
        Map<String, List<String>> _headerMap = getHeaders();
        StringBuilder _response = new StringBuilder();
        _response.append(_headerMap.get(null)).append("\r\n");
        for (String _fieldName : _headerMap.keySet()) {
            if (_fieldName != null) {
                for (String _fieldValue : _headerMap.get(_fieldName)) {
                    _response.append(_fieldName).append(": ").append(_fieldValue).append("\r\n");
                }
            }
        }
        _response.
            append("\r\n").
            append(getEntityBodyAsString());
        return _response.toString();
    }
}
