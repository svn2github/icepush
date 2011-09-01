package org.icepush.client;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CookieHandler
extends java.net.CookieHandler {
    private static final Logger LOGGER = Logger.getLogger(CookieHandler.class.getName());

    private final Map<URI, List<Cookie>> cookieMap = new HashMap<URI, List<Cookie>>();

    public void clear() {
        cookieMap.clear();
    }

    public Map<String, List<String>> get(final URI requestURI, final Map<String, List<String>> requestHeaders)
    throws IOException {
        Map<String, List<String>> _cookieMap = new HashMap<String, List<String>>();
        String _requestURI = requestURI.toString();
        List<String> _cookieList = new ArrayList<String>();
        for (Map.Entry<URI, List<Cookie>> _entry : cookieMap.entrySet()) {
            if (_requestURI.startsWith(_entry.getKey().toString())) {
                for (Cookie _cookie : _entry.getValue()) {
                    // todo: Change logger level to FINEST.
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.log(Level.INFO, "Adding Cookie: " + _cookie.getName() + "=" + _cookie.getValue());
                    }
                    _cookieList.add(_cookie.getName() + "=" + _cookie.getValue());
                }
            }
        }
        _cookieMap.put("Cookie", _cookieList);
        return Collections.unmodifiableMap(_cookieMap);
    }

    public void put(final URI requestURI, Map<String, List<String>> responseHeaders)
    throws IOException {
        for (Map.Entry<String, List<String>> _entry : responseHeaders.entrySet()) {
            String _key = _entry.getKey();
            if (_key != null && _key.equalsIgnoreCase("Set-Cookie")) {
                for (String _value : _entry.getValue()) {
                    Cookie _cookie = new Cookie(_value);
                    // todo: Change logger level to FINEST.
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.log(
                            Level.INFO,
                            "Caching Cookie: " +
                                _cookie.getName() + "=" + _cookie.getValue() + "; " + _cookie.getPath());
                    }
                    URI _resolvedURI = requestURI.resolve(_cookie.getPath());
                    if (cookieMap.containsKey(_resolvedURI)) {
                        cookieMap.get(_resolvedURI).add(_cookie);
                    } else {
                        List<Cookie> _cookieList = new ArrayList<Cookie>();
                        _cookieList.add(_cookie);
                        cookieMap.put(_resolvedURI, _cookieList);
                    }
                }
            }
        }
    }
}
