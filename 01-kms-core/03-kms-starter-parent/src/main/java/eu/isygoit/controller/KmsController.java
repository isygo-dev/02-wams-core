package eu.isygoit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.annotation.InjectExceptionHandler;
import eu.isygoit.api.KmsServiceApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.dto.data.TagDto;
import eu.isygoit.enums.IKmsActionType;
import eu.isygoit.exception.handler.KmsExceptionHandler;
import eu.isygoit.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
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
@RequestMapping(path = "/api/v1/private/key")
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
    public ResponseEntity<DescribeKeyResponse> describeKey(DescribeKeyRequest request) {

        log.info("Describing key: {}", request.getKeyId());

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            DescribeKeyResponse response =
                    keyManagementService.describeKey(tenant, request.getKeyId(), request.getGrantTokens());

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.DESCRIBE_KEY,
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
    public ResponseEntity<ListKeysResponse> listKeys(ListKeysRequest request) {

        log.info("Listing keys with limit: {}", request.getLimit());

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            ListKeysResponse response =
                    keyManagementService.listKeys(tenant, request.getLimit(), request.getMarker());

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
    public ResponseEntity<UpdateKeyDescriptionResponse> updateKeyDescription(@Valid UpdateKeyDescriptionRequest request) {

        log.info("Updating key description for key: {}", request.getKeyId());

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            UpdateKeyDescriptionResponse response =
                    keyManagementService.updateKeyDescription(tenant, request.getKeyId(), request);

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.UPDATE_KEY_METADATA,
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
    public ResponseEntity<EnableKeyResponse> enableKey(EnableKeyRequest request) {

        log.info("Enabling key: {}", request.getKeyId());

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            EnableKeyResponse response =
                    keyManagementService.enableKey(tenant, request.getKeyId());

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.ENABLE_KEY,
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
    public ResponseEntity<DisableKeyResponse> disableKey(DisableKeyRequest request) {

        log.info("Disabling key: {}", request.getKeyId());

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            DisableKeyResponse response =
                    keyManagementService.disableKey(tenant, request.getKeyId());

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.DISABLE_KEY,
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
    public ResponseEntity<ScheduleKeyDeletionResponse> scheduleKeyDeletion(ScheduleKeyDeletionRequest request) {

        log.info("Scheduling deletion for key: {} with window: {} days",
                request.getKeyId(),
                request);

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            ScheduleKeyDeletionResponse response =
                    keyManagementService.scheduleKeyDeletion(
                            tenant,
                            request.getKeyId(),
                            request.getPendingWindowInDays()
                    );

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.SCHEDULE_KEY_DELETION,
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
    public ResponseEntity<CancelKeyDeletionResponse> cancelKeyDeletion(CancelKeyDeletionRequest request) {

        log.info("Cancelling deletion for key: {}", request.getKeyId());

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            CancelKeyDeletionResponse response =
                    keyManagementService.cancelKeyDeletion(tenant, request.getKeyId());

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.CANCEL_KEY_DELETION,
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
    public ResponseEntity<DeleteKeyResponse> deleteKey(DeleteKeyRequest request) {

        log.info("Permanently deleting key: {}", request.getKeyId());

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            keyManagementService.deleteKey(tenant, request.getKeyId());

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.DELETE_KEY,
                    request.getKeyId(),
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
    public ResponseEntity<KeyRotationStatusResponseDto> updateKeyRotation(@Valid UpdateKeyRotationRequestDto request) {

        log.info("Updating rotation for key: {}", request.getKeyId());

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            KeyRotationStatusResponseDto response =
                    keyManagementService.updateKeyRotation(
                            tenant,
                            request.getKeyId(),
                            request
                    );

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.UPDATE_KEY_ROTATION,
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
    public ResponseEntity<RotateKeyResponse> rotateKey(RotateKeyRequest request) {

        log.info("Rotating key: {}", request.getKeyId());

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            RotateKeyResponse response =
                    keyManagementService.rotateKey(tenant, request.getKeyId());

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.ROTATE_KEY,
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
    public ResponseEntity<GetKeyRotationStatusResponse> getKeyRotationStatus(GetKeyRotationStatusRequest request) {

        log.info("Getting rotation status for key: {}", request.getKeyId());

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            GetKeyRotationStatusResponse response =
                    keyManagementService.getKeyRotationStatus(tenant, request.getKeyId());

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.GET_KEY_ROTATION_STATUS,
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
    public ResponseEntity<ListKeyRotationsResponseDto> listKeyRotations(ListKeyRotationsRequestDto request) {

        log.info("Listing key rotations for: {}", request.getKeyId());

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            ListKeyRotationsResponseDto response =
                    keyManagementService.listKeyRotations(
                            tenant,
                            request.getKeyId(),
                            request.getLimit(),
                            request.getNextToken()
                    );

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.LIST_KEY_ROTATIONS,
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

    // ============================================================================
    // CRYPTOGRAPHIC OPERATIONS APIs
    // ============================================================================

    @Override
    public ResponseEntity<EncryptResponse> encrypt(
            @Valid EncryptRequest request) {

        log.info("Encrypting with request.getKeyId(): {}", request.getKeyId());

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            EncryptResponse response =
                    encryptionService.encrypt(tenant, request);

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
    public ResponseEntity<DecryptResponse> decrypt(
            @Valid DecryptRequest request) {

        log.info("Decrypting data");

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            DecryptResponse response =
                    encryptionService.decrypt(tenant, request);

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
    public ResponseEntity<ReEncryptResponse> reEncrypt(
            @Valid ReEncryptRequest request) {

        log.info("Re-encrypting to destination key: {}",
                request.getDestinationKeyId());

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            ReEncryptResponse response =
                    encryptionService.reEncrypt(tenant, request);

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
    public ResponseEntity<GenerateDataKeyResponse> generateDataKey(
            @Valid GenerateDataKeyRequest request) {

        log.info("Generating data key with request.getKeyId(): {}", request.getKeyId());

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            GenerateDataKeyResponse response =
                    dataKeyService.generateDataKey(tenant, request);

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
            @Valid GenerateDataKeyWithoutPlaintextRequest request) {

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
            @Valid GenerateDataKeyPairRequest request) {

        log.info("Generating data key pair");

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            GenerateDataKeyPairResponse response =
                    dataKeyService.generateDataKeyPair(tenant, request);

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
            @Valid GenerateDataKeyPairWithoutPlaintextRequest request) {

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
    public ResponseEntity<SignResponse> sign(
            @Valid SignRequest request) {

        log.info("Signing with request.getKeyId(): {}", request.getKeyId());

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            SignResponse response =
                    signingService.sign(tenant, request);

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
    public ResponseEntity<VerifyResponse> verify(@Valid VerifyRequest request) {
        log.info("Verifying signature with request.getKeyId(): {}", request.getKeyId());
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
    public ResponseEntity<GenerateMacResponse> generateMac(@Valid GenerateMacRequest request) {
        log.info("Generating MAC with request.getKeyId(): {}", request.getKeyId());
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
    public ResponseEntity<VerifyMacResponse> verifyMac(@Valid VerifyMacRequest request) {
        log.info("Verifying MAC with request.getKeyId(): {}", request.getKeyId());
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
    public ResponseEntity<GetPublicKeyResponse> getPublicKey(GetPublicKeyRequest request) {
        log.info("Getting public key for: {}", request.getKeyId());
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            GetPublicKeyResponse response = keyManagementService.getPublicKey(tenant, request.getKeyId());
            auditService.logAction(tenant, IKmsActionType.Types.GET_PUBLIC_KEY, request.getKeyId(),
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
    public ResponseEntity<ListKeyVersionsResponse> listKeyVersions(ListKeyVersionsRequest request) {

        log.info("Listing key versions for: {}", request.getKeyId());

        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();

            ListKeyVersionsResponse response =
                    keyVersionService.listKeyVersions(tenant, request.getKeyId(), request.getLimit(), request.getMarker());

            auditService.logAction(
                    tenant,
                    IKmsActionType.Types.LIST_KEY_VERSIONS,
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
    public ResponseEntity<ActiveVersionResponseDto> getActiveVersion(ActiveVersionRequestDto request) {
        log.info("Getting active version for: {}", request.getKeyId());
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            ActiveVersionResponseDto response = keyVersionService.getActiveVersion(tenant, request.getKeyId());
            auditService.logAction(tenant, IKmsActionType.Types.GET_ACTIVE_VERSION, request.getKeyId(),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<UpdatePrimaryRegionResponse> updatePrimaryRegion(UpdatePrimaryRegionRequestDto request) {
        log.info("Updating primary region for key: {} to region: {}", request.getKeyId(), request.getPrimaryRegion());
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            UpdatePrimaryRegionResponse response = multiRegionService.updatePrimaryRegion(tenant, request.getKeyId(), request);
            auditService.logAction(tenant, IKmsActionType.Types.UPDATE_PRIMARY_REGION, request.getKeyId(),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ReplicateKeyResponse> replicateKey(ReplicateKeyRequestDto request) {
        log.info("Replicating key: {} to region: {}", request.getKeyId(), request.getReplicaRegion());
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            ReplicateKeyResponse response = multiRegionService.replicateKey(tenant, request.getKeyId(), request);
            auditService.logAction(tenant, IKmsActionType.Types.REPLICATE_KEY, request.getKeyId(),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<SynchronizeMultiRegionKeyResponse> synchronizeMultiRegionKey(SynchronizeMultiRegionKeyRequest request) {
        log.info("Synchronizing multi-region key: {}", request.getKeyId());
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            SynchronizeMultiRegionKeyResponse response = multiRegionService.synchronizeMultiRegionKey(tenant, request.getKeyId());
            auditService.logAction(tenant, IKmsActionType.Types.SYNCHRONIZE_MULTI_REGION_KEY, request.getKeyId(),
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
            CreateAliasRequestDto internalReq = CreateAliasRequestDto.builder()
                    .aliasName(request.getAliasName())
                    .targetKeyId(request.getTargetKeyId())
                    .build();
            keyManagementService.createAlias(tenant, internalReq);
            auditService.logAction(tenant, IKmsActionType.Types.CREATE_ALIAS, request.getTargetKeyId(),
                    requestContextService.getCurrentContext().getSenderUser(), requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(new CreateAliasResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<UpdateAliasResponse> updateAlias(UpdateAliasRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            UpdateAliasRequestDto internalReq = UpdateAliasRequestDto.builder()
                    .targetKeyId(request.getTargetKeyId())
                    .build();
            keyManagementService.updateAlias(tenant, request.getAliasName(), internalReq);
            auditService.logAction(tenant, IKmsActionType.Types.UPDATE_ALIAS, request.getAliasName(),
                    requestContextService.getCurrentContext().getSenderUser(), requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(new UpdateAliasResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<DeleteAliasResponse> deleteAlias(DeleteAliasRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            keyManagementService.deleteAlias(tenant, request.getAliasName());
            auditService.logAction(tenant, IKmsActionType.Types.DELETE_ALIAS, request.getAliasName(), requestContextService.getCurrentContext().getSenderUser(), requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(new DeleteAliasResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    private String buildAliasArn(String aliasName) {
        // You need to get the current AWS account ID and region dynamically.
        // For now, use placeholders or fetch from configuration.
        String accountId = "123456789012"; // replace with real account ID
        String region = "us-east-1";       // replace with actual region
        return String.format("arn:aws:kms:%s:%s:alias/%s", region, accountId, aliasName);
    }

    private String formatDate(LocalDateTime date) {
        return date != null ? date.format(DateTimeFormatter.ISO_DATE_TIME) : null;
    }

    @Override
    public ResponseEntity<ListAliasesResponse> listAliases(ListAliasesRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            ListAliasesResponseDto internal = keyManagementService.listAliases(tenant, request.getLimit(), request.getMarker());

            ListAliasesResponse response = ListAliasesResponse.builder()
                    .aliases(internal.getAliases().stream()
                            .map(a -> ListAliasesResponse.AliasEntry.builder()
                                    .aliasName(a.getAliasName())
                                    .aliasArn(buildAliasArn(a.getAliasName()))   // compute ARN
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
    public ResponseEntity<ListAliasesResponse> listAliasesForKey(ListAliasesForKeyRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            ListAliasesResponseDto internal = keyManagementService.listAliasesForKey(tenant, request.getKeyId(), request.getLimit(), request.getMarker());

            ListAliasesResponse response = ListAliasesResponse.builder()
                    .aliases(internal.getAliases().stream()
                            .map(a -> ListAliasesResponse.AliasEntry.builder()
                                    .aliasName(a.getAliasName())
                                    .aliasArn(buildAliasArn(a.getAliasName()))   // compute ARN
                                    .targetKeyId(a.getTargetKeyId())
                                    .creationDate(formatDate(a.getCreatedAt()))
                                    .lastUpdatedDate(formatDate(a.getUpdatedAt()))
                                    .build())
                            .collect(Collectors.toList()))
                    .build();
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

// =========================================================================
// Key Policies
// =========================================================================

    @Override
    public ResponseEntity<PutKeyPolicyResponse> putKeyPolicy(PutKeyPolicyRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            SetKeyPolicyRequestDto internal = SetKeyPolicyRequestDto.builder()
                    .policy(request.getPolicy())
                    .policyName(request.getPolicyName())
                    .bypassPolicyLockoutSafetyCheck(request.getBypassPolicyLockoutSafetyCheck())
                    .build();
            keyPolicyService.setKeyPolicy(tenant, request.getKeyId(), internal);
            auditService.logAction(tenant, IKmsActionType.Types.SET_KEY_POLICY, request.getKeyId(), requestContextService.getCurrentContext().getSenderUser(), requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(new PutKeyPolicyResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<GetKeyPolicyResponse> getKeyPolicy(GetKeyPolicyRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            Map<String, Object> policyMap = keyPolicyService.getKeyPolicy(tenant, request.getKeyId());
            String policyJson = new ObjectMapper().writeValueAsString(policyMap);
            GetKeyPolicyResponse response = GetKeyPolicyResponse.builder().policy(policyJson).build();
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

// =========================================================================
// Grants
// =========================================================================

    @Override
    public ResponseEntity<CreateGrantResponse> createGrant(CreateGrantRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            // Map AWS request to internal DTO
            CreateGrantRequestDto internal = CreateGrantRequestDto.builder()
                    .principal(request.getGranteePrincipal())          // map granteePrincipal to principal
                    .granteePrincipal(request.getGranteePrincipal())  // also set granteePrincipal
                    .operations(request.getOperations())
                    .build();

            GrantResponseDto internalRes = keyPolicyService.createGrant(tenant, request.getKeyId(), internal);

            auditService.logAction(tenant, IKmsActionType.Types.CREATE_GRANT, request.getKeyId(),
                    requestContextService.getCurrentContext().getSenderUser(), requestContextService.getCurrentContext().getClientIp());

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
    public ResponseEntity<ListGrantsResponse> listGrants(ListGrantsRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            ListGrantsResponseDto internal = keyPolicyService.listGrants(tenant, request.getKeyId(), request.getLimit(), request.getMarker());

            ListGrantsResponse response = ListGrantsResponse.builder()
                    .grants(internal.getGrants().stream()
                            .map(g -> ListGrantsResponse.Grant.builder()
                                    .grantId(g.getGrantId())
                                    .granteePrincipal(g.getGranteePrincipal())
                                    .retiringPrincipal(g.getRetiringPrincipal())
                                    .operations(g.getOperations())
                                    .constraints(null) // No structured constraints from internal DTO
                                    .creationDate(formatDate(g.getCreatedAt()))
                                    .lastUpdatedDate(formatDate(g.getCreatedAt())) // same as creation
                                    .keyId(request.getKeyId()) // from path
                                    .name(null) // not available
                                    .build())
                            .collect(Collectors.toList()))
                    .nextMarker(internal.getNextToken()) // note: internal uses nextToken, not nextMarker
                    .truncated(internal.getGrants().size() < (request.getLimit() != null ? request.getLimit() : Integer.MAX_VALUE)) // approximate
                    .build();
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<RevokeGrantResponse> revokeGrant(RevokeGrantRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            keyPolicyService.revokeGrant(tenant, request.getKeyId(), request.getGrantId());
            auditService.logAction(tenant, IKmsActionType.Types.REVOKE_GRANT, request.getKeyId(), requestContextService.getCurrentContext().getSenderUser(), requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(new RevokeGrantResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<RetireGrantResponse> retireGrant(RetireGrantRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            RetireGrantRequestDto internal = RetireGrantRequestDto.builder()
                    .grantToken(request != null ? request.getGrantToken() : null)
                    .build();
            keyPolicyService.retireGrant(tenant, request.getGrantId(), internal);
            auditService.logAction(tenant, IKmsActionType.Types.RETIRE_GRANT, request.getGrantId(), requestContextService.getCurrentContext().getSenderUser(), requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(new RetireGrantResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<EnableKeyRotationResponse> enableKeyRotation(EnableKeyRotationRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            // Create internal request with enabled = true, default period 365
            UpdateKeyRotationRequestDto internalRequest = UpdateKeyRotationRequestDto.builder()
                    .enableRotation(true)
                    .rotationPeriodDays(365)
                    .applyImmediately(true)
                    .build();
            keyManagementService.updateKeyRotation(tenant, request.getKeyId(), internalRequest);
            auditService.logAction(tenant, IKmsActionType.Types.UPDATE_KEY_ROTATION, request.getKeyId(),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            // AWS returns an empty response
            return ResponseFactory.responseOk(new EnableKeyRotationResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<DisableKeyRotationResponse> disableKeyRotation(DisableKeyRotationRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            UpdateKeyRotationRequestDto internalRequest = UpdateKeyRotationRequestDto.builder()
                    .enableRotation(false)
                    .applyImmediately(true)
                    .build();
            keyManagementService.updateKeyRotation(tenant, request.getKeyId(), internalRequest);
            auditService.logAction(tenant, IKmsActionType.Types.UPDATE_KEY_ROTATION, request.getKeyId(),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(new DisableKeyRotationResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ListKeyPoliciesResponse> listKeyPolicies(ListKeyPoliciesRequest request) {
        // AWS KMS typically returns only the default policy name.
        // Our internal service doesn't have a dedicated method, so we return a minimal response.
        ListKeyPoliciesResponse response = ListKeyPoliciesResponse.builder()
                .policyNames(List.of("default"))
                .nextMarker(null)
                .truncated(false)
                .build();
        return ResponseFactory.responseOk(response);
    }

    @Override
    public ResponseEntity<ListRetirableGrantsResponse> listRetirableGrants(ListRetirableGrantsRequest request) {
        // Our internal IKeyPolicyService does not have a method to list retirable grants.
        // Return an empty list as a safe default.
        ListRetirableGrantsResponse response = ListRetirableGrantsResponse.builder()
                .grants(List.of())
                .nextMarker(null)
                .truncated(false)
                .build();
        return ResponseFactory.responseOk(response);
    }

// =========================================================================
// Tags
// =========================================================================

    @Override
    public ResponseEntity<TagResourceResponse> tagResource(TagResourceRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            TagResourceRequestDto internal = TagResourceRequestDto.builder()
                    .tags(request.getTags().stream()
                            .map(t -> new TagDto(t.getTagKey(), t.getTagValue()))
                            .collect(Collectors.toMap(TagDto::getTagKey, TagDto::getTagValue)))
                    .build();
            keyManagementService.tagResource(tenant, request.getKeyId(), internal);
            auditService.logAction(tenant, IKmsActionType.Types.TAG_RESOURCE, request.getKeyId(), requestContextService.getCurrentContext().getSenderUser(), requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(new TagResourceResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<UntagResourceResponse> untagResource(UntagResourceRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            UntagResourceRequestDto internal = UntagResourceRequestDto.builder()
                    .tagKeys(request.getTagKeys())
                    .build();
            keyManagementService.untagResource(tenant, request.getKeyId(), internal);
            auditService.logAction(tenant, IKmsActionType.Types.UNTAG_RESOURCE, request.getKeyId(), requestContextService.getCurrentContext().getSenderUser(), requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(new UntagResourceResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ListResourceTagsResponse> listResourceTags(ListResourceTagsRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            ListTagsResponseDto internal = keyManagementService.listResourceTags(tenant, request.getKeyId());
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
    public ResponseEntity<GetParametersForImportResponse> getParametersForImport(GetParametersForImportRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            ImportParametersResponseDto internal = keyManagementService.getParametersForImport(tenant, request.getKeyId());
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
    public ResponseEntity<ImportKeyMaterialResponse> importKeyMaterial(ImportKeyMaterialRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            ImportKeyMaterialRequestDto internal = ImportKeyMaterialRequestDto.builder()
                    .importToken(request.getImportToken().getBytes())
                    .encryptedKeyMaterial(request.getEncryptedKeyMaterial().getBytes())
                    .validTo(request.getValidTo())
                    .expirationModel(request.getExpirationModel())
                    .build();
            keyManagementService.importKeyMaterial(tenant, request.getKeyId(), internal);
            auditService.logAction(tenant, IKmsActionType.Types.IMPORT_KEY_MATERIAL, request.getKeyId(), requestContextService.getCurrentContext().getSenderUser(), requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(new ImportKeyMaterialResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<DeleteImportedKeyMaterialResponse> deleteImportedKeyMaterial(DeleteImportedKeyMaterialRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            keyManagementService.deleteImportedKeyMaterial(tenant, request.getKeyId());
            auditService.logAction(tenant, IKmsActionType.Types.DELETE_IMPORTED_KEY_MATERIAL, request.getKeyId(), requestContextService.getCurrentContext().getSenderUser(), requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(new DeleteImportedKeyMaterialResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

// =========================================================================
// Custom Key Stores
// =========================================================================

    @Override
    public ResponseEntity<CreateCustomKeyStoreResponse> createCustomKeyStore(CreateCustomKeyStoreRequest request) {
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
                        .xksProxyAuthenticationCredential(null) // do not expose credential in response
                        .xksProxyConnectivity(null)
                        .build();

        return DescribeCustomKeyStoreResponse.builder().customKeyStore(store).build();
    }

    @Override
    public ResponseEntity<DescribeCustomKeyStoreResponse> describeCustomKeyStore(DescribeCustomKeyStoreRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            CustomKeyStoreResponseDto internal = customKeyStoreService.describeCustomKeyStore(tenant, request.getCustomKeyStoreId());
            DescribeCustomKeyStoreResponse response = toAwsResponse(internal);
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<UpdateCustomKeyStoreResponse> updateCustomKeyStore(UpdateCustomKeyStoreRequest request) {
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
            customKeyStoreService.updateCustomKeyStore(tenant, request.getCustomKeyStoreId(), internal);
            return ResponseFactory.responseOk(new UpdateCustomKeyStoreResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<DeleteCustomKeyStoreResponse> deleteCustomKeyStore(DeleteCustomKeyStoreRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            customKeyStoreService.deleteCustomKeyStore(tenant, request.getCustomKeyStoreId());
            return ResponseFactory.responseOk(new DeleteCustomKeyStoreResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ConnectCustomKeyStoreResponse> connectCustomKeyStore(ConnectCustomKeyStoreRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            customKeyStoreService.connectCustomKeyStore(tenant, request.getCustomKeyStoreId());
            return ResponseFactory.responseOk(new ConnectCustomKeyStoreResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<DisconnectCustomKeyStoreResponse> disconnectCustomKeyStore(DisconnectCustomKeyStoreRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            customKeyStoreService.disconnectCustomKeyStore(tenant, request.getCustomKeyStoreId());
            return ResponseFactory.responseOk(new DisconnectCustomKeyStoreResponse());
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ListCustomKeyStoresResponse> listCustomKeyStores(ListCustomKeyStoresRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            ListCustomKeyStoresResponseDto internal = customKeyStoreService.listCustomKeyStores(tenant, request.getLimit(), request.getMarker());
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

// =========================================================================
// Audit & Utility
// =========================================================================

    @Override
    public ResponseEntity<AuditLogResponse> getAuditLogs(AuditLogRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            LocalDateTime from = request.getFromDate() != null ? LocalDateTime.parse(request.getFromDate(), DateTimeFormatter.ISO_DATE_TIME) : null;
            LocalDateTime to = request.getToDate() != null ? LocalDateTime.parse(request.getToDate(), DateTimeFormatter.ISO_DATE_TIME) : null;
            AuditLogResponseDto internal = auditService.getAuditLogs(tenant, request.getKeyId(), from, to, request.getLimit());
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
    public ResponseEntity<KeyUsageStatsResponse> getKeyUsageStats(KeyUsageStatsRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            KeyUsageStatsResponseDto internal = keyManagementService.getKeyUsageStats(tenant, request.getKeyId());
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
    public ResponseEntity<GenerateRandomResponse> generateRandom(GenerateRandomRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            byte[] randomBytes = new byte[request.getNumberOfBytes()];
            new SecureRandom().nextBytes(randomBytes);
            String plaintext = Base64.getEncoder().encodeToString(randomBytes);
            GenerateRandomResponse response = GenerateRandomResponse.builder().plaintext(plaintext).build();
            auditService.logAction(tenant, IKmsActionType.Types.GENERATE_RANDOM_DATA, null, requestContextService.getCurrentContext().getSenderUser(), requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ValidateKeyResponse> validateKey(ValidateKeyRequest request) {
        String tenant = requestContextService.getCurrentContext().getSenderTenant();
        try {
            keyManagementService.validateKey(tenant, request.getKeyId());
            ValidateKeyResponse response = ValidateKeyResponse.builder().valid(true).build();
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            return getBackExceptionResponse(e);
        }
    }
}