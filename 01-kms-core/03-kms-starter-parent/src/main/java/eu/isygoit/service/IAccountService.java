package eu.isygoit.service;

import eu.isygoit.com.rest.service.tenancy.ICrudTenantServiceOperations;
import eu.isygoit.model.Account;

/**
 * The interface Account service.
 */
public interface IAccountService extends ICrudTenantServiceOperations<Long, Account> {

    /**
     * Check if exists boolean.
     *
     * @param account           the account
     * @param createIfNotExists the create if not exists
     * @return the boolean
     */
    boolean checkIfExists(Account account, boolean createIfNotExists);
}
