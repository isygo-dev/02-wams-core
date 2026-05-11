package eu.isygoit.repository;

import eu.isygoit.model.KmsAlias;
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
 * Repository interface for KmsAlias entity
 */
@Repository
public interface KmsAliasRepository extends JpaRepository<KmsAlias, Long> {

    /**
     * Find alias by tenant and alias name
     *
     * @param tenant    the tenant
     * @param aliasName the alias name
     * @return Optional containing the alias if found
     */
    Optional<KmsAlias> findByTenantAndAliasName(String tenant, String aliasName);

    /**
     * Find all aliases for a specific tenant
     *
     * @param tenant   the tenant
     * @param pageable pagination information
     * @return Page of aliases
     */
    Page<KmsAlias> findByTenant(String tenant, Pageable pageable);

    /**
     * Find all aliases for a specific tenant
     *
     * @param tenant the tenant
     * @return List of aliases
     */
    List<KmsAlias> findByTenant(String tenant);

    /**
     * Find all aliases associated with a specific key
     *
     * @param tenant the tenant
     * @param keyId  the key ID
     * @return List of aliases
     */
    List<KmsAlias> findByTenantAndKeyId(String tenant, String keyId, Pageable pageable);

    /**
     * Find alias by alias name (case-insensitive)
     *
     * @param aliasName the alias name
     * @return Optional containing the alias if found
     */
    @Query("SELECT a FROM KmsAlias a WHERE LOWER(a.aliasName) = LOWER(:aliasName)")
    Optional<KmsAlias> findByAliasNameIgnoreCase(@Param("aliasName") String aliasName);

    /**
     * Check if alias exists for a tenant
     *
     * @param tenant    the tenant
     * @param aliasName the alias name
     * @return true if exists, false otherwise
     */
    boolean existsByTenantAndAliasName(String tenant, String aliasName);

    /**
     * Delete alias by tenant and alias name
     *
     * @param tenant    the tenant
     * @param aliasName the alias name
     */
    @Modifying
    @Transactional
    void deleteByTenantAndAliasName(String tenant, String aliasName);

    /**
     * Delete all aliases for a specific key
     *
     * @param tenant the tenant
     * @param keyId  the key ID
     */
    @Modifying
    @Transactional
    void deleteByTenantAndKeyId(String tenant, String keyId);

    /**
     * Count aliases for a specific key
     *
     * @param tenant the tenant
     * @param keyId  the key ID
     * @return count of aliases
     */
    long countByTenantAndKeyId(String tenant, String keyId);

    /**
     * Find aliases by alias name pattern (starts with)
     *
     * @param tenant      the tenant
     * @param aliasPrefix the alias prefix
     * @return List of matching aliases
     */
    List<KmsAlias> findByTenantAndAliasNameStartingWith(String tenant, String aliasPrefix);

    /**
     * Find aliases by alias name pattern (contains)
     *
     * @param tenant       the tenant
     * @param aliasPattern the alias pattern
     * @param pageable     pagination information
     * @return Page of matching aliases
     */
    @Query("SELECT a FROM KmsAlias a WHERE a.tenant = :tenant AND LOWER(a.aliasName) LIKE LOWER(CONCAT('%', :aliasPattern, '%'))")
    Page<KmsAlias> findByTenantAndAliasNameContaining(@Param("tenant") String tenant,
                                                      @Param("aliasPattern") String aliasPattern,
                                                      Pageable pageable);

    /**
     * Update alias mapping to a different key
     *
     * @param tenant    the tenant
     * @param aliasName the alias name
     * @param newKeyId  the new key ID
     * @return number of updated records
     */
    @Modifying
    @Transactional
    @Query("UPDATE KmsAlias a SET a.keyId = :newKeyId, a.lastUpdatedDate = CURRENT_TIMESTAMP WHERE a.tenant = :tenant AND a.aliasName = :aliasName")
    int updateKeyMapping(@Param("tenant") String tenant,
                         @Param("aliasName") String aliasName,
                         @Param("newKeyId") Long newKeyId);
}