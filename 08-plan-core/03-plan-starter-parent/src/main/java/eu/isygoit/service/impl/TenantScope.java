package eu.isygoit.service.impl;

import eu.isygoit.exception.InvalidRequestException;

/**
 * Resolves the tenant scope from the authenticated caller.
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
}
