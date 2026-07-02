package eu.isygoit.remote.dms;

import eu.isygoit.api.CategoryServiceApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(configuration = FeignConfig.class, name = "document-service", contextId = "category", path = "/api/v1/private/category")
public interface CategoryService extends CategoryServiceApi {

}