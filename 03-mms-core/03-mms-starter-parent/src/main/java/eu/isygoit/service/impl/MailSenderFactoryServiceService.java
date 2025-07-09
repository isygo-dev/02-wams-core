package eu.isygoit.service.impl;

import eu.isygoit.constants.TenantConstants;
import eu.isygoit.exception.SenderConfigNotFoundException;
import eu.isygoit.model.SenderConfig;
import eu.isygoit.repository.SenderConfigRepository;
import eu.isygoit.service.IMailSenderFactoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * The type Mail sender factory service service.
 */
@Slf4j
@Service
@Transactional
public class MailSenderFactoryServiceService implements IMailSenderFactoryService {

    @Autowired
    private SenderConfigRepository senderConfigRepository;

    @Autowired
    private Map<String, MailSender> mailSenders;

    /**
     * Sender from config mail sender.
     *
     * @param tenant the tenant
     * @return the mail sender
     */
    public MailSender senderFromConfig(String tenant) {
        Optional<SenderConfig> optional = senderConfigRepository.findFirstByTenantIgnoreCase(tenant);
        if (!optional.isPresent()) {
            optional = senderConfigRepository.findFirstByTenantIgnoreCase(TenantConstants.DEFAULT_TENANT_NAME);
        }

        if (optional.isPresent()) {
            SenderConfig senderConfig = optional.get();

            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(senderConfig.getHost());
            mailSender.setPort(Integer.valueOf(senderConfig.getPort()));
            mailSender.setUsername(senderConfig.getUsername());
            mailSender.setPassword(senderConfig.getPassword());
            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", senderConfig.getTransportProtocol());
            props.put("mail.smtp.auth", senderConfig.getSmtpAuth());
            props.put("mail.smtp.starttls.enable", senderConfig.getSmtpStarttlsEnable());
            props.put("mail.smtp.starttls.required", senderConfig.getSmtpStarttlsRequired());
            props.put("mail.debug", senderConfig.getDebug());
            return mailSender;
        }

        throw new SenderConfigNotFoundException("for tenant: " + tenant);
    }

    /**
     * Gets sender.
     *
     * @param tenant the tenant
     * @return the sender
     */
    public MailSender getSender(String tenant) {
        if (mailSenders.containsKey(tenant)) {
            return mailSenders.get(tenant);
        }

        MailSender mailSender = senderFromConfig(tenant);
        mailSenders.put(tenant, mailSender);
        return mailSender;
    }
}
