package eu.isygoit.factory;

import eu.isygoit.constants.TenantConstants;
import eu.isygoit.model.SenderConfig;
import eu.isygoit.repository.SenderConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * The type Sender factory.
 */
@Slf4j
@Component
public class SenderFactory {

    private final Map<String, JavaMailSenderImpl> senderMap = new HashMap<>();

    @Autowired
    private SenderConfigRepository senderConfigRepository;
    @Qualifier("defaultSender")
    @Autowired
    private JavaMailSender defaultSender;

    /**
     * Remove sender.
     *
     * @param tenant the tenant
     */
    public void removeSender(String tenant) {
        senderMap.remove(tenant);
    }

    /**
     * Gets sender.
     *
     * @param tenant the tenant
     * @return the sender
     */
    public JavaMailSenderImpl getSender(String tenant) {
        // get data from table config
        if (senderMap.containsKey(tenant)) {
            return senderMap.get(tenant);
        }

        Optional<SenderConfig> optional = senderConfigRepository.findFirstByTenantIgnoreCase(tenant);
        if (!optional.isPresent()) {
            optional = senderConfigRepository.findFirstByTenantIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME);
        }

        if (optional.isPresent()) {
            SenderConfig senderConfig = optional.get();
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(senderConfig.getHost());
            int port = Integer.parseInt(senderConfig.getPort());
            mailSender.setPort(port);
            mailSender.setUsername(senderConfig.getUsername());
            mailSender.setPassword(senderConfig.getPassword());
            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", senderConfig.getTransportProtocol());
            props.put("mail.smtp.auth", senderConfig.getSmtpAuth());
            props.put("mail.smtp.starttls.enable", senderConfig.getSmtpStarttlsRequired());
            props.put("mail.debug", senderConfig.getDebug());

            senderMap.put(tenant, mailSender);
            return mailSender;
        } else {
            return (JavaMailSenderImpl) defaultSender;
        }
    }
}
