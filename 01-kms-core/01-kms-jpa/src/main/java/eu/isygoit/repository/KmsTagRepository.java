package eu.isygoit.repository;

import eu.isygoit.model.KmsTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Repository interface for KmsTag entity
 */
@Repository
public interface KmsTagRepository extends JpaRepository<KmsTag, Long> {

    /**
     * Find all tags for a specific key
     *
     * @param tenant the tenant
     * @param keyId  the key ID
     * @return List of tags
     */
    List<KmsTag> findByTenantAndKeyId(String tenant, String keyId);

    /**
     * Find all tags for a specific key with pagination
     *
     * @param tenant   the tenant
     * @param keyId    the key ID
     * @param pageable pagination information
     * @return Page of tags
     */
    Page<KmsTag> findByTenantAndKeyId(String tenant, String keyId, Pageable pageable);

    /**
     * Delete all tags for a specific key
     *
     * @param tenant the tenant
     * @param keyId  the key ID
     */
    @Modifying
    @Transactional
    void deleteByTenantAndKeyId(String tenant, String keyId);

    /**
     * Delete specific tags for a key
     *
     * @param tenant  the tenant
     * @param keyId   the key ID
     * @param tagKeys list of tag keys to delete
     */
    @Modifying
    @Transactional
    void deleteByTenantAndKeyIdAndTagKeyIn(String tenant, String keyId, List<String> tagKeys);

    boolean existsByTenantAndKeyId(String tenant, String keyId);
}