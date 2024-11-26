package eu.isygoit.remote.mms;

import eu.isygoit.api.ChatMessageControllerApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * The interface Mms chat message service.
 */
@FeignClient(configuration = FeignConfig.class, name = "messaging-service", contextId = "mms-chat", path = "/api/v1/private/chat")
public interface MmsChatMessageService extends ChatMessageControllerApi {
}
