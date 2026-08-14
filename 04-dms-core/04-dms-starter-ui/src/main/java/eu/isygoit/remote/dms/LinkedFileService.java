package eu.isygoit.remote.dms;

import eu.isygoit.api.CategoryServiceApi;
import eu.isygoit.api.LinkedFileApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(configuration = FeignConfig.class, name = "document-service", contextId = "linked-file", path = "/api/v1/private/linked-files")
public interface LinkedFileService extends LinkedFileApi {

}