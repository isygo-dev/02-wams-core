package eu.isygoit.service;

import eu.isygoit.dto.request.CreateKeyRequestDto;
import eu.isygoit.dto.response.CreateKeyResponseDto;
import eu.isygoit.dto.response.KeyMetadataResponseDto;
import eu.isygoit.dto.response.ListKeysResponseDto;
import eu.isygoit.dto.response.RotateKeyResponseDto;

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
    KeyMetadataResponseDto getKeyMetadata(String tenant, String keyId);

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
    KeyMetadataResponseDto enableKey(String tenant, String keyId);

    /**
     * Disable key.
     *
     * @param tenant the tenant
     * @param keyId  the key id
     * @return the key metadata response dto
     */
    KeyMetadataResponseDto disableKey(String tenant, String keyId);

    /**
     * Schedule key deletion.
     *
     * @param tenant              the tenant
     * @param keyId               the key id
     * @param pendingWindowInDays the pending window in days
     * @return the key metadata response dto
     */
    KeyMetadataResponseDto scheduleKeyDeletion(String tenant, String keyId, Integer pendingWindowInDays);

    /**
     * Rotate key.
     *
     * @param tenant the tenant
     * @param keyId  the key id
     * @return the rotate key response dto
     */
    RotateKeyResponseDto rotateKey(String tenant, String keyId);
}

