package eu.isygoit.controller;

import com.fasterxml.jackson.core.type.TypeReference;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Full KMS Controller - Implements all methods from KmsServiceApi
 *
 * <p>This controller provides a complete WAMS KMS-compatible REST API for:
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

    private ObjectMapper objectMapper = new ObjectMapper();

    // ============================================================================
    // KEY MANAGEMENT APIs
    // ============================================================================

    @Override
    public ResponseEntity<CreateKeyResponse> createKey(CreateKeyRequest request) {
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
    public ResponseEntity<DescribeKeyResponse> describeKey(String keyId) {
        log.info("Describing key: {}", keyId);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            keyId = dataKeyService.resolveKeyId(tenant, keyId);

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
            Integer limit,
            String nextToken) {

        log.info("Listing keys with limit: {}", limit);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            ListKeysResponse response = keyManagementService.listKeys(tenant, limit, nextToken);

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
            String keyId,
            UpdateKeyDescriptionRequest request) {

        log.info("Updating key description for key: {}", keyId);
        request.setKeyId(keyId); // ensure consistency

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            request.setKeyId(dataKeyService.resolveKeyId(tenant, request.getKeyId()));

            UpdateKeyDescriptionResponse response =
                    keyManagementService.updateKeyDescription(tenant, keyId, request);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.UPDATE_KEY_DESCRIPTION,
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
    public ResponseEntity<EnableKeyResponse> enableKey(String keyId) {
        log.info("Enabling key: {}", keyId);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            keyId = dataKeyService.resolveKeyId(tenant, keyId);

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
    public ResponseEntity<DisableKeyResponse> disableKey(String keyId) {
        log.info("Disabling key: {}", keyId);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            keyId = dataKeyService.resolveKeyId(tenant, keyId);

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
    public ResponseEntity<DisableKeyVersionResponse> disableKeyVersion(String keyId, String keyVersionId) {
        log.info("Disabling key: {} v: {}", keyId, keyVersionId);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            keyId = dataKeyService.resolveKeyId(tenant, keyId);

            DisableKeyVersionResponse response = keyVersionService.disableKeyVersion(tenant, keyId, keyVersionId);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.DISABLE_KEY_VERSION,
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
    public ResponseEntity<EnableKeyVersionResponse> enableKeyVersion(String keyId, String keyVersionId) {
        log.info("Disabling key: {} v: {}", keyId, keyVersionId);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            keyId = dataKeyService.resolveKeyId(tenant, keyId);

            EnableKeyVersionResponse response = keyVersionService.enableKeyVersion(tenant, keyId, keyVersionId);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.ENABLE_KEY_VERSION,
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
            String keyId,
            Integer pendingWindowInDays) {

        log.info("Scheduling deletion for key: {} with window: {} days", keyId, pendingWindowInDays);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            keyId = dataKeyService.resolveKeyId(tenant, keyId);

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
    public ResponseEntity<CancelKeyDeletionResponse> cancelKeyDeletion(String keyId) {
        log.info("Cancelling deletion for key: {}", keyId);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            keyId = dataKeyService.resolveKeyId(tenant, keyId);

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
    public ResponseEntity<DeleteKeyResponse> deleteKey(String keyId) {
        log.info("Permanently deleting key: {}", keyId);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            keyId = dataKeyService.resolveKeyId(tenant, keyId);

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
    public ResponseEntity<UpdateKeyRotationResponse> updateKeyRotation(
            String keyId,
            UpdateKeyRotationRequest request) {

        log.info("Updating rotation for key: {}", keyId);
        request.setKeyId(keyId);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            request.setKeyId(dataKeyService.resolveKeyId(tenant, request.getKeyId()));

            UpdateKeyRotationResponse response =
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
    public ResponseEntity<RotateKeyResponse> rotateKey(String keyId) {
        log.info("Rotating key: {}", keyId);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            keyId = dataKeyService.resolveKeyId(tenant, keyId);

            RotateKeyResponse response = keyManagementService.rotateKey(tenant, keyId);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.ROTATE_KEY_ON_DEMAND,
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
    public ResponseEntity<GetKeyRotationStatusResponse> getKeyRotationStatus(String keyId) {
        log.info("Getting rotation status for key: {}", keyId);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            keyId = dataKeyService.resolveKeyId(tenant, keyId);

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
    public ResponseEntity<ListKeyRotationsResponse> listKeyRotations(
            String keyId,
            Integer limit,
            String nextToken) {

        log.info("Listing key rotations for: {}", keyId);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            ListKeyRotationsResponse response =
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
    public ResponseEntity<EnableKeyRotationResponse> enableKeyRotation(String keyId) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        keyId = dataKeyService.resolveKeyId(tenant, keyId);
        try {
            UpdateKeyRotationRequest internalRequest = UpdateKeyRotationRequest.builder()
                    .enableRotation(true)
                    .rotationPeriodInDays(365)
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
    public ResponseEntity<DisableKeyRotationResponse> disableKeyRotation(String keyId) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        keyId = dataKeyService.resolveKeyId(tenant, keyId);
        try {
            UpdateKeyRotationRequest internalRequest = UpdateKeyRotationRequest.builder()
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
    public ResponseEntity<EncryptResponse> encrypt(EncryptRequest request) {
        log.info("Encrypting with keyId: {}", request.getKeyId());

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            request.setKeyId(dataKeyService.resolveKeyId(tenant, request.getKeyId()));

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
    public ResponseEntity<DecryptResponse> decrypt(DecryptRequest request) {
        log.info("Decrypting data");

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            request.setKeyId(dataKeyService.resolveKeyId(tenant, request.getKeyId()));

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
    public ResponseEntity<ReEncryptResponse> reEncrypt(ReEncryptRequest request) {
        log.info("Re-encrypting to destination key: {}", request.getDestinationKeyId());

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            request.setSourceKeyId(dataKeyService.resolveKeyId(tenant, request.getSourceKeyId()));
            request.setDestinationKeyId(dataKeyService.resolveKeyId(tenant, request.getDestinationKeyId()));

            ReEncryptResponse response = encryptionService.reEncrypt(tenant, request);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.RE_ENCRYPT,
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
    public ResponseEntity<GenerateDataKeyResponse> generateDataKey(GenerateDataKeyRequest request) {
        log.info("Generating data key with keyId: {}", request.getKeyId());

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            request.setKeyId(dataKeyService.resolveKeyId(tenant, request.getKeyId()));

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
            GenerateDataKeyWithoutPlaintextRequest request) {
        log.info("Generating data key without plaintext");

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            request.setKeyId(dataKeyService.resolveKeyId(tenant, request.getKeyId()));

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
            GenerateDataKeyPairRequest request) {
        log.info("Generating data key pair");

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            request.setKeyId(dataKeyService.resolveKeyId(tenant, request.getKeyId()));

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
            GenerateDataKeyPairWithoutPlaintextRequest request) {
        log.info("Generating data key pair without plaintext");

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            request.setKeyId(dataKeyService.resolveKeyId(tenant, request.getKeyId()));

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
    public ResponseEntity<SignResponse> sign(SignRequest request) {
        log.info("Signing with keyId: {}", request.getKeyId());

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            request.setKeyId(dataKeyService.resolveKeyId(tenant, request.getKeyId()));

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
    public ResponseEntity<VerifyResponse> verify(VerifyRequest request) {
        log.info("Verifying signature with keyId: {}", request.getKeyId());
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            request.setKeyId(dataKeyService.resolveKeyId(tenant, request.getKeyId()));
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
    public ResponseEntity<GenerateMacResponse> generateMac(GenerateMacRequest request) {
        log.info("Generating MAC with keyId: {}", request.getKeyId());
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            request.setKeyId(dataKeyService.resolveKeyId(tenant, request.getKeyId()));
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
    public ResponseEntity<VerifyMacResponse> verifyMac(VerifyMacRequest request) {
        log.info("Verifying MAC with keyId: {}", request.getKeyId());
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            request.setKeyId(dataKeyService.resolveKeyId(tenant, request.getKeyId()));
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
    public ResponseEntity<GetPublicKeyResponse> getPublicKey(String keyId) {
        log.info("Getting public key for: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            keyId = dataKeyService.resolveKeyId(tenant, keyId);
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
            String keyId,
            Integer limit,
            String nextToken) {

        log.info("Listing key versions for: {}", keyId);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            keyId = dataKeyService.resolveKeyId(tenant, keyId);

            ListKeyVersionsResponse response =
                    keyVersionService.listKeyVersions(tenant, keyId, limit, nextToken);

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
    public ResponseEntity<ActiveVersionResponse> getActiveVersion(String keyId) {
        log.info("Getting active version for: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            keyId = dataKeyService.resolveKeyId(tenant, keyId);
            ActiveVersionResponse response = keyVersionService.getActiveVersion(tenant, keyId);
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
            String keyId,
            UpdatePrimaryRegionRequest request) {

        log.info("Updating primary region for key: {} to region: {}", keyId, request.getPrimaryRegion());
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            keyId = dataKeyService.resolveKeyId(tenant, keyId);
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
            String keyId,
            ReplicateKeyRequest request) {

        log.info("Replicating key: {} to region: {}", keyId, request.getReplicaRegion());
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            keyId = dataKeyService.resolveKeyId(tenant, keyId);
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
            String keyId) {

        log.info("Synchronizing multi-region key: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            keyId = dataKeyService.resolveKeyId(tenant, keyId);
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
    public ResponseEntity<CreateAliasResponse> createAlias(CreateAliasRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            CreateAliasRequest internalReq = CreateAliasRequest.builder()
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
            UpdateAliasRequest request) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            UpdateAliasRequest internalReq = UpdateAliasRequest.builder()
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
            Integer limit,
            String nextToken) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();

        try {
            ListAliasesResponse response = keyManagementService.listAliases(tenant, limit, nextToken);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.LIST_ALIASES,
                    null,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ListAliasesResponse> listAliasesForKey(
            String keyId,
            Integer limit,
            String nextToken) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        keyId = dataKeyService.resolveKeyId(tenant, keyId);
        try {
            ListAliasesResponse response = keyManagementService.listAliasesForKey(tenant, keyId, limit, nextToken);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.LIST_ALIASES_FOR_KEY,
                    String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    private String buildAliasWrn(String aliasName) {
        String accountId = "123456789012"; // replace with real account ID
        String region = "us-east-1";
        return String.format("wrn:wams:kms:%s:%s:alias:%s", region, accountId, aliasName);
    }

    private String formatDate(LocalDateTime date) {
        return date != null ? date.format(DateTimeFormatter.ISO_DATE_TIME) : null;
    }

    // =========================================================================
    // Key Policies
    // =========================================================================

    @Override
    public ResponseEntity<PutKeyPolicyResponse> putKeyPolicy(
            String keyId,
            PutKeyPolicyRequest request) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        request.setKeyId(dataKeyService.resolveKeyId(tenant, request.getKeyId()));
        try {
            SetKeyPolicyRequest internal = SetKeyPolicyRequest.builder()
                    .policy(request.getPolicy())
                    .bypassPolicyLockoutSafetyCheck(request.getBypassPolicyLockoutSafetyCheck())
                    .build();
            keyPolicyService.setKeyPolicy(tenant, keyId, internal);
            auditService.logAction(tenant, IKmsActionType.Types.PUT_KEY_POLICY, keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(new PutKeyPolicyResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<GetKeyPolicyResponse> getKeyPolicy(
            String keyId) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        keyId = dataKeyService.resolveKeyId(tenant, keyId);
        try {
            Map<String, Object> policyMap = keyPolicyService.getKeyPolicy(tenant, keyId);
            String policyJson = new ObjectMapper().writeValueAsString(policyMap);
            GetKeyPolicyResponse response = GetKeyPolicyResponse.builder().policy(objectMapper.readValue(
                    policyJson,
                    new TypeReference<Map<String, Object>>() {
                    }
            )).build();
            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.GET_KEY_POLICY,
                    String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    // =========================================================================
    // Grants
    // =========================================================================

    @Override
    public ResponseEntity<CreateGrantResponse> createGrant(
            String keyId,
            CreateGrantRequest request) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        request.setKeyId(dataKeyService.resolveKeyId(tenant, request.getKeyId()));
        try {
            CreateGrantRequest internal = CreateGrantRequest.builder()
                    .granteePrincipal(request.getGranteePrincipal())
                    .retiringPrincipal(request.getRetiringPrincipal())
                    .operations(request.getOperations())
                    .build();

            GrantResponse internalRes = keyPolicyService.createGrant(tenant, keyId, internal);

            auditService.logAction(tenant, IKmsActionType.Types.CREATE_GRANT, keyId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());

            CreateGrantResponse response = CreateGrantResponse.builder()
                    .grantId(internalRes.getGrantId())
                    .grantToken(internalRes.getGrantToken())
                    .build();

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.CREATE_GRANT,
                    String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ListGrantsResponse> listGrants(
            String keyId,
            Integer limit,
            String nextToken,
            String grantId,
            String granteePrincipal) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        keyId = dataKeyService.resolveKeyId(tenant, keyId);
        try {
            ListGrantsResponse response = keyPolicyService.listGrants(tenant, keyId, limit, nextToken);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.LIST_GRANTS,
                    String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<RevokeGrantResponse> revokeGrant(
            String keyId,
            String grantId) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        keyId = dataKeyService.resolveKeyId(tenant, keyId);
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
    public ResponseEntity<RetireGrantResponse> retireGrant(RetireGrantRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        request.setKeyId(dataKeyService.resolveKeyId(tenant, request.getKeyId()));
        try {
            RetireGrantRequest internal = RetireGrantRequest.builder()
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
            String retiringPrincipal,
            Integer limit,
            String nextToken) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        ListRetirableGrantsResponse response = keyPolicyService.listRetirableGrants(tenant, retiringPrincipal, limit, nextToken);

        auditService.logAction(
                tenant,
                IKmsActionType.Types.LIST_RETIRED_GRANTS,
                String.valueOf(retiringPrincipal),
                requestContextService.getCurrentContext().getSenderUser(),
                requestContextService.getCurrentContext().getClientIp()
        );

        return ResponseFactory.responseOk(response);
    }

    // =========================================================================
    // Tags
    // =========================================================================

    @Override
    public ResponseEntity<TagResourceResponse> tagResource(
            String keyId,
            TagResourceRequest request) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        request.setKeyId(dataKeyService.resolveKeyId(tenant, request.getKeyId()));
        try {
            keyManagementService.tagResource(tenant, keyId, request);
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
            String keyId,
            UntagResourceRequest request) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        request.setKeyId(dataKeyService.resolveKeyId(tenant, request.getKeyId()));
        try {
            UntagResourceRequest internal = UntagResourceRequest.builder()
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
            String keyId,
            Integer limit,
            String nextToken) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        keyId = dataKeyService.resolveKeyId(tenant, keyId);
        try {
            ListTagsResponse internal = keyManagementService.listResourceTags(tenant, keyId);
            ListResourceTagsResponse response = ListResourceTagsResponse.builder()
                    .tags(internal.getTags().stream()
                            .map(t -> ListResourceTagsResponse.Tag.builder()
                                    .tagKey(t.getTagKey())
                                    .tagValue(t.getTagValue())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.LIST_RESOURCE_TAGS,
                    String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

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
            String keyId,
            GetParametersForImportRequest request) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        keyId = dataKeyService.resolveKeyId(tenant, keyId);
        try {
            ImportParametersResponse internal = keyManagementService.getParametersForImport(tenant, keyId);
            GetParametersForImportResponse response = GetParametersForImportResponse.builder()
                    .keyId(internal.getKeyId())
                    .importToken(Arrays.toString(internal.getImportToken()))
                    .publicKey(Arrays.toString(internal.getWrappingKey().publicKey()))
                    .validTo(internal.getValidTo())
                    .build();

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.GET_PARAMETERS_FOR_IMPORT,
                    String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ImportKeyMaterialResponse> importKeyMaterial(
            String keyId,
            ImportKeyMaterialRequest request) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        request.setKeyId(dataKeyService.resolveKeyId(tenant, request.getKeyId()));
        try {
            ImportKeyMaterialRequest internal = ImportKeyMaterialRequest.builder()
                    .importToken(request.getImportToken())
                    .encryptedKeyMaterial(request.getEncryptedKeyMaterial())
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
            String keyId) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        keyId = dataKeyService.resolveKeyId(tenant, keyId);
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
            CreateCustomKeyStoreRequest request) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            CreateCustomKeyStoreRequest internal = CreateCustomKeyStoreRequest.builder()
                    .customKeyStoreName(request.getCustomKeyStoreName())
                    .cloudHsmClusterId(request.getCloudHsmClusterId())
                    .keyStorePassword(request.getKeyStorePassword())
                    .trustAnchorCertificate(request.getTrustAnchorCertificate())
                    .customKeyStoreType(request.getCustomKeyStoreType())
                    .xksProxyUriEndpoint(request.getXksProxyUriEndpoint())
                    .xksProxyUriPath(request.getXksProxyUriPath())
                    .xksProxyAuthenticationCredential(request.getXksProxyAuthenticationCredential())
                    .xksProxyConnectivity(request.getXksProxyConnectivity())
                    .build();
            DescribeCustomKeyStoreResponse.CustomKeyStore internalRes = customKeyStoreService.createCustomKeyStore(tenant, internal);
            CreateCustomKeyStoreResponse response = CreateCustomKeyStoreResponse.builder()
                    .customKeyStoreId(internalRes.getCustomKeyStoreId())
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
            DescribeCustomKeyStoreResponse.CustomKeyStore internal = customKeyStoreService.describeCustomKeyStore(tenant, customKeyStoreId);
            DescribeCustomKeyStoreResponse response = DescribeCustomKeyStoreResponse.builder()
                    .customKeyStore(internal)
                    .build();

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.DELETE_CUSTOM_KEY_STORE,
                    String.valueOf(customKeyStoreId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<UpdateCustomKeyStoreResponse> updateCustomKeyStore(
            @PathVariable("customKeyStoreId") Long customKeyStoreId,
            UpdateCustomKeyStoreRequest request) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            UpdateCustomKeyStoreRequest internal = UpdateCustomKeyStoreRequest.builder()
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
            Long customKeyStoreId) {

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
            Long customKeyStoreId) {

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
            Long customKeyStoreId) {

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
            Integer limit,
            String nextToken
    ) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            ListCustomKeyStoresResponse response = customKeyStoreService.listCustomKeyStores(tenant, limit, nextToken);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.LIST_CUSTOM_KEY_STORE,
                    null,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    // =========================================================================
    // Audit & Utility
    // =========================================================================

    @Override
    public ResponseEntity<AuditLogResponse> getAuditLogs(
            String keyId,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Integer limit) {

        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        keyId = dataKeyService.resolveKeyId(tenant, keyId);
        try {
            AuditLogResponse response = auditService.getAuditLogs(tenant, keyId, fromDate, toDate, limit);
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<KeyUsageStatsResponse> getKeyUsageStats(String keyId) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        keyId = dataKeyService.resolveKeyId(tenant, keyId);
        try {
            KeyUsageStatsResponse response = keyManagementService.getKeyUsageStats(tenant, keyId);
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<GenerateRandomResponse> generateRandom(GenerateRandomRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            GenerateRandomResponse response = dataKeyService.generateRandom(request);
            auditService.logAction(tenant, IKmsActionType.Types.GENERATE_RANDOM, null,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.GENERATE_RANDOM,
                    null,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ValidateKeyResponse> validateKey(String keyId) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        keyId = dataKeyService.resolveKeyId(tenant, keyId);
        try {
            keyManagementService.isValidKey(tenant, keyId);
            ValidateKeyResponse response = ValidateKeyResponse.builder().valid(true).build();

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.VALIDATE_KEY,
                    String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp()
            );

            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }
}