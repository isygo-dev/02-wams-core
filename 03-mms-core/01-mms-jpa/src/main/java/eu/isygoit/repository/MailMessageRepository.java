package eu.isygoit.repository;

import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;

import eu.isygoit.annotation.IgnoreRepository;
import eu.isygoit.model.MailMessage;
import org.springframework.data.cassandra.repository.AllowFiltering;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.List;
import java.util.UUID;

/**
 * The interface Mail message repository.
 */
@IgnoreRepository
public interface MailMessageRepository extends CassandraRepository<MailMessage, UUID> {

    /**
     * Find all by tenant ignore case list.
     *
     * @param tenant the tenant
     * @return the list
     */
    @AllowFiltering
    List<MailMessage> findAllByTenantIgnoreCase(String tenant);
}
