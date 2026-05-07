package eu.isygoit.service;

import eu.isygoit.dto.response.AuditLogResponseDto;
import eu.isygoit.enums.IKmsActionType;

import java.time.LocalDateTime;

/**
 * The interface Audit service.
 */
public interface IAuditService {

    /**
     * Get audit logs.
     *
     * @param tenant   the tenant
     * @param keyId    the key id
     * @param fromDate the from date
     * @param toDate   the to date
     * @param limit    the limit
     * @return the audit log response dto
     */
    AuditLogResponseDto getAuditLogs(String tenant, String  keyId, LocalDateTime fromDate, LocalDateTime toDate, Integer limit);

    /**
     * Log action.
     *
     * @param tenant    the tenant
     * @param action    the action
     * @param keyId     the key id
     * @param principal the principal
     * @param ip        the ip
     */
    void logAction(String tenant, IKmsActionType.Types action, String keyId, String principal, String ip);
}

