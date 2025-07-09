package eu.isygoit.repository;

import eu.isygoit.model.PEBConfig;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;

/**
 * The interface Peb config repository.
 */
public interface PEBConfigRepository extends JpaPagingAndSortingTenantAndCodeAssignableRepository<PEBConfig, Long> {

}
