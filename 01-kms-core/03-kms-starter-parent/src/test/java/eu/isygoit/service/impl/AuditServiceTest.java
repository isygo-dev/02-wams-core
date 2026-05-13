package eu.isygoit.service.impl;

import eu.isygoit.dto.KmsDtos.AuditLogResponseDto;
import eu.isygoit.enums.IKmsActionType;
import eu.isygoit.service.IAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for AuditServiceImpl.
 *
 * Covers:
 * - Getting audit logs with various date ranges
 * - Logging actions for different KMS operations
 * - Edge cases and null handling
 */
@ExtendWith(MockitoExtension.class)
public class AuditServiceTest {

    private static final String TENANT = "test-tenant";
    private static final String KEY_ID = "test-key-123";
    private static final String PRINCIPAL = "test-principal";
    private static final String IP_ADDRESS = "192.168.1.1";

    @InjectMocks
    private AuditServiceImpl auditService;

    @BeforeEach
    void setUp() {
        assertNotNull(auditService);
    }

    // ==============================================================
    // Test: getAuditLogs method
    // ==============================================================

    @Test
    void shouldGetAuditLogsWithValidParameters() {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
        LocalDateTime toDate = LocalDateTime.now();
        Integer limit = 100;

        AuditLogResponseDto response = auditService.getAuditLogs(TENANT, KEY_ID, fromDate, toDate, limit);

        assertNotNull(response);
        assertNotNull(response.getLogs());
        assertEquals(0, response.getLogs().size());
    }

    @Test
    void shouldReturnEmptyLogsWhenNoAuditData() {
        AuditLogResponseDto response = auditService.getAuditLogs(TENANT, KEY_ID, LocalDateTime.now().minusDays(30), LocalDateTime.now(), 100);

        assertNotNull(response);
        assertNotNull(response.getLogs());
        assertTrue(response.getLogs() instanceof ArrayList);
        assertEquals(0, response.getLogs().size());
    }

    @Test
    void shouldHandleNullFromDate() {
        LocalDateTime toDate = LocalDateTime.now();
        AuditLogResponseDto response = auditService.getAuditLogs(TENANT, KEY_ID, null, toDate, 100);

        assertNotNull(response);
        assertNotNull(response.getLogs());
    }

    @Test
    void shouldHandleNullToDate() {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
        AuditLogResponseDto response = auditService.getAuditLogs(TENANT, KEY_ID, fromDate, null, 100);

        assertNotNull(response);
        assertNotNull(response.getLogs());
    }

    @Test
    void shouldHandleNullBothDates() {
        AuditLogResponseDto response = auditService.getAuditLogs(TENANT, KEY_ID, null, null, 100);

        assertNotNull(response);
        assertNotNull(response.getLogs());
    }

    @Test
    void shouldHandleNullLimit() {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
        LocalDateTime toDate = LocalDateTime.now();
        AuditLogResponseDto response = auditService.getAuditLogs(TENANT, KEY_ID, fromDate, toDate, null);

        assertNotNull(response);
        assertNotNull(response.getLogs());
    }

    @Test
    void shouldHandleNullTenant() {
        AuditLogResponseDto response = auditService.getAuditLogs(null, KEY_ID, LocalDateTime.now().minusDays(7), LocalDateTime.now(), 100);

        assertNotNull(response);
        assertNotNull(response.getLogs());
    }

    @Test
    void shouldHandleNullKeyId() {
        AuditLogResponseDto response = auditService.getAuditLogs(TENANT, null, LocalDateTime.now().minusDays(7), LocalDateTime.now(), 100);

        assertNotNull(response);
        assertNotNull(response.getLogs());
    }

    @Test
    void shouldHandleFutureFromDate() {
        LocalDateTime fromDate = LocalDateTime.now().plusDays(1);
        LocalDateTime toDate = LocalDateTime.now().plusDays(10);
        AuditLogResponseDto response = auditService.getAuditLogs(TENANT, KEY_ID, fromDate, toDate, 100);

        assertNotNull(response);
        assertNotNull(response.getLogs());
    }

    @Test
    void shouldHandleLargeLimit() {
        AuditLogResponseDto response = auditService.getAuditLogs(TENANT, KEY_ID, LocalDateTime.now().minusYears(1), LocalDateTime.now(), 10000);

        assertNotNull(response);
        assertNotNull(response.getLogs());
    }

    @Test
    void shouldHandleZeroLimit() {
        AuditLogResponseDto response = auditService.getAuditLogs(TENANT, KEY_ID, LocalDateTime.now().minusDays(7), LocalDateTime.now(), 0);

        assertNotNull(response);
        assertNotNull(response.getLogs());
    }

    @Test
    void shouldHandleEmptyStringTenant() {
        AuditLogResponseDto response = auditService.getAuditLogs("", KEY_ID, LocalDateTime.now().minusDays(7), LocalDateTime.now(), 100);

        assertNotNull(response);
        assertNotNull(response.getLogs());
    }

    @Test
    void shouldHandleEmptyStringKeyId() {
        AuditLogResponseDto response = auditService.getAuditLogs(TENANT, "", LocalDateTime.now().minusDays(7), LocalDateTime.now(), 100);

        assertNotNull(response);
        assertNotNull(response.getLogs());
    }

    // ==============================================================
    // Test: logAction method
    // ==============================================================

    @Test
    void shouldLogActionSuccessfully() {
        assertDoesNotThrow(() ->
                auditService.logAction(TENANT, IKmsActionType.Types.ENCRYPT, KEY_ID, PRINCIPAL, IP_ADDRESS)
        );
    }

    @Test
    void shouldLogActionWithDifferentActionTypes() {
        for (IKmsActionType.Types actionType : IKmsActionType.Types.values()) {
            assertDoesNotThrow(() ->
                    auditService.logAction(TENANT, actionType, KEY_ID, PRINCIPAL, IP_ADDRESS)
            );
        }
    }

    @Test
    void shouldHandleLogActionWithNullAction() {
        assertDoesNotThrow(() ->
                auditService.logAction(TENANT, null, KEY_ID, PRINCIPAL, IP_ADDRESS)
        );
    }

    @Test
    void shouldHandleLogActionWithNullTenant() {
        assertDoesNotThrow(() ->
                auditService.logAction(null, IKmsActionType.Types.ENCRYPT, KEY_ID, PRINCIPAL, IP_ADDRESS)
        );
    }

    @Test
    void shouldHandleLogActionWithNullKeyId() {
        assertDoesNotThrow(() ->
                auditService.logAction(TENANT, IKmsActionType.Types.ENCRYPT, null, PRINCIPAL, IP_ADDRESS)
        );
    }

    @Test
    void shouldHandleLogActionWithNullPrincipal() {
        assertDoesNotThrow(() ->
                auditService.logAction(TENANT, IKmsActionType.Types.ENCRYPT, KEY_ID, null, IP_ADDRESS)
        );
    }

    @Test
    void shouldHandleLogActionWithNullIp() {
        assertDoesNotThrow(() ->
                auditService.logAction(TENANT, IKmsActionType.Types.ENCRYPT, KEY_ID, PRINCIPAL, null)
        );
    }

    @Test
    void shouldHandleLogActionWithAllNullParameters() {
        assertDoesNotThrow(() ->
                auditService.logAction(null, null, null, null, null)
        );
    }

    @Test
    void shouldHandleLogActionWithEmptyStrings() {
        assertDoesNotThrow(() ->
                auditService.logAction("", IKmsActionType.Types.ENCRYPT, "", "", "")
        );
    }

    @Test
    void shouldHandleLogActionWithLongValues() {
        String longTenant = "a".repeat(255);
        String longKeyId = "a".repeat(255);
        String longPrincipal = "a".repeat(255);
        String longIp = "a".repeat(255);

        assertDoesNotThrow(() ->
                auditService.logAction(longTenant, IKmsActionType.Types.ENCRYPT, longKeyId, longPrincipal, longIp)
        );
    }

    @Test
    void shouldLogMultipleActions() {
        for (Integer i = 0; i < 10; i++) {
            Integer finalI = i;
            assertDoesNotThrow(() ->
                    auditService.logAction(
                            TENANT + "-" + finalI,
                            IKmsActionType.Types.ENCRYPT,
                            KEY_ID + "-" + finalI,
                            PRINCIPAL + "-" + finalI,
                            "192.168.1." + finalI
                    )
            );
        }
    }

    @Test
    void shouldLogActionWithSpecialCharacters() {
        String specialTenant = "tenant@#$%";
        String specialKeyId = "key!@#$%^&*()";
        String specialPrincipal = "principal<>?:{}";
        String specialIp = "192.168.1.1";

        assertDoesNotThrow(() ->
                auditService.logAction(specialTenant, IKmsActionType.Types.ENCRYPT, specialKeyId, specialPrincipal, specialIp)
        );
    }

    @Test
    void shouldHandleLogActionWithUnicodeCharacters() {
        String unicodeTenant = "테넌트";
        String unicodeKeyId = "キー";
        String unicodePrincipal = "主体";

        assertDoesNotThrow(() ->
                auditService.logAction(unicodeTenant, IKmsActionType.Types.ENCRYPT, unicodeKeyId, unicodePrincipal, "192.168.1.1")
        );
    }

    @Test
    void shouldLogDecryptAction() {
        assertDoesNotThrow(() ->
                auditService.logAction(TENANT, IKmsActionType.Types.DECRYPT, KEY_ID, PRINCIPAL, IP_ADDRESS)
        );
    }

    @Test
    void shouldLogSignAction() {
        assertDoesNotThrow(() ->
                auditService.logAction(TENANT, IKmsActionType.Types.SIGN, KEY_ID, PRINCIPAL, IP_ADDRESS)
        );
    }

    @Test
    void shouldLogVerifyAction() {
        assertDoesNotThrow(() ->
                auditService.logAction(TENANT, IKmsActionType.Types.VERIFY, KEY_ID, PRINCIPAL, IP_ADDRESS)
        );
    }

    @Test
    void shouldLogGenerateDataKeyAction() {
        assertDoesNotThrow(() ->
                auditService.logAction(TENANT, IKmsActionType.Types.GENERATE_DATA_KEY, KEY_ID, PRINCIPAL, IP_ADDRESS)
        );
    }

    @Test
    void shouldLogCreateKeyAction() {
        assertDoesNotThrow(() ->
                auditService.logAction(TENANT, IKmsActionType.Types.CREATE_KEY, KEY_ID, PRINCIPAL, IP_ADDRESS)
        );
    }

    @Test
    void shouldLogUpdateKeyAction() {
        assertDoesNotThrow(() ->
                auditService.logAction(TENANT, IKmsActionType.Types.UPDATE_KEY_DESCRIPTION, KEY_ID, PRINCIPAL, IP_ADDRESS)
        );
    }

    @Test
    void shouldLogDeleteKeyAction() {
        assertDoesNotThrow(() ->
                auditService.logAction(TENANT, IKmsActionType.Types.DELETE_KEY, KEY_ID, PRINCIPAL, IP_ADDRESS)
        );
    }

    @Test
    void shouldLogScheduleKeyDeletionAction() {
        assertDoesNotThrow(() ->
                auditService.logAction(TENANT, IKmsActionType.Types.SCHEDULE_KEY_DELETION, KEY_ID, PRINCIPAL, IP_ADDRESS)
        );
    }

    @Test
    void shouldHandleLogActionWithIpv6Address() {
        String ipv6 = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";
        assertDoesNotThrow(() ->
                auditService.logAction(TENANT, IKmsActionType.Types.ENCRYPT, KEY_ID, PRINCIPAL, ipv6)
        );
    }
}

