package eu.isygoit.remote.ims;

import eu.isygoit.api.AnnexServiceApi;
import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.config.FeignConfig;
import eu.isygoit.dto.data.AnnexDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(configuration = FeignConfig.class, name = "identity-service", contextId = "annex", path = "/api/v1/private/annex")
public interface AnnexService extends AnnexServiceApi {

}