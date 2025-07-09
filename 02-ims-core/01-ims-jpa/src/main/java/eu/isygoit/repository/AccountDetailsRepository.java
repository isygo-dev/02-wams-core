package eu.isygoit.repository;

import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;

import eu.isygoit.model.AccountDetails;

/**
 * The interface Account details repository.
 */
public interface AccountDetailsRepository extends JpaPagingAndSortingRepository<AccountDetails, Long> {
}
