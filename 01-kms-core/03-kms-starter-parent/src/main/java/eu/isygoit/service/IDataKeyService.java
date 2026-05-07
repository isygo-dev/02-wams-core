package eu.isygoit.service;

import eu.isygoit.dto.request.GenerateDataKeyPairRequestDto;
import eu.isygoit.dto.request.GenerateDataKeyRequestDto;
import eu.isygoit.dto.response.DataKeyPairResponseDto;
import eu.isygoit.dto.response.DataKeyResponseDto;
import jakarta.validation.Valid;

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

    DataKeyResponseDto generateDataKeyWithoutPlaintext(String tenant, @Valid GenerateDataKeyRequestDto request);

    DataKeyPairResponseDto generateDataKeyPair(String tenant, @Valid GenerateDataKeyPairRequestDto request);

    DataKeyPairResponseDto generateDataKeyPairWithoutPlaintext(String tenant, @Valid GenerateDataKeyPairRequestDto request);
}

