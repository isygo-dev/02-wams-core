package eu.isygoit.repository;

import eu.isygoit.model.SenderConfig;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;

/**
 * The interface Sender config repository.
 */
public interface SenderConfigRepository extends JpaPagingAndSortingTenantAssignableRepository<SenderConfig, Long> {

}
