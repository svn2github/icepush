package org.icepush;

import java.util.logging.Logger;

@ExtendedConfigurator
public class SystemConfigurator
implements Configurator {
    private static final Logger LOGGER = Logger.getLogger(SystemConfigurator.class.getName());

    public void configure() {
        // Logger Notification Provider Property Names
        System.setProperty("notify.cloud.logger.enabled.property.name", "smtp.enabled");
        // E-mail Notification Provider Property Names
        System.setProperty("notify.cloud.email.enabled.property.name", "smtp.enabled");
        System.setProperty("notify.cloud.email.security.property.name", "smtp.security");
        System.setProperty("notify.cloud.email.verify.property.name", "smtp.verify-server-certificate");
        System.setProperty("notify.cloud.email.from.property.name", "smtp.from");
        System.setProperty("notify.cloud.email.scheme.property.name", "smtp.scheme");
        System.setProperty("notify.cloud.email.host.property.name", "smtp.host");
        System.setProperty("notify.cloud.email.port.property.name", "smtp.port");
        System.setProperty("notify.cloud.email.userName.property.name", "smtp.user");
        System.setProperty("notify.cloud.email.password.property.name", "smtp.password");
        System.setProperty("notify.cloud.email.debug.property.name", "smtp.debug");
    }
}
