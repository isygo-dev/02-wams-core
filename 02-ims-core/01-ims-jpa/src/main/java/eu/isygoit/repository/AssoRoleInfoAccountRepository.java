package eu.isygoit.repository;

import eu.isygoit.model.AssoRoleInfoAccount;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * The interface Account repository.
 */
public interface AssoRoleInfoAccountRepository extends JpaRepository<AssoRoleInfoAccount, AssoRoleInfoAccount.AssoRoleInfoAccountId> {

    Integer countAllById_RoleInfoCode(String roleInfoCode);
}
