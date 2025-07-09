package eu.isygoit.repository;

import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;

import eu.isygoit.model.AppParameter;
import org.springframework.stereotype.Repository;

import java.util.Optional;


/**
 * The interface App parameter repository.
 */
@Repository
public interface AppParameterRepository extends JpaPagingAndSortingTenantAssignableRepository<AppParameter, Long> {

    /**
     * Find by tenant ignore case and name ignore case optional.
     *
     * @param tenant the tenant
     * @param name   the name
     * @return the optional
     */
    Optional<AppParameter> findByTenantIgnoreCaseAndNameIgnoreCase(String tenant, String name);
}
