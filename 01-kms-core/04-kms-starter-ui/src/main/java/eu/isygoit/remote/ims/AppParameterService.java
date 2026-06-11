package eu.isygoit.remote.ims;

import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.config.FeignConfig;
import eu.isygoit.dto.data.AppParameterDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "app-parameter", path = "/api/v1/private/appParameter")
public interface AppParameterService extends IMappedCrudApi<Long, AppParameterDto, AppParameterDto> {

    @GetMapping("/value")
    String getValueByTenantAndName(@RequestParam("tenant") String tenant,
                                   @RequestParam("name") String name,
                                   @RequestParam(value = "allowDefault", required = false) Boolean allowDefault,
                                   @RequestParam(value = "defaultValue", required = false) String defaultValue);

    @GetMapping("/technical-admin-email")
    String getTechnicalAdminEmail();
}