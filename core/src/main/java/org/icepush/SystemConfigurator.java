package org.icepush;

import java.util.logging.Logger;

@ExtendedConfigurator
public class SystemConfigurator
implements Configurator {
    private static final Logger LOGGER = Logger.getLogger(SystemConfigurator.class.getName());

    public void configure() {
        // Logger Notification Provider Property Names
        System.setProperty(
            "com.icesoft.notify.cloud.logger.enabled.property.name", "smtp.enabled"
        );
        // E-mail Notification Provider Property Names
        System.setProperty(
            "com.icesoft.notify.cloud.email.enabled.property.name", "smtp.enabled"
        );
        System.setProperty(
            "com.icesoft.notify.cloud.email.security.property.name", "smtp.security"
        );
        System.setProperty(
            "com.icesoft.notify.cloud.email.verify.property.name", "smtp.verify-server-certificate"
        );
        System.setProperty(
            "com.icesoft.notify.cloud.email.from.property.name", "smtp.from"
        );
        System.setProperty(
            "com.icesoft.notify.cloud.email.scheme.property.name", "smtp.scheme"
        );
        System.setProperty(
            "com.icesoft.notify.cloud.email.host.property.name", "smtp.host"
        );
        System.setProperty(
            "com.icesoft.notify.cloud.email.port.property.name", "smtp.port"
        );
        System.setProperty(
            "com.icesoft.notify.cloud.email.userName.property.name", "smtp.user"
        );
        System.setProperty(
            "com.icesoft.notify.cloud.email.password.property.name", "smtp.password"
        );
        System.setProperty(
            "com.icesoft.notify.cloud.email.debug.property.name", "smtp.debug"
        );
    }
}
