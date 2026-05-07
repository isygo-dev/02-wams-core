package eu.isygoit.controller;

import eu.isygoit.annotation.InjectExceptionHandler;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.dto.request.*;
import eu.isygoit.dto.response.*;
import eu.isygoit.enums.IEnumCharSet;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The type Key controller.
 */
@Slf4j
@Validated
@RestController
@InjectExceptionHandler(KmsExceptionHandler.class)
@RequestMapping(path = "/api/v1/private/key")
@Tag(name = "KMS Keys", description = "Key Management Service - All cryptographic operations and key management endpoints")
public class KeyController extends ControllerExceptionHandler implements IKmsServiceApi {

    @Autowired
    private IKeyService keyService;

    @Autowired
    private IKeyManagementService keyManagementService;

    @Autowired
    private IEncryptionService encryptionService;

    @Autowired
    private ISigningService signingService;

    @Autowired
    private IKeyPolicyService keyPolicyService;

    @Autowired
    private IKeyVersionService keyVersionService;

    @Autowired
    private IDataKeyService dataKeyService;

    @Autowired
    private IAuditService auditService;

    @Autowired(required = false)
    private KeyServiceApi legacyKeyServiceApi;

    // ======================== Key Management APIs ========================

    @Override
    public ResponseEntity<CreateKeyResponseDto> createKey(@Valid @RequestBody CreateKeyRequestDto request) {
        log.info("Creating key with spec: {} and purpose: {}", request.getKeySpec(), request.getPurpose());
        try {
            String tenant = getTenant();
            CreateKeyResponseDto response = keyManagementService.createKey(tenant, request);
            auditService.logAction(tenant, "CREATE_KEY", response.getKeyId(), getPrincipal(), getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<KeyMetadataResponseDto> getKeyMetadata(@PathVariable String keyId) {
        log.info("Getting key metadata for keyId: {}", keyId);
        try {
            String tenant = getTenant();
            KeyMetadataResponseDto response = keyManagementService.getKeyMetadata(tenant, keyId);
            auditService.logAction(tenant, "GET_KEY_METADATA", keyId, getPrincipal(), getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ListKeysResponseDto> listKeys(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String nextToken) {
        log.info("Listing keys with limit: {}", limit);
        try {
            String tenant = getTenant();
            ListKeysResponseDto response = keyManagementService.listKeys(tenant, limit, nextToken);
            auditService.logAction(tenant, "LIST_KEYS", "-", getPrincipal(), getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<KeyMetadataResponseDto> enableKey(@PathVariable String keyId) {
        log.info("Enabling key: {}", keyId);
        try {
            String tenant = getTenant();
            KeyMetadataResponseDto response = keyManagementService.enableKey(tenant, keyId);
            auditService.logAction(tenant, "ENABLE_KEY", keyId, getPrincipal(), getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<KeyMetadataResponseDto> disableKey(@PathVariable String keyId) {
        log.info("Disabling key: {}", keyId);
        try {
            String tenant = getTenant();
            KeyMetadataResponseDto response = keyManagementService.disableKey(tenant, keyId);
            auditService.logAction(tenant, "DISABLE_KEY", keyId, getPrincipal(), getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<KeyMetadataResponseDto> scheduleKeyDeletion(
            @PathVariable String keyId,
            @RequestParam(required = false) Integer pendingWindowInDays) {
        log.info("Scheduling deletion for key: {} with pending window: {} days", keyId, pendingWindowInDays);
        try {
            String tenant = getTenant();
            KeyMetadataResponseDto response = keyManagementService.scheduleKeyDeletion(tenant, keyId, pendingWindowInDays);
            auditService.logAction(tenant, "SCHEDULE_KEY_DELETION", keyId, getPrincipal(), getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<RotateKeyResponseDto> rotateKey(@PathVariable String keyId) {
        log.info("Rotating key: {}", keyId);
        try {
            String tenant = getTenant();
            RotateKeyResponseDto response = keyManagementService.rotateKey(tenant, keyId);
            auditService.logAction(tenant, "ROTATE_KEY", keyId, getPrincipal(), getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    // ======================== Cryptographic Operations ========================

    @Override
    public ResponseEntity<EncryptResponseDto> encrypt(@Valid @RequestBody EncryptRequestDto request) {
        log.info("Encrypting data with keyId: {}", request.getKeyId());
        try {
            String tenant = getTenant();
            EncryptResponseDto response = encryptionService.encrypt(tenant, request);
            auditService.logAction(tenant, "ENCRYPT", request.getKeyId(), getPrincipal(), getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<DecryptResponseDto> decrypt(@Valid @RequestBody DecryptRequestDto request) {
        log.info("Decrypting data");
        try {
            String tenant = getTenant();
            DecryptResponseDto response = encryptionService.decrypt(tenant, request);
            auditService.logAction(tenant, "DECRYPT", response.getKeyId(), getPrincipal(), getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<EncryptResponseDto> reencrypt(@Valid @RequestBody ReencryptRequestDto request) {
        log.info("Re-encrypting data to destination key: {}", request.getDestinationKeyId());
        try {
            String tenant = getTenant();
            EncryptResponseDto response = encryptionService.reencrypt(tenant, request);
            auditService.logAction(tenant, "REENCRYPT", request.getDestinationKeyId(), getPrincipal(), getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    // ======================== Signing APIs ========================

    @Override
    public ResponseEntity<SignResponseDto> sign(@Valid @RequestBody SignRequestDto request) {
        log.info("Signing message with keyId: {} using algorithm: {}", request.getKeyId(), request.getAlgorithm());
        try {
            String tenant = getTenant();
            SignResponseDto response = signingService.sign(tenant, request);
            auditService.logAction(tenant, "SIGN", request.getKeyId(), getPrincipal(), getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<VerifyResponseDto> verify(@Valid @RequestBody VerifyRequestDto request) {
        log.info("Verifying signature with keyId: {}", request.getKeyId());
        try {
            String tenant = getTenant();
            VerifyResponseDto response = signingService.verify(tenant, request);
            auditService.logAction(tenant, "VERIFY", request.getKeyId(), getPrincipal(), getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    // ======================== Key Policy & Access Control ========================

    @Override
    public ResponseEntity<?> setKeyPolicy(@PathVariable String keyId, @Valid @RequestBody SetKeyPolicyRequestDto request) {
        log.info("Setting key policy for keyId: {}", keyId);
        try {
            String tenant = getTenant();
            Object response = keyPolicyService.setKeyPolicy(tenant, keyId, request);
            auditService.logAction(tenant, "SET_KEY_POLICY", keyId, getPrincipal(), getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<?> getKeyPolicy(@PathVariable String keyId) {
        log.info("Getting key policy for keyId: {}", keyId);
        try {
            String tenant = getTenant();
            Object response = keyPolicyService.getKeyPolicy(tenant, keyId);
            auditService.logAction(tenant, "GET_KEY_POLICY", keyId, getPrincipal(), getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<GrantResponseDto> createGrant(
            @PathVariable String keyId,
            @Valid @RequestBody CreateGrantRequestDto request) {
        log.info("Creating grant for keyId: {} principal: {}", keyId, request.getPrincipal());
        try {
            String tenant = getTenant();
            GrantResponseDto response = keyPolicyService.createGrant(tenant, keyId, request);
            auditService.logAction(tenant, "CREATE_GRANT", keyId, getPrincipal(), getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<?> revokeGrant(@PathVariable String keyId, @PathVariable String grantId) {
        log.info("Revoking grant: {} for keyId: {}", grantId, keyId);
        try {
            String tenant = getTenant();
            String response = keyPolicyService.revokeGrant(tenant, keyId, grantId);
            auditService.logAction(tenant, "REVOKE_GRANT", keyId, getPrincipal(), getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    // ======================== Key Versioning APIs ========================

    @Override
    public ResponseEntity<KeyVersionListResponseDto> listKeyVersions(@PathVariable String keyId) {
        log.info("Listing key versions for keyId: {}", keyId);
        try {
            String tenant = getTenant();
            KeyVersionListResponseDto response = keyVersionService.listKeyVersions(tenant, keyId);
            auditService.logAction(tenant, "LIST_KEY_VERSIONS", keyId, getPrincipal(), getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ActiveVersionResponseDto> getActiveVersion(@PathVariable String keyId) {
        log.info("Getting active version for keyId: {}", keyId);
        try {
            String tenant = getTenant();
            ActiveVersionResponseDto response = keyVersionService.getActiveVersion(tenant, keyId);
            auditService.logAction(tenant, "GET_ACTIVE_VERSION", keyId, getPrincipal(), getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    // ======================== Data Key API ========================

    @Override
    public ResponseEntity<DataKeyResponseDto> generateDataKey(@Valid @RequestBody GenerateDataKeyRequestDto request) {
        log.info("Generating data key with keyId: {}", request.getKeyId());
        try {
            String tenant = getTenant();
            DataKeyResponseDto response = dataKeyService.generateDataKey(tenant, request);
            auditService.logAction(tenant, "GENERATE_DATA_KEY", request.getKeyId(), getPrincipal(), getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    // ======================== Audit APIs ========================

    @Override
    public ResponseEntity<AuditLogResponseDto> getAuditLogs(
            @RequestParam(required = false) String keyId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Integer limit) {
        log.info("Getting audit logs for keyId: {} from: {} to: {}", keyId, fromDate, toDate);
        try {
            String tenant = getTenant();
            LocalDateTime from = null;
            LocalDateTime to = null;

            if (fromDate != null) {
                from = LocalDateTime.parse(fromDate, DateTimeFormatter.ISO_DATE_TIME);
            }
            if (toDate != null) {
                to = LocalDateTime.parse(toDate, DateTimeFormatter.ISO_DATE_TIME);
            }

            AuditLogResponseDto response = auditService.getAuditLogs(tenant, keyId, from, to, limit);
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    // ======================== Legacy Random Key APIs ========================

    /**
     * Generate random key.
     *
     * @param length      the length
     * @param charSetType the char set type
     * @return the response entity
     */
    @GetMapping("/random")
    public ResponseEntity<String> newRandomKey(
            @RequestParam Integer length,
            @RequestParam IEnumCharSet.Types charSetType) {
        log.info("Call generateRandomKey");
        try {
            return ResponseFactory.responseOk(keyService.getRandomKey(length, charSetType));
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    /**
     * Renew random key.
     *
     * @param tenant      the tenant
     * @param keyName     the key name
     * @param length      the length
     * @param charSetType the char set type
     * @return the response entity
     */
    @PutMapping("/random/{keyName}")
    public ResponseEntity<String> renewRandomKey(
            @RequestParam String tenant,
            @PathVariable String keyName,
            @RequestParam Integer length,
            @RequestParam IEnumCharSet.Types charSetType) {
        log.info("Call generateRandomKeyName");
        try {
            String keyValue = keyService.getRandomKey(length, charSetType);
            keyService.createOrUpdateKeyByName(tenant, keyName, keyValue);
            return ResponseFactory.responseOk(keyValue);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    /**
     * Get random key.
     *
     * @param tenant  the tenant
     * @param keyName the key name
     * @return the response entity
     */
    @GetMapping("/random/{keyName}")
    public ResponseEntity<String> getRandomKey(
            @RequestParam String tenant,
            @PathVariable String keyName) {
        log.info("Call getRandomKeyName");
        try {
            return ResponseFactory.responseOk(keyService.getKeyByName(tenant, keyName).getValue());
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    // ======================== Helper Methods ========================

    private String getTenant() {
        // Implementation depends on your security context
        return "default-tenant";
    }

    private String getPrincipal() {
        // Implementation depends on your security context
        return "system";
    }

    private String getClientIp() {
        // Implementation depends on your request context
        return "127.0.0.1";
    }
}