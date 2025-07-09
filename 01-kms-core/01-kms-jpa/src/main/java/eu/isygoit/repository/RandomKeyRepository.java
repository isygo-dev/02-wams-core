package eu.isygoit.repository;

import eu.isygoit.model.RandomKey;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;

import java.util.Optional;

/**
 * The interface Random key repository.
 */
public interface RandomKeyRepository extends JpaPagingAndSortingTenantAssignableRepository<RandomKey, Long> {

    /**
     * Find by tenant ignore case and name optional.
     *
     * @param tenant the tenant
     * @param name   the name
     * @return the optional
     */
    Optional<RandomKey> findByTenantIgnoreCaseAndName(String tenant, String name);
}
