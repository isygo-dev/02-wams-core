package eu.isygoit.remote.ims;

import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.config.FeignConfig;
import eu.isygoit.dto.data.TenantDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "tenant", path = "/api/v1/private/tenant")
public interface TenantService extends IMappedCrudApi<Long, TenantDto, TenantDto> {

    @PutMapping("/update-admin-status")
    TenantDto updateAdminStatus(@RequestParam("id") Long id,
                                @RequestParam("newStatus") IEnumEnabledBinaryStatus.Types newStatus);

    @GetMapping("/all-names")
    List<String> getAllTenantNames();

    @GetMapping("/by-name")
    TenantDto getByName();

    @PutMapping("/update-social-link")
    TenantDto updateSocialLink(@RequestParam("id") Long id,
                               @RequestParam("social") String social,
                               @RequestParam("link") String link);
}