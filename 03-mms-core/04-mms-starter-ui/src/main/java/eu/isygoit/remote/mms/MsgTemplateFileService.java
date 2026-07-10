package eu.isygoit.remote.mms;

import eu.isygoit.api.MsgTemplateFileServiceApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * The type Template controller.
 */
@FeignClient(configuration = FeignConfig.class, name = "messaging-service", contextId = "msg-template-file", path = "/api/v1/private/mail/template")
public interface MsgTemplateFileService extends MsgTemplateFileServiceApi {

}
