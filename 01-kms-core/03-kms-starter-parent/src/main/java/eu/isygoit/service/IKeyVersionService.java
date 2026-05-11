package eu.isygoit.service;

import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.dto.KmsDtos.ListKeyVersionsResponse;

/**
 * The interface Key version service.
 */
public interface IKeyVersionService {

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
            String marker);

    /**
     * Get active version.
     *
     * @param tenant the tenant
     * @param keyId  the key id
     * @return the active version response dto
     */
    ActiveVersionResponseDto getActiveVersion(String tenant, String keyId);
}

