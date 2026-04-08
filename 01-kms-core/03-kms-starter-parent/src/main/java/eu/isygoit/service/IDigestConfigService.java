package eu.isygoit.service;

import eu.isygoit.com.rest.service.tenancy.ICrudTenantServiceOperations;
import eu.isygoit.model.DigestConfig;

/**
 * The interface Digest config service.
 */
public interface IDigestConfigService extends ICrudTenantServiceOperations<Long, DigestConfig> {
}
