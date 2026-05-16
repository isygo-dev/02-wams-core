package eu.isygoit.repository;

import eu.isygoit.model.KmsKeyPolicy;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for KMS Key Policy entity
 */
@Repository
public interface KmsKeyPolicyRepository extends JpaRepository<KmsKeyPolicy, Long> {

    /**
     * Find key policy by tenant and keyId
     */
    List<KmsKeyPolicy> findByTenantAndKeyId(String tenant, String keyId, Pageable pageable);

    KmsKeyPolicy findByTenantAndKeyId(String tenant, String keyId);

    Optional<KmsKeyPolicy> findByTenantAndKeyIdAndPolicyName(String tenant, String keyId, String policyName);
}
