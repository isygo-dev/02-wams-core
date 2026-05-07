package eu.isygoit.service;

import eu.isygoit.dto.response.ActiveVersionResponseDto;
import eu.isygoit.dto.response.KeyVersionListResponseDto;

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
    KeyVersionListResponseDto listKeyVersions(String tenant, Long keyId);

    /**
     * Get active version.
     *
     * @param tenant the tenant
     * @param keyId  the key id
     * @return the active version response dto
     */
    ActiveVersionResponseDto getActiveVersion(String tenant, Long keyId);
}

