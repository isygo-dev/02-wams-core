package eu.isygoit.repository;

import eu.isygoit.model.CustomKeyStore;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CustomKeyStoreRepository extends JpaPagingAndSortingTenantAssignableRepository<CustomKeyStore, Long> {

    boolean existsByTenantAndName(String tenant, @NotBlank String keyStoreName);

    List<CustomKeyStore> findByTenantAndIdGreaterThanOrderByIdAsc(String tenant, Long lastId, Pageable pageable);

    List<CustomKeyStore> findByTenantOrderByIdAsc(String tenant, Pageable pageable);

    long countByTenant(String tenant);

    Optional<CustomKeyStore> findByTenantAndId(String tenant, Long keyStoreId);
}
