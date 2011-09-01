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

import javax.mail.Message;
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
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmailNotificationProvider implements NotificationProvider {
    private static final Logger log = Logger.getLogger(EmailNotificationProvider.class.getName());
    private Session session;
    private InternetAddress fromAddress;

    public EmailNotificationProvider(String host, int port, String from, String user, String password) {
        try {
            Properties configuration = new Properties();
            configuration.setProperty("mail.smtp.host", host);
            configuration.setProperty("mail.smtp.port", String.valueOf(port));
            configuration.setProperty("mail.smtp.from", from);
            configuration.setProperty("mail.smtp.user", user);
            configuration.setProperty("mail.smtp.password", password);

            session = Session.getDefaultInstance(configuration, null);
            fromAddress = new InternetAddress(from);
        } catch (AddressException e) {
            throw new RuntimeException(e);
        }
    }

    public void registerWith(OutOfBandNotifier outOfBandNotifier) {
        outOfBandNotifier.registerProvider("mail", this);
    }

    public void send(String uri, PushNotification notification) {
        URI destinationURI = URI.create(uri);

        MimeMessage mimeMessage = new MimeMessage(session);
        try {
            mimeMessage.setFrom(fromAddress);
            mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(destinationURI.getSchemeSpecificPart()));
            mimeMessage.setSubject(notification.getSubject());
            mimeMessage.setText(notification.getDetail());
            Transport.send(mimeMessage);
        } catch (MessagingException ex) {
            log.log(Level.WARNING, "Failed to send email message.", ex);
        }
    }

    public static class AutoRegister implements ServletContextListener {
        private static final Logger LOGGER = Logger.getLogger(AutoRegister.class.getName());

        public void contextInitialized(ServletContextEvent servletContextEvent) {
            try {
                Class.forName("javax.mail.Message");

                //mail library present, start using it
                ServletContext servletContext = servletContextEvent.getServletContext();
                Configuration configuration = new ServletContextConfiguration("smtp", servletContext);
                String host = configuration.getAttribute("host", "labs.icesoft.com");
                String from = configuration.getAttribute("from", "tomcat@icesoft.com");
                int port = configuration.getAttributeAsInteger("port", 25);
                String user = configuration.getAttribute("user", "");
                String password = configuration.getAttribute("password", "");
                ExtensionRegistry.addExtension(servletContext, 10, NotificationProvider.class.getName(), new EmailNotificationProvider(host, port, from, user, password));
            } catch (ClassNotFoundException e) {
                LOGGER.info("Could not setup the email notification provider, the mail.jar library is missing.");
            }
        }

        public void contextDestroyed(ServletContextEvent servletContextEvent) {
        }
    }
}
