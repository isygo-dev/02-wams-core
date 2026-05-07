package eu.isygoit.repository;

import eu.isygoit.model.CustomKeyStore;
import eu.isygoit.repository.tenancy.JpaPagingAndSortingTenantAssignableRepository;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Optional;

public interface CustomKeyStoreRepository extends JpaPagingAndSortingTenantAssignableRepository<CustomKeyStore, Long> {

    boolean existsByTenantAndName(String tenant, @NotBlank String keyStoreName);

    List<CustomKeyStore> findByTenantAndIdGreaterThanOrderByIdAsc(String tenant, Long lastId, int pageSize);

    List<CustomKeyStore> findByTenantOrderByIdAsc(String tenant, int pageSize);

    long countByTenant(String tenant);

    Optional<Object> findByTenantAndCustomKeyStoreId(String tenant, String keyStoreId);
}
