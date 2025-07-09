package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethods;
import eu.isygoit.com.rest.service.tenancy.ICrudTenantServiceMethods;
import eu.isygoit.model.PEBConfig;

/**
 * The interface Ipeb config service.
 */
public interface IPEBConfigService extends ICrudTenantServiceMethods<Long, PEBConfig> {
}
