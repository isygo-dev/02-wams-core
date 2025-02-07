package eu.isygoit.factory;

import eu.isygoit.constants.DomainConstants;
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

    private final SenderConfigRepository senderConfigRepository;
    private final JavaMailSender defaultSender;

    @Autowired
    public SenderFactory(SenderConfigRepository senderConfigRepository, @Qualifier("defaultSender") JavaMailSender defaultSender) {
        this.senderConfigRepository = senderConfigRepository;
        this.defaultSender = defaultSender;
    }

    /**
     * Remove sender.
     *
     * @param domain the domain
     */
    public void removeSender(String domain) {
        senderMap.remove(domain);
    }

    /**
     * Gets sender.
     *
     * @param domain the domain
     * @return the sender
     */
    public JavaMailSenderImpl getSender(String domain) {
        // get data from table config
        if (senderMap.containsKey(domain)) {
            return senderMap.get(domain);
        }

        Optional<SenderConfig> optional = senderConfigRepository.findFirstByDomainIgnoreCase(domain);
        if (!optional.isPresent()) {
            optional = senderConfigRepository.findFirstByDomainIgnoreCase(DomainConstants.DEFAULT_DOMAIN_NAME);
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

            senderMap.put(domain, mailSender);
            return mailSender;
        } else {
            return (JavaMailSenderImpl) defaultSender;
        }
    }
}
