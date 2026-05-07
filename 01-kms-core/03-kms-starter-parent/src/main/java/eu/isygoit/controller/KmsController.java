package eu.isygoit.controller;

import eu.isygoit.annotation.InjectExceptionHandler;
import eu.isygoit.api.KmsServiceApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.constants.CtrlConstants;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.dto.request.*;
import eu.isygoit.dto.response.*;
import eu.isygoit.dto.response.DataKeyPairResponseDto;
import eu.isygoit.dto.response.GenerateMacResponseDto;
import eu.isygoit.dto.response.ImportParametersResponseDto;
import eu.isygoit.dto.response.KeyRotationStatusDto;
import eu.isygoit.dto.response.ListGrantsResponseDto;
import eu.isygoit.dto.response.ReEncryptResponseDto;
import eu.isygoit.enums.IEnumCharSet;
import eu.isygoit.enums.IKmsActionType;
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
@RequestMapping(path = "/api/v1/private/key")
@Tag(name = "KMS Keys", description = "Key Management Service - All cryptographic operations and key management endpoints")
public class KmsController extends ControllerExceptionHandler implements KmsServiceApi {

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
    public ResponseEntity<CreateKeyResponseDto> createKey(@Valid @RequestBody CreateKeyRequestDto request) {
        log.info("Creating key with spec: {} and purpose: {}", request.getKeySpec(), request.getPurpose());
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            CreateKeyResponseDto response = keyManagementService.createKey(tenant, request);
            auditService.logAction(tenant, IKmsActionType.Types.CREATE_KEY, String.valueOf(response.getKeyId()),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseCreated(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<KeyMetadataResponseDto> describeKey(@PathVariable Long keyId) {
        log.info("Describing key: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            KeyMetadataResponseDto response = keyManagementService.getKeyMetadata(tenant, keyId);
            auditService.logAction(tenant, IKmsActionType.Types.DESCRIBE_KEY, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ListKeysResponseDto> listKeys(@RequestParam(required = false) Integer limit,
                                                        @RequestParam(required = false) String nextToken) {
        log.info("Listing keys with limit: {}", limit);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            ListKeysResponseDto response = keyManagementService.listKeys(tenant, limit, nextToken);
            auditService.logAction(tenant, IKmsActionType.Types.LIST_KEYS, null,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<KeyMetadataResponseDto> updateKeyMetadata(@PathVariable Long keyId,
                                                                    @Valid @RequestBody UpdateKeyMetadataRequestDto request) {
        log.info("Updating metadata for key: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            KeyMetadataResponseDto response = keyManagementService.updateKeyMetadata(tenant, keyId, request);
            auditService.logAction(tenant, IKmsActionType.Types.UPDATE_KEY_METADATA, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<KeyMetadataResponseDto> enableKey(@PathVariable Long keyId) {
        log.info("Enabling key: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            KeyMetadataResponseDto response = keyManagementService.enableKey(tenant, keyId);
            auditService.logAction(tenant, IKmsActionType.Types.ENABLE_KEY, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<KeyMetadataResponseDto> disableKey(@PathVariable Long keyId) {
        log.info("Disabling key: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            KeyMetadataResponseDto response = keyManagementService.disableKey(tenant, keyId);
            auditService.logAction(tenant, IKmsActionType.Types.DISABLE_KEY, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<KeyMetadataResponseDto> scheduleKeyDeletion(@PathVariable Long keyId,
                                                                      @RequestParam(required = false) Integer pendingWindowInDays) {
        log.info("Scheduling deletion for key: {} with window: {} days", keyId, pendingWindowInDays);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            KeyMetadataResponseDto response = keyManagementService.scheduleKeyDeletion(tenant, keyId, pendingWindowInDays);
            auditService.logAction(tenant, IKmsActionType.Types.SCHEDULE_KEY_DELETION, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<KeyMetadataResponseDto> cancelKeyDeletion(@PathVariable Long keyId) {
        log.info("Cancelling deletion for key: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            KeyMetadataResponseDto response = keyManagementService.cancelKeyDeletion(tenant, keyId);
            auditService.logAction(tenant, IKmsActionType.Types.CANCEL_KEY_DELETION, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<?> deleteKey(@PathVariable Long keyId) {
        log.info("Permanently deleting key: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            keyManagementService.deleteKey(tenant, keyId);
            auditService.logAction(tenant, IKmsActionType.Types.DELETE_KEY, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseNoContent();
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    // ============================================================================
    // KEY ROTATION APIs
    // ============================================================================

    @Override
    public ResponseEntity<KeyRotationStatusDto> updateKeyRotation(@PathVariable Long keyId,
                                                                  @Valid @RequestBody UpdateKeyRotationRequestDto request) {
        log.info("Updating rotation for key: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            KeyRotationStatusDto response = keyManagementService.updateKeyRotation(tenant, keyId, request);
            auditService.logAction(tenant, IKmsActionType.Types.UPDATE_KEY_ROTATION, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<RotateKeyResponseDto> rotateKey(@PathVariable Long keyId) {
        log.info("Rotating key: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            RotateKeyResponseDto response = keyManagementService.rotateKey(tenant, keyId);
            auditService.logAction(tenant, IKmsActionType.Types.ROTATE_KEY, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<KeyRotationStatusDto> getKeyRotationStatus(@PathVariable Long keyId) {
        log.info("Getting rotation status for key: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            KeyRotationStatusDto response = keyManagementService.getKeyRotationStatus(tenant, keyId);
            auditService.logAction(tenant, IKmsActionType.Types.GET_KEY_ROTATION_STATUS, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ListKeyRotationsResponseDto> listKeyRotations(@PathVariable Long keyId,
                                                                        @RequestParam(required = false) Integer limit,
                                                                        @RequestParam(required = false) String nextToken) {
        log.info("Listing key rotations for: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            ListKeyRotationsResponseDto response = keyManagementService.listKeyRotations(tenant, keyId, limit, nextToken);
            auditService.logAction(tenant, IKmsActionType.Types.LIST_KEY_ROTATIONS, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
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
    public ResponseEntity<EncryptResponseDto> encrypt(@Valid @RequestBody EncryptRequestDto request) {
        log.info("Encrypting with keyId: {}", request.getKeyId());
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            EncryptResponseDto response = encryptionService.encrypt(tenant, request);
            auditService.logAction(tenant, IKmsActionType.Types.ENCRYPT, String.valueOf(request.getKeyId()),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
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
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            DecryptResponseDto response = encryptionService.decrypt(tenant, request);
            auditService.logAction(tenant, IKmsActionType.Types.DECRYPT, String.valueOf(response.getKeyId()),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ReEncryptResponseDto> reEncrypt(@Valid @RequestBody ReEncryptRequestDto request) {
        log.info("Re-encrypting to destination key: {}", request.getDestinationKeyId());
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            ReEncryptResponseDto response = encryptionService.reencrypt(tenant, request);
            auditService.logAction(tenant, IKmsActionType.Types.REENCRYPT, String.valueOf(request.getDestinationKeyId()),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<DataKeyResponseDto> generateDataKey(@Valid @RequestBody GenerateDataKeyRequestDto request) {
        log.info("Generating data key with keyId: {}", request.getKeyId());
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            DataKeyResponseDto response = dataKeyService.generateDataKey(tenant, request);
            auditService.logAction(tenant, IKmsActionType.Types.GENERATE_DATA_KEY, String.valueOf(request.getKeyId()),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<DataKeyResponseDto> generateDataKeyWithoutPlaintext(@Valid @RequestBody GenerateDataKeyRequestDto request) {
        log.info("Generating data key without plaintext");
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            DataKeyResponseDto response = dataKeyService.generateDataKeyWithoutPlaintext(tenant, request);
            auditService.logAction(tenant, IKmsActionType.Types.GENERATE_DATA_KEY_WITHOUT_PLAINTEXT, String.valueOf(request.getKeyId()),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<DataKeyPairResponseDto> generateDataKeyPair(@Valid @RequestBody GenerateDataKeyPairRequestDto request) {
        log.info("Generating data key pair");
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            DataKeyPairResponseDto response = dataKeyService.generateDataKeyPair(tenant, request);
            auditService.logAction(tenant, IKmsActionType.Types.GENERATE_DATA_KEY_PAIR, String.valueOf(request.getKeyId()),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<DataKeyPairResponseDto> generateDataKeyPairWithoutPlaintext(@Valid @RequestBody GenerateDataKeyPairRequestDto request) {
        log.info("Generating data key pair without plaintext");
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            DataKeyPairResponseDto response = dataKeyService.generateDataKeyPairWithoutPlaintext(tenant, request);
            auditService.logAction(tenant, IKmsActionType.Types.GENERATE_DATA_KEY_PAIR_WITHOUT_PLAINTEXT, String.valueOf(request.getKeyId()),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<SignResponseDto> sign(@Valid @RequestBody SignRequestDto request) {
        log.info("Signing with keyId: {}", request.getKeyId());
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            SignResponseDto response = signingService.sign(tenant, request);
            auditService.logAction(tenant, IKmsActionType.Types.SIGN, String.valueOf(request.getKeyId()),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
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
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            VerifyResponseDto response = signingService.verify(tenant, request);
            auditService.logAction(tenant, IKmsActionType.Types.VERIFY, String.valueOf(request.getKeyId()),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<GenerateMacResponseDto> generateMac(@Valid @RequestBody GenerateMacRequestDto request) {
        log.info("Generating MAC with keyId: {}", request.getKeyId());
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            GenerateMacResponseDto response = signingService.generateMac(tenant, request);
            auditService.logAction(tenant, IKmsActionType.Types.GENERATE_MAC, String.valueOf(request.getKeyId()),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<VerifyMacResponseDto> verifyMac(@Valid @RequestBody VerifyMacRequestDto request) {
        log.info("Verifying MAC with keyId: {}", request.getKeyId());
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            VerifyMacResponseDto response = signingService.verifyMac(tenant, request);
            auditService.logAction(tenant, IKmsActionType.Types.VERIFY_MAC, String.valueOf(request.getKeyId()),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<PublicKeyResponseDto> getPublicKey(@PathVariable Long keyId) {
        log.info("Getting public key for: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            PublicKeyResponseDto response = keyManagementService.getPublicKey(tenant, keyId);
            auditService.logAction(tenant, IKmsActionType.Types.GET_PUBLIC_KEY, String.valueOf(keyId),
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
    public ResponseEntity<KeyVersionListResponseDto> listKeyVersions(@PathVariable Long keyId) {
        log.info("Listing key versions for: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            KeyVersionListResponseDto response = keyVersionService.listKeyVersions(tenant, keyId);
            auditService.logAction(tenant, IKmsActionType.Types.LIST_KEY_VERSIONS, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ActiveVersionResponseDto> getActiveVersion(@PathVariable Long keyId) {
        log.info("Getting active version for: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            ActiveVersionResponseDto response = keyVersionService.getActiveVersion(tenant, keyId);
            auditService.logAction(tenant, IKmsActionType.Types.GET_ACTIVE_VERSION, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<KeyMetadataResponseDto> updatePrimaryRegion(@PathVariable Long keyId,
                                                                      @Valid @RequestBody UpdatePrimaryRegionRequestDto request) {
        log.info("Updating primary region for key: {} to region: {}", keyId, request.getPrimaryRegion());
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            KeyMetadataResponseDto response = multiRegionService.updatePrimaryRegion(tenant, keyId, request);
            auditService.logAction(tenant, IKmsActionType.Types.UPDATE_PRIMARY_REGION, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ReplicateKeyResponseDto> replicateKey(@PathVariable Long keyId,
                                                                @Valid @RequestBody ReplicateKeyRequestDto request) {
        log.info("Replicating key: {} to region: {}", keyId, request.getReplicaRegion());
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            ReplicateKeyResponseDto response = multiRegionService.replicateKey(tenant, keyId, request);
            auditService.logAction(tenant, IKmsActionType.Types.REPLICATE_KEY, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<KeyMetadataResponseDto> synchronizeMultiRegionKey(@PathVariable Long keyId) {
        log.info("Synchronizing multi-region key: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            KeyMetadataResponseDto response = multiRegionService.synchronizeMultiRegionKey(tenant, keyId);
            auditService.logAction(tenant, IKmsActionType.Types.SYNCHRONIZE_MULTI_REGION_KEY, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    // ============================================================================
    // ALIAS MANAGEMENT APIs
    // ============================================================================

    @Override
    public ResponseEntity<AliasResponseDto> createAlias(@Valid @RequestBody CreateAliasRequestDto request) {
        log.info("Creating alias: {}", request.getAliasName());
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            AliasResponseDto response = keyManagementService.createAlias(tenant, request);
            auditService.logAction(tenant, IKmsActionType.Types.CREATE_ALIAS, String.valueOf(request.getTargetKeyId()),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseCreated(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<AliasResponseDto> updateAlias(@PathVariable String aliasName,
                                                        @Valid @RequestBody UpdateAliasRequestDto request) {
        log.info("Updating alias: {}", aliasName);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            AliasResponseDto response = keyManagementService.updateAlias(tenant, aliasName, request);
            auditService.logAction(tenant, IKmsActionType.Types.UPDATE_ALIAS, aliasName,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<?> deleteAlias(@PathVariable String aliasName) {
        log.info("Deleting alias: {}", aliasName);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            keyManagementService.deleteAlias(tenant, aliasName);
            auditService.logAction(tenant, IKmsActionType.Types.DELETE_ALIAS, aliasName,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseNoContent();
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ListAliasesResponseDto> listAliases(@RequestParam(required = false) Integer limit,
                                                              @RequestParam(required = false) String nextToken) {
        log.info("Listing aliases with limit: {}", limit);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            ListAliasesResponseDto response = keyManagementService.listAliases(tenant, limit, nextToken);
            auditService.logAction(tenant, IKmsActionType.Types.LIST_ALIASES, "-",
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ListAliasesResponseDto> listAliasesForKey(@PathVariable Long keyId) {
        log.info("Listing aliases for key: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            ListAliasesResponseDto response = keyManagementService.listAliasesForKey(tenant, keyId);
            auditService.logAction(tenant, IKmsActionType.Types.LIST_ALIASES_FOR_KEY, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    // ============================================================================
    // KEY POLICY & GRANTS APIs
    // ============================================================================

    @Override
    public ResponseEntity<?> setKeyPolicy(@PathVariable Long keyId,
                                          @Valid @RequestBody SetKeyPolicyRequestDto request) {
        log.info("Setting policy for key: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            Object response = keyPolicyService.setKeyPolicy(tenant, keyId, request);
            auditService.logAction(tenant, IKmsActionType.Types.SET_KEY_POLICY, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<?> getKeyPolicy(@PathVariable Long keyId) {
        log.info("Getting policy for key: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            Object response = keyPolicyService.getKeyPolicy(tenant, keyId);
            auditService.logAction(tenant, IKmsActionType.Types.GET_KEY_POLICY, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<GrantResponseDto> createGrant(@PathVariable Long keyId,
                                                        @Valid @RequestBody CreateGrantRequestDto request) {
        log.info("Creating grant for key: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            GrantResponseDto response = keyPolicyService.createGrant(tenant, keyId, request);
            auditService.logAction(tenant, IKmsActionType.Types.CREATE_GRANT, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ListGrantsResponseDto> listGrants(@PathVariable Long keyId,
                                                            @RequestParam(required = false) Integer limit,
                                                            @RequestParam(required = false) String nextToken) {
        log.info("Listing grants for key: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            ListGrantsResponseDto response = keyPolicyService.listGrants(tenant, keyId, limit, nextToken);
            auditService.logAction(tenant, IKmsActionType.Types.LIST_GRANTS, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<?> revokeGrant(@PathVariable Long keyId,
                                         @PathVariable String grantId) {
        log.info("Revoking grant {} for key: {}", grantId, keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            keyPolicyService.revokeGrant(tenant, keyId, grantId);
            auditService.logAction(tenant, IKmsActionType.Types.REVOKE_GRANT, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseNoContent();
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<?> retireGrant(@PathVariable String grantId,
                                         @Valid @RequestBody RetireGrantRequestDto request) {
        log.info("Retiring grant: {}", grantId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            keyPolicyService.retireGrant(tenant, grantId, request);
            auditService.logAction(tenant, IKmsActionType.Types.RETIRE_GRANT, grantId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseNoContent();
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    // ============================================================================
    // TAGGING APIs
    // ============================================================================

    @Override
    public ResponseEntity<?> tagResource(@PathVariable Long keyId,
                                         @Valid @RequestBody TagResourceRequestDto request) {
        log.info("Tagging resource: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            keyManagementService.tagResource(tenant, keyId, request);
            auditService.logAction(tenant, IKmsActionType.Types.TAG_RESOURCE, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk("Tags added successfully");
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<?> untagResource(@PathVariable Long keyId,
                                           @Valid @RequestBody UntagResourceRequestDto request) {
        log.info("Untagging resource: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            keyManagementService.untagResource(tenant, keyId, request);
            auditService.logAction(tenant, IKmsActionType.Types.UNTAG_RESOURCE, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk("Tags removed successfully");
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ListTagsResponseDto> listResourceTags(@PathVariable Long keyId) {
        log.info("Listing tags for key: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            ListTagsResponseDto response = keyManagementService.listResourceTags(tenant, keyId);
            auditService.logAction(tenant, IKmsActionType.Types.LIST_RESOURCE_TAGS, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    // ============================================================================
    // KEY MATERIAL IMPORT APIs (BYOK)
    // ============================================================================

    @Override
    public ResponseEntity<ImportParametersResponseDto> getParametersForImport(@PathVariable Long keyId) {
        log.info("Getting import parameters for key: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            ImportParametersResponseDto response = keyManagementService.getParametersForImport(tenant, keyId);
            auditService.logAction(tenant, IKmsActionType.Types.GET_PARAMETERS_FOR_IMPORT, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<KeyMetadataResponseDto> importKeyMaterial(@PathVariable Long keyId,
                                                                    @Valid @RequestBody ImportKeyMaterialRequestDto request) {
        log.info("Importing key material for key: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            KeyMetadataResponseDto response = keyManagementService.importKeyMaterial(tenant, keyId, request);
            auditService.logAction(tenant, IKmsActionType.Types.IMPORT_KEY_MATERIAL, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<KeyMetadataResponseDto> deleteImportedKeyMaterial(@PathVariable Long keyId) {
        log.info("Deleting imported key material for key: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            KeyMetadataResponseDto response = keyManagementService.deleteImportedKeyMaterial(tenant, keyId);
            auditService.logAction(tenant, IKmsActionType.Types.DELETE_IMPORTED_KEY_MATERIAL, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    // ============================================================================
    // CUSTOM KEY STORE APIs
    // ============================================================================

    @Override
    public ResponseEntity<CustomKeyStoreResponseDto> createCustomKeyStore(@Valid @RequestBody CreateCustomKeyStoreRequestDto request) {
        log.info("Creating custom key store: {}", request.getKeyStoreName());
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            CustomKeyStoreResponseDto response = customKeyStoreService.createCustomKeyStore(tenant, request);
            auditService.logAction(tenant, IKmsActionType.Types.CREATE_CUSTOM_KEY_STORE, String.valueOf(response.getKeyStoreId()),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseCreated(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<CustomKeyStoreResponseDto> describeCustomKeyStore(@PathVariable String keyStoreId) {
        log.info("Describing custom key store: {}", keyStoreId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            CustomKeyStoreResponseDto response = customKeyStoreService.describeCustomKeyStore(tenant, keyStoreId);
            auditService.logAction(tenant, IKmsActionType.Types.DESCRIBE_CUSTOM_KEY_STORE, keyStoreId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<CustomKeyStoreResponseDto> updateCustomKeyStore(@PathVariable String keyStoreId,
                                                                          @Valid @RequestBody UpdateCustomKeyStoreRequestDto request) {
        log.info("Updating custom key store: {}", keyStoreId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            CustomKeyStoreResponseDto response = customKeyStoreService.updateCustomKeyStore(tenant, keyStoreId, request);
            auditService.logAction(tenant, IKmsActionType.Types.UPDATE_CUSTOM_KEY_STORE, keyStoreId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<?> deleteCustomKeyStore(@PathVariable String keyStoreId) {
        log.info("Deleting custom key store: {}", keyStoreId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            customKeyStoreService.deleteCustomKeyStore(tenant, keyStoreId);
            auditService.logAction(tenant, IKmsActionType.Types.DELETE_CUSTOM_KEY_STORE, keyStoreId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseNoContent();
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<ListCustomKeyStoresResponseDto> listCustomKeyStores(@RequestParam(required = false) Integer limit,
                                                                              @RequestParam(required = false) String nextToken) {
        log.info("Listing custom key stores with limit: {}", limit);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            ListCustomKeyStoresResponseDto response = customKeyStoreService.listCustomKeyStores(tenant, limit, nextToken);
            auditService.logAction(tenant, IKmsActionType.Types.LIST_CUSTOM_KEY_STORES, "-",
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<?> connectCustomKeyStore(@PathVariable String keyStoreId) {
        log.info("Connecting custom key store: {}", keyStoreId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            customKeyStoreService.connectCustomKeyStore(tenant, keyStoreId);
            auditService.logAction(tenant, IKmsActionType.Types.CONNECT_CUSTOM_KEY_STORE, keyStoreId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk("Custom key store connected successfully");
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<?> disconnectCustomKeyStore(@PathVariable String keyStoreId) {
        log.info("Disconnecting custom key store: {}", keyStoreId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            customKeyStoreService.disconnectCustomKeyStore(tenant, keyStoreId);
            auditService.logAction(tenant, IKmsActionType.Types.DISCONNECT_CUSTOM_KEY_STORE, keyStoreId,
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk("Custom key store disconnected successfully");
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    // ============================================================================
    // AUDIT & UTILITY APIs
    // ============================================================================

    @Override
    public ResponseEntity<AuditLogResponseDto> getAuditLogs(@RequestParam(required = false) Long keyId,
                                                            @RequestParam(required = false) String fromDate,
                                                            @RequestParam(required = false) String toDate,
                                                            @RequestParam(required = false) Integer limit) {
        log.info("Getting audit logs for keyId: {}, fromDate: {}, toDate: {}, limit: {}", keyId, fromDate, toDate, limit);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            LocalDateTime from = fromDate != null ? LocalDateTime.parse(fromDate, DateTimeFormatter.ISO_DATE_TIME) : null;
            LocalDateTime to = toDate != null ? LocalDateTime.parse(toDate, DateTimeFormatter.ISO_DATE_TIME) : null;

            AuditLogResponseDto response = auditService.getAuditLogs(tenant, String.valueOf(keyId), from, to, limit);
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<KeyUsageStatsDto> getKeyUsageStats(@PathVariable Long keyId) {
        log.info("Getting usage stats for key: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            KeyUsageStatsDto response = keyManagementService.getKeyUsageStats(tenant, keyId);
            auditService.logAction(tenant, IKmsActionType.Types.GET_KEY_USAGE_STATS, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<String> generateRandomData(@RequestParam Integer length,
                                                     @RequestParam IEnumCharSet.Types charSetType) {
        log.info("Generating random data of length: {} with charset: {}", length, charSetType);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            String response = keyService.generateRandomData(tenant, length, charSetType);
            auditService.logAction(tenant, IKmsActionType.Types.GENERATE_RANDOM_DATA, "-",
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk(response);
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }

    @Override
    public ResponseEntity<?> validateKey(@PathVariable Long keyId) {
        log.info("Validating key: {}", keyId);
        try {
            String tenant = requestContextService.getCurrentContext().getSenderTenant();
            keyManagementService.validateKey(tenant, keyId);
            auditService.logAction(tenant, IKmsActionType.Types.VALIDATE_KEY, String.valueOf(keyId),
                    requestContextService.getCurrentContext().getSenderUser(),
                    requestContextService.getCurrentContext().getClientIp());
            return ResponseFactory.responseOk("Key is valid and usable");
        } catch (Throwable e) {
            log.error(CtrlConstants.ERROR_API_EXCEPTION, e);
            return getBackExceptionResponse(e);
        }
    }
}