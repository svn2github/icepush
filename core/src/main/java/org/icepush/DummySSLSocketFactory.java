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

package org.icepush;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.cert.X509Certificate;

public class DummySSLSocketFactory extends SSLSocketFactory {
    private SSLSocketFactory factory;

    public DummySSLSocketFactory() {
        try {
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, new TrustManager[]{new DummyTrustManager()}, null);
            factory = (SSLSocketFactory) sslcontext.getSocketFactory();
        } catch (Exception ex) {
            // ignore
        }
    }

    public static SocketFactory getDefault() {
        return new DummySSLSocketFactory();
    }

    public Socket createSocket() throws IOException {
        return factory.createSocket();
    }

    public Socket createSocket(Socket socket, String s, int i, boolean flag)
            throws IOException {
        return factory.createSocket(socket, s, i, flag);
    }

    public Socket createSocket(InetAddress inaddr, int i, InetAddress inaddr1, int j) throws IOException {
        return factory.createSocket(inaddr, i, inaddr1, j);
    }

    public Socket createSocket(InetAddress inaddr, int i) throws IOException {
        return factory.createSocket(inaddr, i);
    }

    public Socket createSocket(String s, int i, InetAddress inaddr, int j)
            throws IOException {
        return factory.createSocket(s, i, inaddr, j);
    }

    public Socket createSocket(String s, int i) throws IOException {
        return factory.createSocket(s, i);
    }

    public String[] getDefaultCipherSuites() {
        return factory.getDefaultCipherSuites();

    }

    public String[] getSupportedCipherSuites() {
        return factory.getSupportedCipherSuites();
    }

    private class DummyTrustManager implements X509TrustManager {

        public void checkClientTrusted(X509Certificate[] cert, String authType) {
            // everything is trusted
        }

        public void checkServerTrusted(X509Certificate[] cert, String authType) {
            // everything is trusted
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}