package eu.isygoit.remote.plan;

import eu.isygoit.api.PlanJobServiceApi;
import eu.isygoit.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * Feign client of the planning service jobs API (CRUD, paging and criteria filter from the stack, plus submit,
 * validate, request, result, export, logs, stop, cancel and dashboard).
 */
@FeignClient(
        configuration = FeignConfig.class,
        name = "plan-service",
        contextId = "plan-jobs",
        path = "/api/v1/private/plan/job")
public interface PlanFeignService extends PlanJobServiceApi {
}
