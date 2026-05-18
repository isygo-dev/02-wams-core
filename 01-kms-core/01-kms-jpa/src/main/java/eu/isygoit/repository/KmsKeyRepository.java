package eu.isygoit.repository;

import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.model.KmsKey;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for KMS Key entity
 */
@Repository
public interface KmsKeyRepository extends JpaRepository<KmsKey, Long> {

    /**
     * Find key by tenant and keyId
     */
    Optional<KmsKey> findByTenantAndKeyId(String tenant, String keyId);

    /**
     * Find key by tenant and keyAlias
     */
    Optional<KmsKey> findByTenantAndPrimaryKeyAlias(String tenant, String keyAlias);

    /**
     * Find key by tenant and keyWrn
     */
    Optional<KmsKey> findByTenantAndKeyWrn(String tenant, String keyWrn);

    /**
     * List all keys for a tenant with pagination
     */
    Page<KmsKey> findByTenant(String tenant, Pageable pageable);

    /**
     * List all keys for a tenant with specific status
     */
    Page<KmsKey> findByTenantAndKeyStatus(String tenant, IEnumKeyStatus.Types status, Pageable pageable);

    /**
     * Find active keys for a tenant
     */
    @Query("SELECT k FROM KmsKey k WHERE k.tenant = :tenant AND k.keyStatus = 'ENABLED'")
    List<KmsKey> findActiveKeysByTenant(@Param("tenant") String tenant);

    boolean existsByTenantAndPrimaryKeyIdAndRegion(String tenant, String keyId, @NotBlank String replicaRegion);
}

