package eu.isygoit.repository;

import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.model.KmsKey;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for KMS Key entity
 */
@Repository
public interface KmsKeyRepository extends JpaRepository<KmsKey, Long> {

    Optional<KmsKey> findByTenantAndKeyId(String tenant, String keyId);

    List<KmsKey> findByTenantAndKeyStatusAndRotationEnabled(String tenant, IEnumKeyStatus.Types status, boolean rotationEnabled);

    Page<KmsKey> findByTenant(String tenant, Pageable pageable);

    List<KmsKey> findByTenantAndKeyStatusAndDeletionDateBefore(String tenant, IEnumKeyStatus.Types status, LocalDateTime deletionDateBefore);

    boolean existsByTenantAndPrimaryKeyIdAndRegion(String tenant, String keyId, @NotBlank String replicaRegion);

    @Modifying
    void deleteByTenantAndKeyId(@Param("tenant") String tenant, @Param("keyId") String keyId);
}

