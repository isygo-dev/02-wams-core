package eu.isygoit.service;

import eu.isygoit.dto.request.*;
import eu.isygoit.dto.response.*;
import eu.isygoit.dto.response.ImportParametersResponseDto;
import eu.isygoit.dto.response.KeyRotationStatusDto;
import jakarta.validation.Valid;

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
    CreateKeyResponseDto createKey(String tenant, CreateKeyRequestDto request);

    /**
     * Get key metadata.
     *
     * @param tenant the tenant
     * @param keyId  the key id
     * @return the key metadata response dto
     */
    KeyMetadataResponseDto getKeyMetadata(String tenant, Long keyId);

    /**
     * List keys.
     *
     * @param tenant    the tenant
     * @param limit     the limit
     * @param nextToken the next token
     * @return the list keys response dto
     */
    ListKeysResponseDto listKeys(String tenant, Integer limit, String nextToken);

    /**
     * Enable key.
     *
     * @param tenant the tenant
     * @param keyId  the key id
     * @return the key metadata response dto
     */
    KeyMetadataResponseDto enableKey(String tenant, Long keyId);

    /**
     * Disable key.
     *
     * @param tenant the tenant
     * @param keyId  the key id
     * @return the key metadata response dto
     */
    KeyMetadataResponseDto disableKey(String tenant, Long keyId);

    /**
     * Schedule key deletion.
     *
     * @param tenant              the tenant
     * @param keyId               the key id
     * @param pendingWindowInDays the pending window in days
     * @return the key metadata response dto
     */
    KeyMetadataResponseDto scheduleKeyDeletion(String tenant, Long keyId, Integer pendingWindowInDays);

    /**
     * Rotate key.
     *
     * @param tenant the tenant
     * @param keyId  the key id
     * @return the rotate key response dto
     */
    RotateKeyResponseDto rotateKey(String tenant, Long keyId);

    KeyMetadataResponseDto updateKeyMetadata(String tenant, Long keyId, @Valid UpdateKeyMetadataRequestDto request);

    KeyMetadataResponseDto cancelKeyDeletion(String tenant, Long keyId);

    KeyRotationStatusDto updateKeyRotation(String tenant, Long keyId, @Valid UpdateKeyRotationRequestDto request);

    KeyRotationStatusDto getKeyRotationStatus(String tenant, Long keyId);

    PublicKeyResponseDto getPublicKey(String tenant, Long keyId);

    AliasResponseDto createAlias(String tenant, @Valid CreateAliasRequestDto request);

    AliasResponseDto updateAlias(String tenant, String aliasName, @Valid UpdateAliasRequestDto request);

    void deleteAlias(String tenant, String aliasName);

    ListAliasesResponseDto listAliases(String tenant, Integer limit, String nextToken);

    ListAliasesResponseDto listAliasesForKey(String tenant, Long keyId);

    Object tagResource(String tenant, Long keyId, @Valid TagResourceRequestDto request);

    Object untagResource(String tenant, Long keyId, @Valid UntagResourceRequestDto request);

    ListTagsResponseDto listResourceTags(String tenant, Long keyId);

    ImportParametersResponseDto getParametersForImport(String tenant, Long keyId);

    KeyMetadataResponseDto importKeyMaterial(String tenant, Long keyId, @Valid ImportKeyMaterialRequestDto request);

    KeyMetadataResponseDto deleteImportedKeyMaterial(String tenant, Long keyId);

    void validateKey(String tenant, Long keyId);

    void deleteKey(String tenant, Long keyId);

    ListKeyRotationsResponseDto listKeyRotations(String tenant, Long keyId, Integer limit, String nextToken);

    KeyUsageStatsDto getKeyUsageStats(String tenant, Long keyId);

    int countKeysInCustomKeyStore(String tenant, String keyStoreId);
}

