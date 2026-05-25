package eu.isygoit.repository;

import eu.isygoit.model.KmsCustomKeyStore;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CustomKeyStoreRepository extends JpaPagingAndSortingTenantAssignableRepository<KmsCustomKeyStore, Long> {

    boolean existsByTenantAndName(String tenant, @NotBlank String keyStoreName);

    Page<KmsCustomKeyStore> findByTenantAndIdGreaterThanOrderByIdAsc(String tenant, Long lastId, Pageable pageable);

    Page<KmsCustomKeyStore> findByTenantOrderByIdAsc(String tenant, Pageable pageable);

    long countByTenant(String tenant);

    Optional<KmsCustomKeyStore> findByTenantAndId(String tenant, Long keyStoreId);
}
