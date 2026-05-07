package eu.isygoit.service;

import eu.isygoit.dto.request.CreateGrantRequestDto;
import eu.isygoit.dto.request.RetireGrantRequestDto;
import eu.isygoit.dto.request.SetKeyPolicyRequestDto;
import eu.isygoit.dto.response.GrantResponseDto;
import eu.isygoit.dto.response.ListGrantsResponseDto;
import jakarta.validation.Valid;

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
    Map<String, Object> setKeyPolicy(String tenant, Long keyId, SetKeyPolicyRequestDto request);

    /**
     * Get key policy.
     *
     * @param tenant the tenant
     * @param keyId  the key id
     * @return the map
     */
    Map<String, Object> getKeyPolicy(String tenant, Long keyId);

    /**
     * Create grant.
     *
     * @param tenant  the tenant
     * @param keyId   the key id
     * @param request the request
     * @return the grant response dto
     */
    GrantResponseDto createGrant(String tenant, Long keyId, CreateGrantRequestDto request);

    /**
     * Revoke grant.
     *
     * @param tenant  the tenant
     * @param keyId   the key id
     * @param grantId the grant id
     * @return the string
     */
    String revokeGrant(String tenant, Long keyId, String grantId);

    ListGrantsResponseDto listGrants(String tenant, Long keyId, Integer limit, String nextToken);

    Object retireGrant(String tenant, String grantId, @Valid RetireGrantRequestDto request);
}

