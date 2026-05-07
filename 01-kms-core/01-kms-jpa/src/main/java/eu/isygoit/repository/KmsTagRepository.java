package eu.isygoit.repository;

import eu.isygoit.model.KmsTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
    List<KmsTag> findByTenantAndKeyId(String tenant, Long keyId);

    /**
     * Find all tags for a specific key with pagination
     *
     * @param tenant   the tenant
     * @param keyId    the key ID
     * @param pageable pagination information
     * @return Page of tags
     */
    Page<KmsTag> findByTenantAndKeyId(String tenant, Long keyId, Pageable pageable);

    /**
     * Find specific tag by tenant, key ID, and tag key
     *
     * @param tenant the tenant
     * @param keyId  the key ID
     * @param tagKey the tag key
     * @return Optional containing the tag if found
     */
    Optional<KmsTag> findByTenantAndKeyIdAndTagKey(String tenant, Long keyId, String tagKey);

    /**
     * Check if a specific tag exists for a key
     *
     * @param tenant the tenant
     * @param keyId  the key ID
     * @param tagKey the tag key
     * @return true if exists, false otherwise
     */
    boolean existsByTenantAndKeyIdAndTagKey(String tenant, Long keyId, String tagKey);

    /**
     * Delete all tags for a specific key
     *
     * @param tenant the tenant
     * @param keyId  the key ID
     */
    @Modifying
    @Transactional
    void deleteByTenantAndKeyId(String tenant, Long keyId);

    /**
     * Delete specific tags for a key
     *
     * @param tenant  the tenant
     * @param keyId   the key ID
     * @param tagKeys list of tag keys to delete
     */
    @Modifying
    @Transactional
    void deleteByTenantAndKeyIdAndTagKeyIn(String tenant, Long keyId, List<String> tagKeys);

    /**
     * Delete specific tag for a key
     *
     * @param tenant the tenant
     * @param keyId  the key ID
     * @param tagKey the tag key
     */
    @Modifying
    @Transactional
    void deleteByTenantAndKeyIdAndTagKey(String tenant, Long keyId, String tagKey);

    /**
     * Find keys by tag
     *
     * @param tenant   the tenant
     * @param tagKey   the tag key
     * @param tagValue the tag value
     * @return List of key IDs that have the specified tag
     */
    @Query("SELECT DISTINCT t.keyId FROM KmsTag t WHERE t.tenant = :tenant AND t.tagKey = :tagKey AND t.tagValue = :tagValue")
    List<Long> findKeyIdsByTag(@Param("tenant") String tenant,
                               @Param("tagKey") String tagKey,
                               @Param("tagValue") String tagValue);

    /**
     * Find keys by tag key only (any value)
     *
     * @param tenant the tenant
     * @param tagKey the tag key
     * @return List of key IDs that have the specified tag key
     */
    @Query("SELECT DISTINCT t.keyId FROM KmsTag t WHERE t.tenant = :tenant AND t.tagKey = :tagKey")
    List<Long> findKeyIdsByTagKey(@Param("tenant") String tenant, @Param("tagKey") String tagKey);

    /**
     * Count tags for a specific key
     *
     * @param tenant the tenant
     * @param keyId  the key ID
     * @return count of tags
     */
    long countByTenantAndKeyId(String tenant, Long keyId);

    /**
     * Find tags by tag key pattern
     *
     * @param tenant        the tenant
     * @param tagKeyPattern the tag key pattern
     * @return List of matching tags
     */
    @Query("SELECT t FROM KmsTag t WHERE t.tenant = :tenant AND t.tagKey LIKE CONCAT(:tagKeyPattern, '%')")
    List<KmsTag> findByTenantAndTagKeyStartingWith(@Param("tenant") String tenant,
                                                   @Param("tagKeyPattern") String tagKeyPattern);

    /**
     * Update tag value for a specific key
     *
     * @param tenant      the tenant
     * @param keyId       the key ID
     * @param tagKey      the tag key
     * @param newTagValue the new tag value
     * @return number of updated records
     */
    @Modifying
    @Transactional
    @Query("UPDATE KmsTag t SET t.tagValue = :newTagValue, t.lastUpdatedDate = CURRENT_TIMESTAMP " +
            "WHERE t.tenant = :tenant AND t.keyId = :keyId AND t.tagKey = :tagKey")
    int updateTagValue(@Param("tenant") String tenant,
                       @Param("keyId") Long keyId,
                       @Param("tagKey") String tagKey,
                       @Param("newTagValue") String newTagValue);

    /**
     * Find all distinct tag keys for a tenant
     *
     * @param tenant the tenant
     * @return List of distinct tag keys
     */
    @Query("SELECT DISTINCT t.tagKey FROM KmsTag t WHERE t.tenant = :tenant")
    List<String> findDistinctTagKeysByTenant(@Param("tenant") String tenant);

    /**
     * Find all distinct tag keys for a specific key
     *
     * @param tenant the tenant
     * @param keyId  the key ID
     * @return List of distinct tag keys
     */
    @Query("SELECT DISTINCT t.tagKey FROM KmsTag t WHERE t.tenant = :tenant AND t.keyId = :keyId")
    List<String> findDistinctTagKeysByTenantAndKeyId(@Param("tenant") String tenant,
                                                     @Param("keyId") Long keyId);

    /**
     * Batch insert tags (for performance optimization)
     *
     * @param tags list of tags to insert
     * @return number of inserted records
     */
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO KMS_TAG (TENANT, KEY_ID, TAG_KEY, TAG_VALUE, CREATION_DATE) " +
            "VALUES (:tenant, :keyId, :tagKey, :tagValue, CURRENT_TIMESTAMP)",
            nativeQuery = true)
    int batchInsertTag(@Param("tenant") String tenant,
                       @Param("keyId") Long keyId,
                       @Param("tagKey") String tagKey,
                       @Param("tagValue") String tagValue);

    /**
     * Find tags by multiple key IDs (for bulk operations)
     *
     * @param tenant the tenant
     * @param keyIds list of key IDs
     * @return List of tags for the specified keys
     */
    @Query("SELECT t FROM KmsTag t WHERE t.tenant = :tenant AND t.keyId IN :keyIds")
    List<KmsTag> findByTenantAndKeyIdIn(@Param("tenant") String tenant,
                                        @Param("keyIds") List<Long> keyIds);

    /**
     * Get all tags for multiple keys grouped by key ID
     * This returns a map-like structure that needs to be processed in service layer
     *
     * @param tenant the tenant
     * @param keyIds list of key IDs
     * @return List of tags
     */
    @Query("SELECT t FROM KmsTag t WHERE t.tenant = :tenant AND t.keyId IN :keyIds ORDER BY t.keyId, t.tagKey")
    List<KmsTag> findByTenantAndKeyIdInOrderByKeyId(@Param("tenant") String tenant,
                                                    @Param("keyIds") List<Long> keyIds);
}