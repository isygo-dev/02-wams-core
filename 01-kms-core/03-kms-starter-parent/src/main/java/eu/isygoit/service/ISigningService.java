package eu.isygoit.service;

import eu.isygoit.dto.KmsDtos.*;
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
    SignResponse sign(String tenant, SignRequest request);

    /**
     * Verify.
     *
     * @param tenant  the tenant
     * @param request the request
     * @return the verify response dto
     */
    VerifyResponse verify(String tenant, VerifyRequest request);

    GenerateMacResponse generateMac(String tenant, @Valid GenerateMacRequest request);

    VerifyMacResponse verifyMac(String tenant, @Valid VerifyMacRequest request);
}

