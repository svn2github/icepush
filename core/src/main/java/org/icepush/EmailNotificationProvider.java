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

import org.icepush.servlet.ServletContextConfiguration;
import org.icepush.util.ExtensionRegistry;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.net.URI;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmailNotificationProvider implements NotificationProvider {
    private static final Logger log = Logger.getLogger(EmailNotificationProvider.class.getName());
    private static final String SECURITY_NONE = "NONE";
    private static final String SECURITY_SSL = "SSL";
    private static final String SECURITY_TLS = "TLS";
    private Session session;
    private InternetAddress fromAddress;
    private String user;
    private String password;
    private String host;
    private int port;
    private String protocol;

    public EmailNotificationProvider(Configuration configuration) {
        host = configuration.getAttribute("host", "localhost");
        String from = configuration.getAttribute("from", "nobody@localhost.com");
        user = configuration.getAttribute("user", "");
        password = configuration.getAttribute("password", "");
        boolean verifyServerCertificate = configuration.getAttributeAsBoolean("verify-server-certificate", false);
        boolean debugSMTP = configuration.getAttributeAsBoolean("debug", false);

        String securityType = configuration.getAttribute("security", SECURITY_NONE);
        boolean secured = SECURITY_TLS.equals(securityType) || SECURITY_SSL.equals(securityType);
        int defaultPort = secured ? 465 : 25;
        port = configuration.getAttributeAsInteger("port", defaultPort);
        protocol = secured ? "smtps" : "smtp";

        try {
            Properties properties = new Properties();
            properties.setProperty("mail.smtp.auth", "true");
            //debug
            if (debugSMTP) {
                properties.setProperty("mail.debug", "true");
            }
            if (SECURITY_TLS.equals(securityType)) {
                properties.put("mail.smtp.starttls.enable", "true");
                properties.put("mail.smtp.ssl.protocols", "TLSv1");
            }
            if (SECURITY_SSL.equals(securityType)) {
                properties.put("mail.smtp.ssl.enable", "true");
                properties.put("mail.smtp.ssl.protocols", "SSLv3");
            }
            if (!verifyServerCertificate) {
                properties.setProperty("mail.smtps.socketFactory.class", "org.icepush.DummySSLSocketFactory");
                properties.setProperty("mail.smtps.socketFactory.fallback", "false");
            }

            session = Session.getInstance(properties);
            fromAddress = new InternetAddress(from);
        } catch (AddressException e) {
            throw new RuntimeException(e);
        }
    }

    public void registerWith(OutOfBandNotifier outOfBandNotifier) {
        outOfBandNotifier.registerProvider("mail", this);
    }

    public void send(final String uri, final PushNotification notification) {
        (new SendMessage(uri, notification)).start();
    }

    public static class AutoRegister implements ServletContextListener {
        private static final Logger LOGGER = Logger.getLogger(AutoRegister.class.getName());

        public void contextInitialized(ServletContextEvent servletContextEvent) {
            try {
                Class.forName("javax.mail.Message");

                //mail library present, start using it
                ServletContext servletContext = servletContextEvent.getServletContext();
                Configuration configuration = new ServletContextConfiguration("smtp", servletContext);
                ExtensionRegistry.addExtension(servletContext, 10, NotificationProvider.class.getName(), new EmailNotificationProvider(configuration));
                LOGGER.info("ICEpush Email Notification Provider Registered.");
            } catch (ClassNotFoundException e) {
                LOGGER.fine("Could not setup the email notification provider, the mail.jar library is missing.");
            }
        }

        public void contextDestroyed(ServletContextEvent servletContextEvent) {
        }
    }

    private class SendMessage extends Thread {
        private final String uri;
        private final PushNotification notification;

        public SendMessage(String uri, PushNotification notification) {
            this.uri = uri;
            this.notification = notification;
        }

        public void run() {
            URI destinationURI = URI.create(uri);

            MimeMessage mimeMessage = new MimeMessage(session);
            try {
                mimeMessage.setFrom(fromAddress);
                InternetAddress address = new InternetAddress(destinationURI.getSchemeSpecificPart());
                mimeMessage.setSubject(notification.getSubject());
                mimeMessage.setText(notification.getDetail());
                Transport transport = session.getTransport(protocol);
                transport.connect(host, port, user, password);
                transport.sendMessage(mimeMessage, new InternetAddress[]{address});
            } catch (MessagingException ex) {
                log.log(Level.WARNING, "Failed to send email message.", ex);
            }
        }
    }
}
