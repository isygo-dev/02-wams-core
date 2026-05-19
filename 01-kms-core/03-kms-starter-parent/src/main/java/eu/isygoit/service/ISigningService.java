package eu.isygoit.service;

import eu.isygoit.dto.KmsDtos.*;
import jakarta.validation.Valid;

/**
 * The interface Signing service.
 */
public interface ISigningService {

    SignResponse sign(String tenant, SignRequest request);
    VerifyResponse verify(String tenant, VerifyRequest request);
    GenerateMacResponse generateMac(String tenant, @Valid GenerateMacRequest request);
    VerifyMacResponse verifyMac(String tenant, @Valid VerifyMacRequest request);
}

