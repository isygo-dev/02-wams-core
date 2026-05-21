package eu.isygoit.service.impl;

import eu.isygoit.dto.KmsDtos.AuditLogResponse;
import eu.isygoit.dto.KmsDtos.AuditLogResponse.LogEntry;
import eu.isygoit.enums.IKmsActionType;
import eu.isygoit.model.KmsAuditLog;
import eu.isygoit.repository.KmsAuditLogRepository;
import eu.isygoit.service.IAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class AuditServiceImpl implements IAuditService {

    @Autowired
    private KmsAuditLogRepository auditLogRepository;

    @Override
    public AuditLogResponse getAuditLogs(String tenant, String keyId,
                                         LocalDateTime fromDate, LocalDateTime toDate, Integer limit) {
        log.info("Getting audit logs for tenant: {} keyId: {} from: {} to: {}", tenant, keyId, fromDate, toDate);

        int pageSize = (limit != null && limit > 0) ? limit : 500;
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "timestamp"));

        Page<KmsAuditLog> page;

        // Use explicit repository methods based on which date filters are present
        if (fromDate != null && toDate != null) {
            page = auditLogRepository.findByTenantAndKeyIdAndTimestampBetween(tenant, keyId, fromDate, toDate, pageable);
        } else if (fromDate != null) {
            page = auditLogRepository.findByTenantAndKeyIdAndTimestampGreaterThanEqual(tenant, keyId, fromDate, pageable);
        } else if (toDate != null) {
            page = auditLogRepository.findByTenantAndKeyIdAndTimestampLessThanEqual(tenant, keyId, toDate, pageable);
        } else {
            page = auditLogRepository.findByTenantAndKeyId(tenant, keyId, pageable);
        }

        List<LogEntry> entries = page.getContent().stream()
                .map(log -> LogEntry.builder()
                        .timestamp(log.getTimestamp())
                        .action(log.getAction() != null ? log.getAction().name() : null)
                        .keyId(log.getKeyId())
                        .principal(log.getPrincipal())
                        .ipAddress(log.getIpAddress())
                        .status(log.getStatus())
                        .errorMessage(log.getErrorMessage())
                        .executionTimeMs(log.getExecutionTimeMs())
                        .build())
                .collect(Collectors.toList());

        return AuditLogResponse.builder()
                .logs(entries)
                .build();
    }

    @Override
    public void logAction(String tenant, IKmsActionType.Types action, String keyId, String principal, String ip) {
        log.info("Logging action: {} for tenant: {} keyId: {} principal: {} ip: {}", action, tenant, keyId, principal, ip);

        KmsAuditLog auditLog = KmsAuditLog.builder()
                .tenant(tenant)
                .keyId(keyId)
                .action(action)
                .principal(principal)
                .ipAddress(ip)
                .timestamp(LocalDateTime.now())
                .status("SUCCESS")
                .build();

        auditLogRepository.save(auditLog);
    }
}