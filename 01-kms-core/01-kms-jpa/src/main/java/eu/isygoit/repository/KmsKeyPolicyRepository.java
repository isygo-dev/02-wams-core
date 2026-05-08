package eu.isygoit.repository;

import eu.isygoit.model.KmsKeyPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for KMS Key Policy entity
 */
@Repository
public interface KmsKeyPolicyRepository extends JpaRepository<KmsKeyPolicy, Long> {

    /**
     * Find key policy by tenant and keyId
     */
    Optional<KmsKeyPolicy> findByTenantAndKeyId(String tenant, Long keyId);
}
