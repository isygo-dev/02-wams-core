package eu.isygoit.service;

import eu.isygoit.com.rest.service.tenancy.ICrudTenantServiceOperations;
import eu.isygoit.model.RegisteredUser;

/**
 * The interface Account service.
 */
public interface IRegisteredUserService extends ICrudTenantServiceOperations<Long, RegisteredUser> {

}
