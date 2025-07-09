package eu.isygoit.repository;

import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;

import eu.isygoit.model.PEBConfig;

/**
 * The interface Peb config repository.
 */
public interface PEBConfigRepository extends JpaPagingAndSortingTenantAndCodeAssignableRepository<PEBConfig, Long> {

}
