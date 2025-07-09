package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethods;
import eu.isygoit.com.rest.service.tenancy.ICrudTenantServiceMethods;
import eu.isygoit.model.DigestConfig;

/**
 * The interface Digest config service.
 */
public interface IDigestConfigService extends ICrudTenantServiceMethods<Long, DigestConfig> {
}
