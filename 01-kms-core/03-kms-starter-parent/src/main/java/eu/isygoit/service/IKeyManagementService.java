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
     * @param tenant the tenant
     * @param limit  the limit
     * @param nextToken the next token
     * @return the list keys response dto
     */
    ListKeysResponse listKeys(String tenant, Integer limit, String nextToken);

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

    boolean isValidKey(String tenant, String keyId);

    void deleteKey(String tenant, String keyId);

    ListKeyRotationsResponseDto listKeyRotations(String tenant, String keyId, Integer limit, String nextToken);

    KeyUsageStatsResponseDto getKeyUsageStats(String tenant, String keyId);

    /**
     * Registers a key as belonging to a specific custom key store.
     * Used when a key is generated inside a custom key store.
     *
     * @param tenant      the tenant identifier
     * @param keyStoreId  the custom key store ID
     * @param keyId       the KMS key ID
     */
    void registerKeyInCustomStore(String tenant, Long keyStoreId, String keyId);

    /**
     * Unregisters a key from a custom key store (e.g., when key is deleted).
     *
     * @param tenant      the tenant identifier
     * @param keyStoreId  the custom key store ID
     * @param keyId       the KMS key ID
     */
    void unregisterKeyFromCustomStore(String tenant, Long keyStoreId, String keyId);

    /**
     * Counts how many keys are currently hosted in a given custom key store.
     *
     * @param tenant     the tenant identifier
     * @param keyStoreId the custom key store ID
     * @return number of keys in that store
     */
    int countKeysInCustomKeyStore(String tenant, Long keyStoreId);

}

