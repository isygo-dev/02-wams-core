package eu.isygoit.service;

import eu.isygoit.dto.request.*;
import eu.isygoit.dto.response.GenerateMacResponseDto;
import eu.isygoit.dto.response.SignResponseDto;
import eu.isygoit.dto.response.VerifyResponseDto;
import jakarta.validation.Valid;

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

    GenerateMacResponseDto generateMac(String tenant, @Valid GenerateMacRequestDto request);

    VerifyMacResponseDto verifyMac(String tenant, @Valid VerifyMacRequestDto request);
}

