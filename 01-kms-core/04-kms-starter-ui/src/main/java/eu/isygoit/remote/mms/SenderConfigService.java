package eu.isygoit.remote.mms;

import eu.isygoit.api.SenderConfigServiceApi;
import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.config.FeignConfig;
import eu.isygoit.dto.data.SenderConfigDto;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * The type Sender config controller.
 */
@FeignClient(configuration = FeignConfig.class, name = "messaging-service", contextId = "sender-config", path = "/api/v1/private/config/sender")
public interface SenderConfigService extends SenderConfigServiceApi {

}
