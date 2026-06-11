package eu.isygoit.remote.ims;

import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.config.FeignConfig;
import eu.isygoit.dto.data.AnnexDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "annex", path = "/api/v1/private/annex")
public interface AnnexService extends IMappedCrudApi<Long, AnnexDto, AnnexDto> {

    @GetMapping("/by-table-code")
    List<AnnexDto> getAnnexByTableCode(@RequestParam("code") String code);

    @GetMapping("/by-table-code-and-reference")
    List<AnnexDto> getAnnexByTableCodeAndReference(@RequestParam("code") String code,
                                                   @RequestParam("reference") String reference);
}