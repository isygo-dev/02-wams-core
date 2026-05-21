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

@Repository
public interface KmsAuditLogRepository extends JpaRepository<KmsAuditLog, Long> {

    // Basic filter: only keyId
    Page<KmsAuditLog> findByTenantAndKeyId(String tenant, String keyId, Pageable pageable);

    // Filter: keyId + timestamp between from and to (both inclusive)
    Page<KmsAuditLog> findByTenantAndKeyIdAndTimestampBetween(String tenant, String keyId,
                                                              LocalDateTime from, LocalDateTime to,
                                                              Pageable pageable);

    // Filter: keyId + timestamp >= fromDate
    Page<KmsAuditLog> findByTenantAndKeyIdAndTimestampGreaterThanEqual(String tenant, String keyId,
                                                                       LocalDateTime from, Pageable pageable);

    // Filter: keyId + timestamp <= toDate
    Page<KmsAuditLog> findByTenantAndKeyIdAndTimestampLessThanEqual(String tenant, String keyId,
                                                                    LocalDateTime to, Pageable pageable);

    // Optional: count by action (unused in service but kept)
    long countByTenantAndActionAndKeyId(String tenant, IKmsActionType.Types action, String keyId);

    // Latest log for a key
    Optional<KmsAuditLog> findFirstByTenantAndKeyIdOrderByTimestampDesc(String tenant, String keyId);
}