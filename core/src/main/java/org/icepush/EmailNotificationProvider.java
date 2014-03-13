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

import static org.icepush.NotificationEvent.NotificationType;
import static org.icepush.NotificationEvent.TargetType;

import java.net.URI;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.icepush.servlet.ServletContextConfiguration;
import org.icepush.util.ExtensionRegistry;

public class EmailNotificationProvider
extends AbstractNotificationProvider
implements NotificationProvider {
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

            String passwordHash = "";
            if (null != password)  {
                passwordHash = String.valueOf(password.hashCode()) + "(hash)";
            }
            log.info("ICEpush Email Notification Provider Properties " +
                    properties + " " + from + " " + user + ":" + passwordHash +
                    "@" + host + ":" + port);

            session = Session.getInstance(properties);
            fromAddress = new InternetAddress(from);
        } catch (AddressException e) {
            throw new RuntimeException(e);
        }
    }

    public void registerWith(OutOfBandNotifier outOfBandNotifier) {
        outOfBandNotifier.registerProvider("mail", this);
    }

    public void send(final String browserID, final String groupName, final PushNotification notification) {
        (new SendMessage(browserID, groupName, notification)).start();
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
        private final String browserID;
        private final String groupName;
        private final PushNotification notification;

        public SendMessage(final String browserID, final String groupName, final PushNotification notification) {
            this.browserID = browserID;
            this.groupName = groupName;
            this.notification = notification;
        }

        public void run() {
            URI destinationURI =
                URI.create(
                    ((LocalPushGroupManager)
                        PushInternalContext.getInstance().getAttribute(PushGroupManager.class.getName())
                    ).getBrowser(browserID).getNotifyBackURI().getURI());

            MimeMessage mimeMessage = new MimeMessage(session);
            try {
                mimeMessage.setFrom(fromAddress);
                InternetAddress address = new InternetAddress(destinationURI.getSchemeSpecificPart());
                mimeMessage.setSubject(notification.getSubject());
                mimeMessage.setText(notification.getDetail());
                Transport transport = session.getTransport(protocol);
                transport.connect(host, port, user, password);
                transport.sendMessage(mimeMessage, new InternetAddress[]{address});
                notificationSent(
                    new NotificationEvent(
                        TargetType.BROWSER_ID, browserID, groupName, NotificationType.CLOUD_PUSH,
                        EmailNotificationProvider.this));
            } catch (MessagingException ex) {
                log.log(Level.WARNING, "Failed to send email message.", ex);
            }
        }
    }
}
