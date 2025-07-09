package eu.isygoit.service;

import eu.isygoit.com.rest.service.tenancy.ICrudTenantServiceMethods;
import eu.isygoit.dto.data.MailOptionsDto;
import eu.isygoit.model.MailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The interface Mail message service.
 */
public interface IMailMessageService extends ICrudTenantServiceMethods<UUID, MailMessage> {
    /**
     * The constant encodingOptions.
     */
    String encodingOptions = "text/html; charset=UTF-8";

    /**
     * Send mail boolean.
     *
     * @param mailSender      the mail sender
     * @param mailMessageData the mail message data
     * @param returnDelivered the return delivered
     * @param returnRead      the return read
     * @param resources       the resources
     * @return the boolean
     */
    boolean sendMail(JavaMailSenderImpl mailSender, MailMessage mailMessageData, boolean returnDelivered, boolean returnRead, Map<String, File> resources);

    /**
     * Send mail boolean.
     *
     * @param senderDomainName the sender tenant name
     * @param mailMessageData  the mail message data
     * @param options          the options
     * @param resources        the resources
     * @return the boolean
     */
    boolean sendMail(String senderDomainName, MailMessage mailMessageData, MailOptionsDto options, Map<String, File> resources);

    /**
     * Multi part file to resource map.
     *
     * @param senderDomainName the sender tenant name
     * @param resources        the resources
     * @return the map
     */
    Map<String, File> multiPartFileToResource(String senderDomainName, List<MultipartFile> resources);
}
