package eu.isygoit.repository;

import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;

import eu.isygoit.model.Account;

/**
 * The interface Account repository.
 */
public interface AccountRepository extends JpaPagingAndSortingTenantAndCodeAssignableRepository<Account, Long> {

}
