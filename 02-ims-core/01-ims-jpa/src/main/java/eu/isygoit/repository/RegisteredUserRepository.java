package eu.isygoit.repository;

import eu.isygoit.model.RegisteredUser;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * The interface Registred new account repository.
 */
@Repository
public interface RegisteredUserRepository extends JpaPagingAndSortingTenantAssignableRepository<RegisteredUser, Long> {

    Optional<RegisteredUser> findByEmail(String email);
}
