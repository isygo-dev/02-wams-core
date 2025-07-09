package eu.isygoit.repository;

import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAndCodeAssignableRepository;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;

import eu.isygoit.model.RegistredUser;
import org.springframework.stereotype.Repository;

/**
 * The interface Registred new account repository.
 */
@Repository
public interface RegistredUserRepository extends JpaPagingAndSortingRepository<RegistredUser, Long> {
}
