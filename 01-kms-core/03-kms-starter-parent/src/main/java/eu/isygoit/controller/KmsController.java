package eu.isygoit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.annotation.InjectExceptionHandler;
import eu.isygoit.api.KmsServiceApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.enums.IKmsActionType;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Full KMS Controller - Implements all methods from KmsServiceApi
 *
 * <p>This controller provides a complete AWS KMS-compatible REST API for:
 * <ul>
 *   <li>Key management (create, describe, list, enable, disable, delete)</li>
 *   <li>Cryptographic operations (encrypt, decrypt, re-encrypt)</li>
 *   <li>Data key generation for envelope encryption</li>
 *   <li>Digital signatures (sign, verify)</li>
 *   <li>Message Authentication Codes (MAC generation and verification)</li>
 *   <li>Key rotation (automatic and manual)</li>
 *   <li>Alias management</li>
 *   <li>Key policies and grants</li>
 *   <li>Tagging for cost allocation</li>
 *   <li>Key material import (BYOK)</li>
 *   <li>Multi-region key replication</li>
 *   <li>Custom key stores</li>
 *   <li>Audit logging and usage statistics</li>
 * </ul>
 * </p>
 *
 * @author Isygoit Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Validated
@RestController
@InjectExceptionHandler(KmsExceptionHandler.class)
@RequestMapping(path = "/api/v1/private/kms")
@Tag(name = "KMS Keys", description = "Key Management Service - All cryptographic operations and key management endpoints")
public class KmsController extends ControllerExceptionHandler implements KmsServiceApi {

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

    @Autowired
    private RequestContextService requestContextService;

    @Autowired
    private IMultiRegionService multiRegionService;

    @Autowired
    private ICustomKeyStoreService customKeyStoreService;

    // ============================================================================
    // KEY MANAGEMENT APIs
    // ============================================================================

    @Override
    public ResponseEntity<CreateKeyResponse> createKey(@Valid @RequestBody CreateKeyRequest request) {
        log.info("Creating key with spec: {} and purpose: {}",
                request.getKeySpec(),
                request.getKeyUsage());

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            CreateKeyResponse response = keyManagementService.createKey(tenant, request);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.CREATE_KEY,
                    String.valueOf(response.getKeyMetadata().getKeyId()),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseCreated(response);

        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<DescribeKeyResponse> describeKey(@PathVariable("keyId") String keyId) {
        log.info("Describing key: {}", keyId);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            DescribeKeyResponse response = keyManagementService.describeKey(tenant, keyId, null);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.DESCRIBE_KEY,
                    keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);

        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ListKeysResponse> listKeys(
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "marker", required = false) String marker) {

        log.info("Listing keys with limit: {}", limit);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            ListKeysResponse response = keyManagementService.listKeys(tenant, limit, marker);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.LIST_KEYS,
                    null,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);

        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<UpdateKeyDescriptionResponse> updateKeyDescription(
            @PathVariable("keyId") String keyId,
            @Valid @RequestBody UpdateKeyDescriptionRequest request) {

        log.info("Updating key description for key: {}", keyId);
        request.setKeyId(keyId); // ensure consistency

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            UpdateKeyDescriptionResponse response =
                    keyManagementService.updateKeyDescription(tenant, keyId, request);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.UPDATE_KEY_METADATA,
                    keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);

        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<EnableKeyResponse> enableKey(@PathVariable("keyId") String keyId) {
        log.info("Enabling key: {}", keyId);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            EnableKeyResponse response = keyManagementService.enableKey(tenant, keyId);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.ENABLE_KEY,
                    keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);

        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<DisableKeyResponse> disableKey(@PathVariable("keyId") String keyId) {
        log.info("Disabling key: {}", keyId);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            DisableKeyResponse response = keyManagementService.disableKey(tenant, keyId);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.DISABLE_KEY,
                    keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);

        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ScheduleKeyDeletionResponse> scheduleKeyDeletion(
            @PathVariable("keyId") String keyId,
            @RequestParam(value = "pendingWindowInDays", defaultValue = "30") Integer pendingWindowInDays) {

        log.info("Scheduling deletion for key: {} with window: {} days", keyId, pendingWindowInDays);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            ScheduleKeyDeletionResponse response =
                    keyManagementService.scheduleKeyDeletion(tenant, keyId, pendingWindowInDays);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.SCHEDULE_KEY_DELETION,
                    keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);

        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<CancelKeyDeletionResponse> cancelKeyDeletion(@PathVariable("keyId") String keyId) {
        log.info("Cancelling deletion for key: {}", keyId);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            CancelKeyDeletionResponse response = keyManagementService.cancelKeyDeletion(tenant, keyId);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.CANCEL_KEY_DELETION,
                    keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);

        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<DeleteKeyResponse> deleteKey(@PathVariable("keyId") String keyId) {
        log.info("Permanently deleting key: {}", keyId);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            keyManagementService.deleteKey(tenant, keyId);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.DELETE_KEY,
                    keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk();

        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    // ============================================================================
    // KEY ROTATION APIs
    // ============================================================================

    @Override
    public ResponseEntity<KeyRotationStatusResponseDto> updateKeyRotation(
            @PathVariable("keyId") String keyId,
            @Valid @RequestBody UpdateKeyRotationRequestDto request) {

        log.info("Updating rotation for key: {}", keyId);
        request.setKeyId(keyId);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            KeyRotationStatusResponseDto response =
                    keyManagementService.updateKeyRotation(tenant, keyId, request);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.UPDATE_KEY_ROTATION,
                    keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);

        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<RotateKeyResponse> rotateKey(@PathVariable("keyId") String keyId) {
        log.info("Rotating key: {}", keyId);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            RotateKeyResponse response = keyManagementService.rotateKey(tenant, keyId);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.ROTATE_KEY,
                    keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);

        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<GetKeyRotationStatusResponse> getKeyRotationStatus(@PathVariable("keyId") String keyId) {
        log.info("Getting rotation status for key: {}", keyId);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            GetKeyRotationStatusResponse response = keyManagementService.getKeyRotationStatus(tenant, keyId);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.GET_KEY_ROTATION_STATUS,
                    keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);

        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ListKeyRotationsResponseDto> listKeyRotations(
            @PathVariable("keyId") String keyId,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "nextToken", required = false) String nextToken) {

        log.info("Listing key rotations for: {}", keyId);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            ListKeyRotationsResponseDto response =
                    keyManagementService.listKeyRotations(tenant, keyId, limit, nextToken);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.LIST_KEY_ROTATIONS,
                    keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);

        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<EnableKeyRotationResponse> enableKeyRotation(@PathVariable("keyId") String keyId) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            UpdateKeyRotationRequestDto internalRequest = UpdateKeyRotationRequestDto.builder()
                    .enableRotation(true)
                    .rotationPeriodDays(365)
                    .applyImmediately(true)
                    .build();
            keyManagementService.updateKeyRotation(tenant, keyId, internalRequest);
            auditService.logAction(tenant, IKmsActionType.Types.UPDATE_KEY_ROTATION, keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(new EnableKeyRotationResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<DisableKeyRotationResponse> disableKeyRotation(@PathVariable("keyId") String keyId) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            UpdateKeyRotationRequestDto internalRequest = UpdateKeyRotationRequestDto.builder()
                    .enableRotation(false)
                    .applyImmediately(true)
                    .build();
            keyManagementService.updateKeyRotation(tenant, keyId, internalRequest);
            auditService.logAction(tenant, IKmsActionType.Types.UPDATE_KEY_ROTATION, keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(new DisableKeyRotationResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    // ============================================================================
    // CRYPTOGRAPHIC OPERATIONS APIs
    // ============================================================================

    @Override
    public ResponseEntity<EncryptResponse> encrypt(@Valid @RequestBody EncryptRequest request) {
        log.info("Encrypting with keyId: {}", request.getKeyId());

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            EncryptResponse response = encryptionService.encrypt(tenant, request);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.ENCRYPT,
                    request.getKeyId(),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);

        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<DecryptResponse> decrypt(@Valid @RequestBody DecryptRequest request) {
        log.info("Decrypting data");

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            DecryptResponse response = encryptionService.decrypt(tenant, request);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.DECRYPT,
                    response.getKeyId(),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);

        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ReEncryptResponse> reEncrypt(@Valid @RequestBody ReEncryptRequest request) {
        log.info("Re-encrypting to destination key: {}", request.getDestinationKeyId());

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            ReEncryptResponse response = encryptionService.reEncrypt(tenant, request);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.REENCRYPT,
                    request.getDestinationKeyId(),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);

        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<GenerateDataKeyResponse> generateDataKey(@Valid @RequestBody GenerateDataKeyRequest request) {
        log.info("Generating data key with keyId: {}", request.getKeyId());

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            GenerateDataKeyResponse response = dataKeyService.generateDataKey(tenant, request);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.GENERATE_DATA_KEY,
                    request.getKeyId(),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);

        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<GenerateDataKeyWithoutPlaintextResponse> generateDataKeyWithoutPlaintext(
            @Valid @RequestBody GenerateDataKeyWithoutPlaintextRequest request) {
        log.info("Generating data key without plaintext");

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            GenerateDataKeyWithoutPlaintextResponse response =
                    dataKeyService.generateDataKeyWithoutPlaintext(tenant, request);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.GENERATE_DATA_KEY_WITHOUT_PLAINTEXT,
                    request.getKeyId(),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);

        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<GenerateDataKeyPairResponse> generateDataKeyPair(
            @Valid @RequestBody GenerateDataKeyPairRequest request) {
        log.info("Generating data key pair");

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            GenerateDataKeyPairResponse response = dataKeyService.generateDataKeyPair(tenant, request);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.GENERATE_DATA_KEY_PAIR,
                    request.getKeyId(),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);

        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<GenerateDataKeyPairWithoutPlaintextResponse> generateDataKeyPairWithoutPlaintext(
            @Valid @RequestBody GenerateDataKeyPairWithoutPlaintextRequest request) {
        log.info("Generating data key pair without plaintext");

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            GenerateDataKeyPairWithoutPlaintextResponse response =
                    dataKeyService.generateDataKeyPairWithoutPlaintext(tenant, request);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.GENERATE_DATA_KEY_PAIR_WITHOUT_PLAINTEXT,
                    request.getKeyId(),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);

        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<SignResponse> sign(@Valid @RequestBody SignRequest request) {
        log.info("Signing with keyId: {}", request.getKeyId());

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            SignResponse response = signingService.sign(tenant, request);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.SIGN,
                    request.getKeyId(),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);

        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<VerifyResponse> verify(@Valid @RequestBody VerifyRequest request) {
        log.info("Verifying signature with keyId: {}", request.getKeyId());
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            VerifyResponse response = signingService.verify(tenant, request);
            auditService.logAction(tenant, IKmsActionType.Types.VERIFY, request.getKeyId(),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<GenerateMacResponse> generateMac(@Valid @RequestBody GenerateMacRequest request) {
        log.info("Generating MAC with keyId: {}", request.getKeyId());
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            GenerateMacResponse response = signingService.generateMac(tenant, request);
            auditService.logAction(tenant, IKmsActionType.Types.GENERATE_MAC, request.getKeyId(),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<VerifyMacResponse> verifyMac(@Valid @RequestBody VerifyMacRequest request) {
        log.info("Verifying MAC with keyId: {}", request.getKeyId());
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            VerifyMacResponse response = signingService.verifyMac(tenant, request);
            auditService.logAction(tenant, IKmsActionType.Types.VERIFY_MAC, request.getKeyId(),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<GetPublicKeyResponse> getPublicKey(@PathVariable("keyId") String keyId) {
        log.info("Getting public key for: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            GetPublicKeyResponse response = keyManagementService.getPublicKey(tenant, keyId);
            auditService.logAction(tenant, IKmsActionType.Types.GET_PUBLIC_KEY, keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    // ============================================================================
    // KEY VERSIONING & MULTI-REGION APIs
    // ============================================================================

    @Override
    public ResponseEntity<ListKeyVersionsResponse> listKeyVersions(
            @PathVariable("keyId") String keyId,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "marker", required = false) String marker) {

        log.info("Listing key versions for: {}", keyId);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            ListKeyVersionsResponse response =
                    keyVersionService.listKeyVersions(tenant, keyId, limit, marker);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.LIST_KEY_VERSIONS,
                    keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);

        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ActiveVersionResponseDto> getActiveVersion(@PathVariable("keyId") String keyId) {
        log.info("Getting active version for: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            ActiveVersionResponseDto response = keyVersionService.getActiveVersion(tenant, keyId);
            auditService.logAction(tenant, IKmsActionType.Types.GET_ACTIVE_VERSION, keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<UpdatePrimaryRegionResponse> updatePrimaryRegion(
            @PathVariable("keyId") String keyId,
            @Valid @RequestBody UpdatePrimaryRegionRequestDto request) {

        log.info("Updating primary region for key: {} to region: {}", keyId, request.getPrimaryRegion());
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            UpdatePrimaryRegionResponse response = multiRegionService.updatePrimaryRegion(tenant, keyId, request);
            auditService.logAction(tenant, IKmsActionType.Types.UPDATE_PRIMARY_REGION, keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ReplicateKeyResponse> replicateKey(
            @PathVariable("keyId") String keyId,
            @Valid @RequestBody ReplicateKeyRequestDto request) {

        log.info("Replicating key: {} to region: {}", keyId, request.getReplicaRegion());
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            ReplicateKeyResponse response = multiRegionService.replicateKey(tenant, keyId, request);
            auditService.logAction(tenant, IKmsActionType.Types.REPLICATE_KEY, keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<SynchronizeMultiRegionKeyResponse> synchronizeMultiRegionKey(
            @PathVariable("keyId") String keyId) {

        log.info("Synchronizing multi-region key: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            SynchronizeMultiRegionKeyResponse response = multiRegionService.synchronizeMultiRegionKey(tenant, keyId);
            auditService.logAction(tenant, IKmsActionType.Types.SYNCHRONIZE_MULTI_REGION_KEY, keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    // =========================================================================
    // Aliases
    // =========================================================================

    @Override
    public ResponseEntity<CreateAliasResponse> createAlias(@Valid @RequestBody CreateAliasRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            CreateAliasRequestDto internalReq = CreateAliasRequestDto.builder()
                    .aliasName(request.getAliasName())
                    .targetKeyId(request.getTargetKeyId())
                    .build();
            keyManagementService.createAlias(tenant, internalReq);
            auditService.logAction(tenant, IKmsActionType.Types.CREATE_ALIAS, request.getTargetKeyId(),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(new CreateAliasResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<UpdateAliasResponse> updateAlias(
            @PathVariable("aliasName") String aliasName,
            @Valid @RequestBody UpdateAliasRequest request) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            UpdateAliasRequestDto internalReq = UpdateAliasRequestDto.builder()
                    .targetKeyId(request.getTargetKeyId())
                    .build();
            keyManagementService.updateAlias(tenant, aliasName, internalReq);
            auditService.logAction(tenant, IKmsActionType.Types.UPDATE_ALIAS, aliasName,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(new UpdateAliasResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<DeleteAliasResponse> deleteAlias(@PathVariable("aliasName") String aliasName) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            keyManagementService.deleteAlias(tenant, aliasName);
            auditService.logAction(tenant, IKmsActionType.Types.DELETE_ALIAS, aliasName,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(new DeleteAliasResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ListAliasesResponse> listAliases(
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "marker", required = false) String marker) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            ListAliasesResponseDto internal = keyManagementService.listAliases(tenant, limit, marker);
            ListAliasesResponse response = ListAliasesResponse.builder()
                    .aliases(internal.getAliases().stream()
                            .map(a -> ListAliasesResponse.AliasEntry.builder()
                                    .aliasName(a.getAliasName())
                                    .aliasArn(buildAliasArn(a.getAliasName()))
                                    .targetKeyId(a.getTargetKeyId())
                                    .creationDate(formatDate(a.getCreatedAt()))
                                    .lastUpdatedDate(formatDate(a.getUpdatedAt()))
                                    .build())
                            .collect(Collectors.toList()))
                    .nextMarker(internal.getNextMarker())
                    .truncated(internal.getTruncated())
                    .build();
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ListAliasesResponse> listAliasesForKey(
            @PathVariable("keyId") String keyId,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "marker", required = false) String marker) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            ListAliasesResponseDto internal = keyManagementService.listAliasesForKey(tenant, keyId, limit, marker);
            ListAliasesResponse response = ListAliasesResponse.builder()
                    .aliases(internal.getAliases().stream()
                            .map(a -> ListAliasesResponse.AliasEntry.builder()
                                    .aliasName(a.getAliasName())
                                    .aliasArn(buildAliasArn(a.getAliasName()))
                                    .targetKeyId(a.getTargetKeyId())
                                    .creationDate(formatDate(a.getCreatedAt()))
                                    .lastUpdatedDate(formatDate(a.getUpdatedAt()))
                                    .build())
                            .collect(Collectors.toList()))
                    .nextMarker(internal.getNextMarker())
                    .truncated(internal.getTruncated())
                    .build();
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    private String buildAliasArn(String aliasName) {
        String accountId = "123456789012"; // replace with real account ID
        String region = "us-east-1";
        return String.format("arn:aws:kms:%s:%s:alias/%s", region, accountId, aliasName);
    }

    private String formatDate(LocalDateTime date) {
        return date != null ? date.format(DateTimeFormatter.ISO_DATE_TIME) : null;
    }

    // =========================================================================
    // Key Policies
    // =========================================================================

    @Override
    public ResponseEntity<PutKeyPolicyResponse> putKeyPolicy(
            @PathVariable("keyId") String keyId,
            @Valid @RequestBody PutKeyPolicyRequest request) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            SetKeyPolicyRequestDto internal = SetKeyPolicyRequestDto.builder()
                    .policy(request.getPolicy())
                    .policyName(request.getPolicyName())
                    .bypassPolicyLockoutSafetyCheck(request.getBypassPolicyLockoutSafetyCheck())
                    .build();
            keyPolicyService.setKeyPolicy(tenant, keyId, internal);
            auditService.logAction(tenant, IKmsActionType.Types.SET_KEY_POLICY, keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(new PutKeyPolicyResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<GetKeyPolicyResponse> getKeyPolicy(
            @PathVariable("keyId") String keyId,
            @RequestParam(value = "policyName", defaultValue = "default") String policyName) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            Map<String, Object> policyMap = keyPolicyService.getKeyPolicy(tenant, keyId);
            String policyJson = new ObjectMapper().writeValueAsString(policyMap);
            GetKeyPolicyResponse response = GetKeyPolicyResponse.builder().policy(policyJson).build();
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ListKeyPoliciesResponse> listKeyPolicies(
            @PathVariable("keyId") String keyId,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "marker", required = false) String marker) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        ListKeyPoliciesResponse response = keyPolicyService.listKeyPolicies(tenant, keyId, limit, marker);
        return ResponseFactory.responseOk(response);
    }

    // =========================================================================
    // Grants
    // =========================================================================

    @Override
    public ResponseEntity<CreateGrantResponse> createGrant(
            @PathVariable("keyId") String keyId,
            @Valid @RequestBody CreateGrantRequest request) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            CreateGrantRequestDto internal = CreateGrantRequestDto.builder()
                    .principal(request.getGranteePrincipal())
                    .granteePrincipal(request.getGranteePrincipal())
                    .operations(request.getOperations())
                    .build();

            GrantResponseDto internalRes = keyPolicyService.createGrant(tenant, keyId, internal);

            auditService.logAction(tenant, IKmsActionType.Types.CREATE_GRANT, keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());

            CreateGrantResponse response = CreateGrantResponse.builder()
                    .grantId(internalRes.getGrantId())
                    .grantToken(internalRes.getGrantToken())
                    .build();

            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ListGrantsResponse> listGrants(
            @PathVariable("keyId") String keyId,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "marker", required = false) String marker,
            @RequestParam(value = "grantId", required = false) String grantId,
            @RequestParam(value = "granteePrincipal", required = false) String granteePrincipal) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            ListGrantsResponseDto internal = keyPolicyService.listGrants(tenant, keyId, limit, marker);

            ListGrantsResponse response = ListGrantsResponse.builder()
                    .grants(internal.getGrants().stream()
                            .map(g -> ListGrantsResponse.Grant.builder()
                                    .grantId(g.getGrantId())
                                    .granteePrincipal(g.getGranteePrincipal())
                                    .retiringPrincipal(g.getRetiringPrincipal())
                                    .operations(g.getOperations())
                                    .constraints(null)
                                    .creationDate(formatDate(g.getCreatedAt()))
                                    .lastUpdatedDate(formatDate(g.getCreatedAt()))
                                    .keyId(keyId)
                                    .name(null)
                                    .build())
                            .collect(Collectors.toList()))
                    .nextMarker(internal.getNextToken())
                    .truncated(internal.getGrants().size() < (limit != null ? limit : Integer.MAX_VALUE))
                    .build();
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<RevokeGrantResponse> revokeGrant(
            @PathVariable("keyId") String keyId,
            @PathVariable("grantId") String grantId) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            keyPolicyService.revokeGrant(tenant, keyId, grantId);
            auditService.logAction(tenant, IKmsActionType.Types.REVOKE_GRANT, keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(new RevokeGrantResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<RetireGrantResponse> retireGrant(@Valid @RequestBody RetireGrantRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            RetireGrantRequestDto internal = RetireGrantRequestDto.builder()
                    .grantToken(request != null ? request.getGrantToken() : null)
                    .build();
            keyPolicyService.retireGrant(tenant, request.getGrantId(), internal);
            auditService.logAction(tenant, IKmsActionType.Types.RETIRE_GRANT, request.getGrantId(),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(new RetireGrantResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ListRetirableGrantsResponse> listRetirableGrants(
            @RequestParam("retiringPrincipal") String retiringPrincipal,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "marker", required = false) String marker) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        ListRetirableGrantsResponse response = keyPolicyService.listRetirableGrants(tenant, retiringPrincipal, limit, marker);

        return ResponseFactory.responseOk(response);
    }

    // =========================================================================
    // Tags
    // =========================================================================

    @Override
    public ResponseEntity<TagResourceResponse> tagResource(
            @PathVariable("keyId") String keyId,
            @Valid @RequestBody TagResourceRequest request) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            TagResourceRequestDto internal = TagResourceRequestDto.builder()
                    .tags(request.getTags().stream()
                            .collect(Collectors.toMap(
                                    ListResourceTagsResponse.Tag::getTagKey,
                                    ListResourceTagsResponse.Tag::getTagValue)))
                    .build();
            keyManagementService.tagResource(tenant, keyId, internal);
            auditService.logAction(tenant, IKmsActionType.Types.TAG_RESOURCE, keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(new TagResourceResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<UntagResourceResponse> untagResource(
            @PathVariable("keyId") String keyId,
            @Valid @RequestBody UntagResourceRequest request) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            UntagResourceRequestDto internal = UntagResourceRequestDto.builder()
                    .tagKeys(request.getTagKeys())
                    .build();
            keyManagementService.untagResource(tenant, keyId, internal);
            auditService.logAction(tenant, IKmsActionType.Types.UNTAG_RESOURCE, keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(new UntagResourceResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ListResourceTagsResponse> listResourceTags(
            @PathVariable("keyId") String keyId,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "marker", required = false) String marker) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            ListTagsResponseDto internal = keyManagementService.listResourceTags(tenant, keyId);
            ListResourceTagsResponse response = ListResourceTagsResponse.builder()
                    .tags(internal.getTags().stream()
                            .map(t -> ListResourceTagsResponse.Tag.builder()
                                    .tagKey(t.getTagKey())
                                    .tagValue(t.getTagValue())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    // =========================================================================
    // BYOK
    // =========================================================================

    @Override
    public ResponseEntity<GetParametersForImportResponse> getParametersForImport(
            @PathVariable("keyId") String keyId,
            @Valid @RequestBody GetParametersForImportRequest request) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            ImportParametersResponseDto internal = keyManagementService.getParametersForImport(tenant, keyId);
            GetParametersForImportResponse response = GetParametersForImportResponse.builder()
                    .keyId(internal.getKeyId())
                    .importToken(Arrays.toString(internal.getImportToken()))
                    .publicKey(Arrays.toString(internal.getWrappingKey()))
                    .validTo(internal.getValidTo())
                    .build();
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ImportKeyMaterialResponse> importKeyMaterial(
            @PathVariable("keyId") String keyId,
            @Valid @RequestBody ImportKeyMaterialRequest request) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            ImportKeyMaterialRequestDto internal = ImportKeyMaterialRequestDto.builder()
                    .importToken(request.getImportToken().getBytes())
                    .encryptedKeyMaterial(request.getEncryptedKeyMaterial().getBytes())
                    .validTo(request.getValidTo())
                    .expirationModel(request.getExpirationModel())
                    .build();
            keyManagementService.importKeyMaterial(tenant, keyId, internal);
            auditService.logAction(tenant, IKmsActionType.Types.IMPORT_KEY_MATERIAL, keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(new ImportKeyMaterialResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<DeleteImportedKeyMaterialResponse> deleteImportedKeyMaterial(
            @PathVariable("keyId") String keyId) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            keyManagementService.deleteImportedKeyMaterial(tenant, keyId);
            auditService.logAction(tenant, IKmsActionType.Types.DELETE_IMPORTED_KEY_MATERIAL, keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(new DeleteImportedKeyMaterialResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    // =========================================================================
    // Custom Key Stores
    // =========================================================================

    @Override
    public ResponseEntity<CreateCustomKeyStoreResponse> createCustomKeyStore(
            @Valid @RequestBody CreateCustomKeyStoreRequest request) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            CreateCustomKeyStoreRequestDto internal = CreateCustomKeyStoreRequestDto.builder()
                    .keyStoreName(request.getCustomKeyStoreName())
                    .cloudHsmClusterId(request.getCloudHsmClusterId())
                    .keyStorePassword(request.getKeyStorePassword())
                    .trustAnchorCertificate(request.getTrustAnchorCertificate())
                    .type(request.getCustomKeyStoreType())
                    .xksProxyUriEndpoint(request.getXksProxyUriEndpoint())
                    .xksProxyUriPath(request.getXksProxyUriPath())
                    .xksProxyAuthenticationCredential(request.getXksProxyAuthenticationCredential())
                    .xksProxyConnectivity(request.getXksProxyConnectivity())
                    .build();
            CustomKeyStoreResponseDto internalRes = customKeyStoreService.createCustomKeyStore(tenant, internal);
            CreateCustomKeyStoreResponse response = CreateCustomKeyStoreResponse.builder()
                    .customKeyStoreId(internalRes.getKeyStoreId())
                    .build();
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<DescribeCustomKeyStoreResponse> describeCustomKeyStore(
            @PathVariable("customKeyStoreId") Long customKeyStoreId) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            CustomKeyStoreResponseDto internal = customKeyStoreService.describeCustomKeyStore(tenant, customKeyStoreId);
            DescribeCustomKeyStoreResponse response = toAwsResponse(internal);
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<UpdateCustomKeyStoreResponse> updateCustomKeyStore(
            @PathVariable("customKeyStoreId") Long customKeyStoreId,
            @Valid @RequestBody UpdateCustomKeyStoreRequest request) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            UpdateCustomKeyStoreRequestDto internal = UpdateCustomKeyStoreRequestDto.builder()
                    .newCustomKeyStoreName(request.getNewCustomKeyStoreName())
                    .keyStorePassword(request.getKeyStorePassword())
                    .cloudHsmClusterId(request.getCloudHsmClusterId())
                    .xksProxyUriEndpoint(request.getXksProxyUriEndpoint())
                    .xksProxyUriPath(request.getXksProxyUriPath())
                    .xksProxyAuthenticationCredential(request.getXksProxyAuthenticationCredential())
                    .xksProxyConnectivity(request.getXksProxyConnectivity())
                    .build();
            customKeyStoreService.updateCustomKeyStore(tenant, customKeyStoreId, internal);
            return ResponseFactory.responseOk(new UpdateCustomKeyStoreResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<DeleteCustomKeyStoreResponse> deleteCustomKeyStore(
            @PathVariable("customKeyStoreId") Long customKeyStoreId) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            customKeyStoreService.deleteCustomKeyStore(tenant, customKeyStoreId);
            return ResponseFactory.responseOk(new DeleteCustomKeyStoreResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ConnectCustomKeyStoreResponse> connectCustomKeyStore(
            @PathVariable("customKeyStoreId") Long customKeyStoreId) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            customKeyStoreService.connectCustomKeyStore(tenant, customKeyStoreId);
            return ResponseFactory.responseOk(new ConnectCustomKeyStoreResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<DisconnectCustomKeyStoreResponse> disconnectCustomKeyStore(
            @PathVariable("customKeyStoreId") Long customKeyStoreId) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            customKeyStoreService.disconnectCustomKeyStore(tenant, customKeyStoreId);
            return ResponseFactory.responseOk(new DisconnectCustomKeyStoreResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ListCustomKeyStoresResponse> listCustomKeyStores(
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "marker", required = false) String marker,
            @RequestParam(value = "customKeyStoreId", required = false) Long customKeyStoreId,
            @RequestParam(value = "customKeyStoreName", required = false) String customKeyStoreName) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            ListCustomKeyStoresResponseDto internal = customKeyStoreService.listCustomKeyStores(tenant, limit, marker);
            ListCustomKeyStoresResponse response = ListCustomKeyStoresResponse.builder()
                    .customKeyStores(internal.getCustomKeyStores().stream()
                            .map(s -> DescribeCustomKeyStoreResponse.CustomKeyStore.builder()
                                    .customKeyStoreId(s.getKeyStoreId())
                                    .customKeyStoreName(s.getKeyStoreName())
                                    .creationDate(s.getCreatedAt())
                                    .connectionState(s.getConnectionState())
                                    .build())
                            .collect(Collectors.toList()))
                    .nextMarker(internal.getNextMarker())
                    .truncated(internal.isTruncated())
                    .build();
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    private DescribeCustomKeyStoreResponse toAwsResponse(CustomKeyStoreResponseDto internal) {
        DescribeCustomKeyStoreResponse.CustomKeyStore store =
                DescribeCustomKeyStoreResponse.CustomKeyStore.builder()
                        .customKeyStoreId(internal.getKeyStoreId())
                        .customKeyStoreName(internal.getKeyStoreName())
                        .creationDate(internal.getCreatedAt())
                        .connectionState(internal.getStatus() != null ? internal.getStatus().name() : null)
                        .cloudHsmClusterId(internal.getCloudHsmClusterId())
                        .customKeyStoreType(internal.getType() != null ? internal.getType().name() : null)
                        .xksProxyUriEndpoint(internal.getXksProxyUriEndpoint())
                        .xksProxyUriPath(internal.getXksProxyUriPath())
                        .xksProxyAuthenticationCredential(null)
                        .xksProxyConnectivity(null)
                        .build();

        return DescribeCustomKeyStoreResponse.builder().customKeyStore(store).build();
    }

    // =========================================================================
    // Audit & Utility
    // =========================================================================

    @Override
    public ResponseEntity<AuditLogResponse> getAuditLogs(
            @PathVariable("keyId") String keyId,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(value = "limit", required = false) Integer limit) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            AuditLogResponseDto internal = auditService.getAuditLogs(tenant, keyId, fromDate, toDate, limit);
            AuditLogResponse response = AuditLogResponse.builder()
                    .logs(internal.getLogs().stream()
                            .map(l -> AuditLogResponse.LogEntry.builder()
                                    .timestamp(l.getTimestamp())
                                    .action(l.getAction())
                                    .keyId(l.getKeyId())
                                    .principal(l.getPrincipal())
                                    .ipAddress(l.getIpAddress())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<KeyUsageStatsResponse> getKeyUsageStats(@PathVariable("keyId") String keyId) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            KeyUsageStatsResponseDto internal = keyManagementService.getKeyUsageStats(tenant, keyId);
            KeyUsageStatsResponse response = KeyUsageStatsResponse.builder()
                    .keyId(internal.getKeyId())
                    .encryptCount(internal.getEncryptCount())
                    .decryptCount(internal.getDecryptCount())
                    .signCount(internal.getSignCount())
                    .verifyCount(internal.getVerifyCount())
                    .lastUsedDate(internal.getLastUsedDate())
                    .build();
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<GenerateRandomResponse> generateRandom(@Valid @RequestBody GenerateRandomRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            GenerateRandomResponse response = dataKeyService.generateRandom(request);
            auditService.logAction(tenant, IKmsActionType.Types.GENERATE_RANDOM_DATA, null,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ValidateKeyResponse> validateKey(@PathVariable("keyId") String keyId) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            keyManagementService.isValidKey(tenant, keyId);
            ValidateKeyResponse response = ValidateKeyResponse.builder().valid(true).build();
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }
}