package eu.isygoit.repository;

import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;
import eu.isygoit.model.DigestConfig;

/**
 * The interface Digester config repository.
 */
public interface DigesterConfigRepository extends JpaPagingAndSortingTenantAndCodeAssignableRepository<DigestConfig, Long> {

}
