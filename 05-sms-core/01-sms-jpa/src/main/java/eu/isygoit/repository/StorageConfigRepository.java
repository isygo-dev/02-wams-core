package eu.isygoit.repository;

import eu.isygoit.model.StorageConfig;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;

/**
 * The interface Storage config repository.
 */
public interface StorageConfigRepository extends JpaPagingAndSortingTenantAssignableRepository<StorageConfig, Long> {

}
