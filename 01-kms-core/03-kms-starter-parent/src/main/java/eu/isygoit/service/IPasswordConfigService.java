package eu.isygoit.service;

import eu.isygoit.com.rest.service.tenancy.ICrudTenantServiceOperations;
import eu.isygoit.model.PasswordConfig;

/**
 * The interface Password config service.
 */
public interface IPasswordConfigService extends ICrudTenantServiceOperations<Long, PasswordConfig> {
}
