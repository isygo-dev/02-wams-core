package eu.isygoit.repository;

import eu.isygoit.enums.IKmsActionType;
import eu.isygoit.model.KmsAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for KMS Audit Log entity
 */
@Repository
public interface KmsAuditLogRepository extends JpaRepository<KmsAuditLog, Long> {

    /**
     * Find audit logs by tenant and key
     */
    @Query("SELECT al FROM KmsAuditLog al WHERE al.tenant = :tenant AND al.keyId = :keyId ORDER BY al.timestamp DESC")
    Page<KmsAuditLog> findByTenantAndKeyId(@Param("tenant") String tenant, @Param("keyId") String keyId, Pageable pageable);

    /**
     * Count by action and keyId
     */
    long countByActionAndKeyId(IKmsActionType.Types action, String keyId);

    /**
     * Find first by keyId order by timestamp desc
     */
    Optional<KmsAuditLog> findFirstByKeyIdOrderByTimestampDesc(String keyId);

    /**
     * Find audit logs by tenant and action
     */
    Page<KmsAuditLog> findByTenantAndAction(String tenant, String action, Pageable pageable);

    /**
     * Find audit logs between dates
     */
    @Query("SELECT al FROM KmsAuditLog al WHERE al.tenant = :tenant AND al.timestamp BETWEEN :fromDate AND :toDate ORDER BY al.timestamp DESC")
    Page<KmsAuditLog> findByDateRange(@Param("tenant") String tenant, @Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate, Pageable pageable);

    /**
     * Complex query for audit logs
     */
    @Query("SELECT al FROM KmsAuditLog al WHERE al.tenant = :tenant " +
            "AND (:keyId IS NULL OR al.keyId = :keyId) " +
            "AND (:action IS NULL OR al.action = :action) " +
            "AND (:fromDate IS NULL OR al.timestamp >= :fromDate) " +
            "AND (:toDate IS NULL OR al.timestamp <= :toDate) " +
            "ORDER BY al.timestamp DESC")
    Page<KmsAuditLog> findByMultipleCriteria(
            @Param("tenant") String tenant,
            @Param("keyId") String keyId,
            @Param("action") String action,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

}

