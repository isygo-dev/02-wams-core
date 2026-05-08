package eu.isygoit.repository;

import eu.isygoit.model.KmsKeyGrant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for KMS Key Grant entity
 */
@Repository
public interface KmsKeyGrantRepository extends JpaRepository<KmsKeyGrant, Long> {

    /**
     * Find grant by tenant and grantId
     */
    Optional<KmsKeyGrant> findByTenantAndGrantId(String tenant, String grantId);

    /**
     * List grants for a key
     */
    Page<KmsKeyGrant> findByTenantAndKeyId(String tenant, Long keyId, Pageable pageable);

    /**
     * List active grants for a key
     */
    Page<KmsKeyGrant> findByTenantAndKeyIdAndStatus(String tenant, Long keyId, String status, Pageable pageable);
}
