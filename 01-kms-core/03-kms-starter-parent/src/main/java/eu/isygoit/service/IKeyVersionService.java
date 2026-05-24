package eu.isygoit.service;

import eu.isygoit.dto.KmsDtos.*;

/**
 * The interface Key version service.
 */
public interface IKeyVersionService {

    /**
     * Disable key / version.
     *
     * @param tenant the tenant
     * @param keyId  the key id
     * @param keyVersionId the key version id
     * @return the key metadata response dto
     */
    DisableKeyVersionResponse disableKeyVersion(String tenant, String keyId, String keyVersionId);

    /**
     * Enable key / version.
     *
     * @param tenant the tenant
     * @param keyId  the key id
     * @param keyVersionId the key version id
     * @return the key metadata response dto
     */
    EnableKeyVersionResponse enableKeyVersion(String tenant, String keyId, String keyVersionId);

    /**
     * List key versions.
     *
     * @param tenant the tenant
     * @param keyId  the key id
     * @return the key version list response dto
     */
    public ListKeyVersionsResponse listKeyVersions(
            String tenant,
            String keyId,
            Integer limit,
            String nextToken);

    /**
     * Get active version.
     *
     * @param tenant the tenant
     * @param keyId  the key id
     * @return the active version response dto
     */
    ActiveVersionResponse getActiveVersion(String tenant, String keyId);
}

