package eu.isygoit.repository;

import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.model.KmsKeyVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for KMS Key Version entity
 */
@Repository
public interface KmsKeyVersionRepository extends JpaRepository<KmsKeyVersion, Long> {

    /**
     * List active versions of a key
     */
    Optional<KmsKeyVersion> findFirstByTenantAndKeyIdAndKeyStatusOrderByCreateDateDesc(
            String tenant,
            String keyId,
            IEnumKeyStatus.Types keyStatus
    );

    /**
     * List all versions for a key with pagination
     */
    Page<KmsKeyVersion> findByTenantAndKeyId(String tenant, String keyId, Pageable pageable);

    Page<KmsKeyVersion> findByTenantAndKeyIdAndCreateDateIsNotNull(String tenant, String keyId, Pageable pageable);

    void deleteByTenantAndKeyId(String tenant, String keyId);

    Optional<KmsKeyVersion> findByTenantAndKeyIdAndVersionId(String tenant, String keyId, String versionId);

    List<KmsKeyVersion> findByTenantAndKeyIdOrderByCreateDateDesc(String tenant, String keyId);
}

