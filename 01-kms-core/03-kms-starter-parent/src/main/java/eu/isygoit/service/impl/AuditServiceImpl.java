package eu.isygoit.service.impl;

import eu.isygoit.dto.request.GenerateDataKeyRequestDto;
import eu.isygoit.dto.response.AuditLogResponseDto;
import eu.isygoit.dto.response.DataKeyResponseDto;
import eu.isygoit.service.IAuditService;
import eu.isygoit.service.IDataKeyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;

/**
 * The type Audit service.
 */
@Slf4j
@Service
@Transactional
public class AuditServiceImpl implements IAuditService {

    @Override
    public AuditLogResponseDto getAuditLogs(String tenant, String keyId,
                                            LocalDateTime fromDate, LocalDateTime toDate, Integer limit) {
        log.info("Getting audit logs for tenant: {} keyId: {} from: {} to: {}",
                tenant, keyId, fromDate, toDate);

        return AuditLogResponseDto.builder()
                .logs(new ArrayList<>())
                .build();
    }

    @Override
    public void logAction(String tenant, String action, String keyId, String principal, String ip) {
        log.info("Logging action: {} for tenant: {} keyId: {} principal: {} ip: {}",
                action, tenant, keyId, principal, ip);
    }
}


