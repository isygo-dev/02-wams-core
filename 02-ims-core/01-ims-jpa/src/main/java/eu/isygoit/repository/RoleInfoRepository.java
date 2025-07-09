package eu.isygoit.repository;

import eu.isygoit.model.RoleInfo;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;

import java.util.Optional;

/**
 * The interface Role info repository.
 */
public interface RoleInfoRepository extends JpaPagingAndSortingTenantAndCodeAssignableRepository<RoleInfo, Long> {

    /**
     * Find by name optional.
     *
     * @param name the name
     * @return the optional
     */
    Optional<RoleInfo> findByName(String name);
}
