package eu.isygoit.remote.ims;

import eu.isygoit.api.TenantServiceApi;
import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.config.FeignConfig;
import eu.isygoit.dto.data.TenantDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "tenant", path = "/api/v1/private/tenant")
public interface TenantService extends TenantServiceApi {

}