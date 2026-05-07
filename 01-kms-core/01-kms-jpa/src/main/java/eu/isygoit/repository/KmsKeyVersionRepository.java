package eu.isygoit.repository;

import eu.isygoit.model.KmsKeyVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for KMS Key Version entity
 */
@Repository
public interface KmsKeyVersionRepository extends JpaRepository<KmsKeyVersion, Long> {

    /**
     * Find key version by tenant, keyId and versionId
     */
    Optional<KmsKeyVersion> findByTenantAndKeyIdAndVersionId(String tenant, String keyId, String versionId);

    /**
     * List all versions of a key
     */
    @Query("SELECT kv FROM KmsKeyVersion kv WHERE kv.tenant = :tenant AND kv.keyId = :keyId ORDER BY kv.creationDate DESC")
    List<KmsKeyVersion> findVersionsByKeyId(@Param("tenant") String tenant, @Param("keyId") String keyId);

    /**
     * List active versions of a key
     */
    @Query("SELECT kv FROM KmsKeyVersion kv WHERE kv.tenant = :tenant AND kv.keyId = :keyId AND kv.status = 'ACTIVE' ORDER BY kv.creationDate DESC LIMIT 1")
    Optional<KmsKeyVersion> findActiveVersionByKeyId(@Param("tenant") String tenant, @Param("keyId") String keyId);

    /**
     * List all versions for a key with pagination
     */
    Page<KmsKeyVersion> findByTenantAndKeyId(String tenant, String keyId, Pageable pageable);

    /**
     * Count versions for a key
     */
    long countByKeyId(String keyId);

}

