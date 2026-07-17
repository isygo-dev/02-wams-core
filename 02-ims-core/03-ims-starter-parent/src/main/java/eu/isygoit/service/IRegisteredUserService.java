package eu.isygoit.service;

import eu.isygoit.com.rest.service.tenancy.ICrudTenantServiceOperations;
import eu.isygoit.dto.request.CreateAccountFromRegisteredRequestDto;
import eu.isygoit.model.Account;
import eu.isygoit.model.RegisteredUser;

/**
 * The interface Account service.
 */
public interface IRegisteredUserService extends ICrudTenantServiceOperations<Long, RegisteredUser> {

    Account createAccountFromRegistered(String senderTenant, CreateAccountFromRegisteredRequestDto request);
}
