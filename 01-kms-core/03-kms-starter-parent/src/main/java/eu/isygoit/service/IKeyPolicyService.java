package eu.isygoit.service;

import eu.isygoit.dto.request.CreateGrantRequestDto;
import eu.isygoit.dto.request.SetKeyPolicyRequestDto;
import eu.isygoit.dto.response.GrantResponseDto;

import java.util.Map;

/**
 * The interface Key policy service.
 */
public interface IKeyPolicyService {

    /**
     * Set key policy.
     *
     * @param tenant  the tenant
     * @param keyId   the key id
     * @param request the request
     * @return the map
     */
    Map<String, Object> setKeyPolicy(String tenant, String keyId, SetKeyPolicyRequestDto request);

    /**
     * Get key policy.
     *
     * @param tenant the tenant
     * @param keyId  the key id
     * @return the map
     */
    Map<String, Object> getKeyPolicy(String tenant, String keyId);

    /**
     * Create grant.
     *
     * @param tenant  the tenant
     * @param keyId   the key id
     * @param request the request
     * @return the grant response dto
     */
    GrantResponseDto createGrant(String tenant, String keyId, CreateGrantRequestDto request);

    /**
     * Revoke grant.
     *
     * @param tenant  the tenant
     * @param keyId   the key id
     * @param grantId the grant id
     * @return the string
     */
    String revokeGrant(String tenant, String keyId, String grantId);
}

