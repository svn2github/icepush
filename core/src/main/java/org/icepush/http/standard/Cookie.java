/*
 * Version: MPL 1.1
 *
 * "The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is ICEfaces 1.5 open source software code, released
 * November 5, 2006. The Initial Developer of the Original Code is ICEsoft
 * Technologies Canada, Corp. Portions created by ICEsoft are Copyright (C)
 * 2004-2010 ICEsoft Technologies Canada, Corp. All Rights Reserved.
 *
 * Contributor(s): _____________________.
 *
 */

package org.icepush.http.standard;

import org.icepush.http.Request;
import org.icepush.http.Response;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Cookie {
    private final static DateFormat DATE_FORMAT = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss zzz");

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private String name;
    private String value;
    private Date expiration;
    private String path;
    private String domain;

    public Cookie(String name, String value) {
        this(name, value, null, "/", null);
    }

    public Cookie(String name, String value, Date expiration, String path, String domain) {
        this.name = name;
        this.value = value;
        this.expiration = expiration;
        this.path = path;
        this.domain = domain;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String asString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(name);
        buffer.append("=");
        buffer.append(value);
        if (expiration != null) {
            buffer.append("; expires=");
            buffer.append(DATE_FORMAT.format(expiration));
        }
        if (path != null) {
            buffer.append("; path=");
            buffer.append(path);
        }
        if (domain != null) {
            buffer.append("; domain=");
            buffer.append(domain);
        }
        return buffer.toString();
    }

    public void writeCookie(Response response) {
        response.setHeader("Set-Cookie", asString());
    }

    public static Cookie readCookie(Request request, String cookieName) {
        String cookies = request.getHeader("Cookie");
        if (null == cookies)  {
            return null;
        }
        String[] cookieString = cookies.split("; ");
        for (int i = 0; i < cookieString.length; i++) {
            String[] nameValue = cookieString[i].split("=");
            String name = nameValue[0];
            if (cookieName.equals(name)) {
                return new Cookie(name, nameValue.length > 1 ? nameValue[1] : "");
            }
        }

        return null;
    }
}
