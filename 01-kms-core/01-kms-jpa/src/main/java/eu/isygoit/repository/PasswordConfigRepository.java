package eu.isygoit.repository;

import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;

import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.model.PasswordConfig;

import java.util.Optional;

/**
 * The interface Password config repository.
 */
public interface PasswordConfigRepository extends JpaPagingAndSortingTenantAndCodeAssignableRepository<PasswordConfig, Long> {

    /**
     * Find by tenant ignore case and type optional.
     *
     * @param tenant the tenant
     * @param type   the type
     * @return the optional
     */
    Optional<PasswordConfig> findByTenantIgnoreCaseAndType(String tenant, IEnumAuth.Types type);
}
