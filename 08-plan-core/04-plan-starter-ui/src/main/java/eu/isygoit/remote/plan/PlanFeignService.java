package eu.isygoit.remote.plan;

import eu.isygoit.api.PlanServiceApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(
        configuration = FeignConfig.class,
        name = "plan-service",
        contextId = "plan-jobs",
        path = "/api/v1/private/plans")
public interface PlanFeignService extends PlanServiceApi {
}
