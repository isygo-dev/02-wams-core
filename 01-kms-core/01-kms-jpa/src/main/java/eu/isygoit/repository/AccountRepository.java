package eu.isygoit.repository;

import eu.isygoit.model.Account;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;

/**
 * The interface Account repository.
 */
public interface AccountRepository extends JpaPagingAndSortingTenantAndCodeAssignableRepository<Account, Long> {

}
