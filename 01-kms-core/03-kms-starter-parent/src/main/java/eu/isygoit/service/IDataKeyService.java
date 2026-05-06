package eu.isygoit.service;

import eu.isygoit.dto.request.GenerateDataKeyRequestDto;
import eu.isygoit.dto.response.DataKeyResponseDto;

/**
 * The interface Data key service.
 */
public interface IDataKeyService {

    /**
     * Generate data key.
     *
     * @param tenant  the tenant
     * @param request the request
     * @return the data key response dto
     */
    DataKeyResponseDto generateDataKey(String tenant, GenerateDataKeyRequestDto request);
}

