package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethod;
import eu.isygoit.dto.request.UpdateAccountRequestDto;
import eu.isygoit.model.Account;

import java.util.Optional;

/**
 * The interface Account service.
 */
public interface IAccountService extends ICrudServiceMethod<Long, Account> {

    /**
     * Check if exists boolean.
     *
     * @param account           the account
     * @param createIfNotExists the create if not exists
     * @return the boolean
     */
    Optional<Account> checkIfExists(Account account, boolean createIfNotExists);
}
