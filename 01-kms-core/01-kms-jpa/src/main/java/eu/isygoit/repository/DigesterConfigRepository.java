package eu.isygoit.repository;

import eu.isygoit.model.DigestConfig;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;

/**
 * The interface Digester config repository.
 */
public interface DigesterConfigRepository extends JpaPagingAndSortingTenantAndCodeAssignableRepository<DigestConfig, Long> {

}
