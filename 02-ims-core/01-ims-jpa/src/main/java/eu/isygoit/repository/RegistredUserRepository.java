package eu.isygoit.repository;

import eu.isygoit.model.RegisteredUser;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;
import org.springframework.stereotype.Repository;

/**
 * The interface Registred new account repository.
 */
@Repository
public interface RegistredUserRepository extends JpaPagingAndSortingTenantAssignableRepository<RegisteredUser, Long> {
}
