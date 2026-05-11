package eu.isygoit.service;

import eu.isygoit.dto.KmsDtos.*;
import jakarta.validation.Valid;

import java.util.List;

/**
 * The interface Key management service.
 */
public interface IKeyManagementService {

    /**
     * Create key.
     *
     * @param tenant  the tenant
     * @param request the request
     * @return the create key response dto
     */
    CreateKeyResponse createKey(String tenant, CreateKeyRequest request);

    /**
     * Get key metadata.
     *
     * @param tenant the tenant
     * @param keyId  the key id
     * @return the key metadata response dto
     */
    DescribeKeyResponse describeKey(String tenant, String keyId, List<String> grantTokens);

    /**
     * List keys.
     *
     * @param tenant    the tenant
     * @param limit     the limit
     * @param marker the next token
     * @return the list keys response dto
     */
    ListKeysResponse listKeys(String tenant, Integer limit, String marker);

    /**
     * Enable key.
     *
     * @param tenant the tenant
     * @param keyId  the key id
     * @return the key metadata response dto
     */
    EnableKeyResponse enableKey(String tenant, String keyId);

    /**
     * Disable key.
     *
     * @param tenant the tenant
     * @param keyId  the key id
     * @return the key metadata response dto
     */
    DisableKeyResponse disableKey(String tenant, String keyId);

    /**
     * Schedule key deletion.
     *
     * @param tenant              the tenant
     * @param keyId               the key id
     * @param pendingWindowInDays the pending window in days
     * @return the key metadata response dto
     */
    ScheduleKeyDeletionResponse scheduleKeyDeletion(
            String tenant,
            String keyId,
            Integer pendingWindowInDays);

    /**
     * Rotate key.
     *
     * @param tenant the tenant
     * @param keyId  the key id
     * @return the rotate key response dto
     */
    RotateKeyResponse rotateKey(String tenant, String keyId);

    UpdateKeyDescriptionResponse updateKeyDescription(
            String tenant,
            String keyId,
            UpdateKeyDescriptionRequest request);

    CancelKeyDeletionResponse cancelKeyDeletion(
            String tenant,
            String keyId);

    KeyRotationStatusResponseDto updateKeyRotation(
            String tenant,
            String keyId,
            UpdateKeyRotationRequestDto request);

    GetKeyRotationStatusResponse getKeyRotationStatus(
            String tenant,
            String keyId);

    GetPublicKeyResponse getPublicKey(String tenant, String keyId);

    AliasResponseDto createAlias(String tenant, @Valid CreateAliasRequestDto request);

    AliasResponseDto updateAlias(String tenant, String aliasName, @Valid UpdateAliasRequestDto request);

    void deleteAlias(String tenant, String aliasName);

    ListAliasesResponseDto listAliases(String tenant, Integer limit, String nextToken);

    ListAliasesResponseDto listAliasesForKey(String tenant, String keyId, Integer limit, String nextToken);

    Object tagResource(String tenant, String keyId, @Valid TagResourceRequestDto request);

    Object untagResource(String tenant, String keyId, @Valid UntagResourceRequestDto request);

    ListTagsResponseDto listResourceTags(String tenant, String keyId);

    ImportParametersResponseDto getParametersForImport(String tenant, String keyId);

    KeyDescriptionResponseDto importKeyMaterial(String tenant, String keyId, @Valid ImportKeyMaterialRequestDto request);

    KeyDescriptionResponseDto deleteImportedKeyMaterial(String tenant, String keyId);

    void validateKey(String tenant, String keyId);

    void deleteKey(String tenant, String keyId);

    ListKeyRotationsResponseDto listKeyRotations(String tenant, String keyId, Integer limit, String nextToken);

    KeyUsageStatsResponseDto getKeyUsageStats(String tenant, String keyId);

    int countKeysInCustomKeyStore(String tenant, Long keyStoreId);
}

