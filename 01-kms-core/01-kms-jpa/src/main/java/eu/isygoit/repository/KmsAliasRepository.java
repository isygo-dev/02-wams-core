package eu.isygoit.repository;

import eu.isygoit.model.KmsAlias;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
     * Find all aliases associated with a specific key
     *
     * @param tenant the tenant
     * @param keyId  the key ID
     * @return List of aliases
     */
    Page<KmsAlias> findByTenantAndKeyId(String tenant, String keyId, Pageable pageable);

    /**
     * Delete all aliases for a specific key
     *
     * @param tenant the tenant
     * @param keyId  the key ID
     */
    @Modifying
    @Transactional
    void deleteByTenantAndTargetKeyId(String tenant, String keyId);
}