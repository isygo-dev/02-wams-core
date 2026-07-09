package eu.isygoit.repository;

import eu.isygoit.model.SenderConfig;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;

/**
 * The interface Sender config repository.
 */
public interface SenderConfigRepository extends JpaPagingAndSortingTenantAndCodeAssignableRepository<SenderConfig, Long> {

}
