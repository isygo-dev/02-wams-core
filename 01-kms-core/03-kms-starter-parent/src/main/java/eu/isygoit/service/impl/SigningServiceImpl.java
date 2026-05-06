package eu.isygoit.service.impl;

import eu.isygoit.dto.request.SignRequestDto;
import eu.isygoit.dto.request.VerifyRequestDto;
import eu.isygoit.dto.response.SignResponseDto;
import eu.isygoit.dto.response.VerifyResponseDto;
import eu.isygoit.service.ISigningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.UUID;

/**
 * The type Signing service.
 */
@Slf4j
@Service
@Transactional
public class SigningServiceImpl implements ISigningService {

    @Override
    public SignResponseDto sign(String tenant, SignRequestDto request) {
        log.info("Signing message for tenant: {} with keyId: {} using algorithm: {}",
                tenant, request.getKeyId(), request.getAlgorithm());

        // Mock signature generation - in production, use actual signing algorithms
        String signature = Base64.getEncoder().encodeToString(
                ("signature-" + UUID.randomUUID()).getBytes()
        );

        return SignResponseDto.builder()
                .signature(signature)
                .keyId(request.getKeyId())
                .build();
    }

    @Override
    public VerifyResponseDto verify(String tenant, VerifyRequestDto request) {
        log.info("Verifying signature for tenant: {} with keyId: {}", tenant, request.getKeyId());

        // Mock signature verification - in production, use actual verification algorithms
        boolean valid = request.getSignature() != null && !request.getSignature().isEmpty();

        return VerifyResponseDto.builder()
                .valid(valid)
                .build();
    }
}

