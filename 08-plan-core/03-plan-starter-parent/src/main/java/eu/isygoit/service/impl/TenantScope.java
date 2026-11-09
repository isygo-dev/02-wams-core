package eu.isygoit.service.impl;

import eu.isygoit.dto.request.PlanningRequest;
import eu.isygoit.exception.InvalidRequestException;

/**
 * Binds planning requests to the tenant of the authenticated caller. The tenant carried by a client supplied
 * request is never trusted: it must be empty or equal to the caller tenant.
 */
final class TenantScope {
    private TenantScope() {
    }

    static String requireTenant(String tenant) {
        if (tenant == null || tenant.isBlank()) {
            throw new InvalidRequestException("TENANT_REQUIRED", "The caller tenant could not be resolved");
        }
        return tenant;
    }

    /**
     * Returns the request carrying the caller tenant.
     *
     * @throws InvalidRequestException when the request names another tenant
     */
    static PlanningRequest bind(String tenant, PlanningRequest request) {
        requireTenant(tenant);
        if (request == null) {
            return null;
        }
        if (request.tenant() != null && !request.tenant().isBlank() && !tenant.equals(request.tenant())) {
            throw new InvalidRequestException("TENANT_MISMATCH",
                    "The request tenant does not match the authenticated tenant");
        }
        if (tenant.equals(request.tenant())) {
            return request;
        }
        return new PlanningRequest(tenant, request.meta(), request.domain(), request.horizon(), request.slots(),
                request.calendars(), request.setupMatrices(), request.resources(), request.tasks(),
                request.constraints(), request.objectives(), request.solverOptions(), request.initialSolution());
    }
}
