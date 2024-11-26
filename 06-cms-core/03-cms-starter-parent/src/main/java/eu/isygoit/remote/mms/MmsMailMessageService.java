package eu.isygoit.remote.mms;

import eu.isygoit.api.MailMessageControllerApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * The interface Mms mail message service.
 */
@FeignClient(configuration = FeignConfig.class, name = "messaging-service", contextId = "mms-email", path = "/api/v1/private/mail")
public interface MmsMailMessageService extends MailMessageControllerApi {
}
