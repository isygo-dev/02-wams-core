package eu.isygoit.service;

import eu.isygoit.dto.request.SignRequestDto;
import eu.isygoit.dto.request.VerifyRequestDto;
import eu.isygoit.dto.response.SignResponseDto;
import eu.isygoit.dto.response.VerifyResponseDto;

/**
 * The interface Signing service.
 */
public interface ISigningService {

    /**
     * Sign.
     *
     * @param tenant  the tenant
     * @param request the request
     * @return the sign response dto
     */
    SignResponseDto sign(String tenant, SignRequestDto request);

    /**
     * Verify.
     *
     * @param tenant  the tenant
     * @param request the request
     * @return the verify response dto
     */
    VerifyResponseDto verify(String tenant, VerifyRequestDto request);
}

