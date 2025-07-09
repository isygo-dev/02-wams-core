package eu.isygoit.repository;

import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;

import eu.isygoit.model.AssoRoleInfoAccount;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * The interface Account repository.
 */
public interface AssoRoleInfoAccountRepository extends JpaRepository<AssoRoleInfoAccount, AssoRoleInfoAccount.AssoRoleInfoAccountId> {

    Integer countAllById_RoleInfoCode(String roleInfoCode);
}
