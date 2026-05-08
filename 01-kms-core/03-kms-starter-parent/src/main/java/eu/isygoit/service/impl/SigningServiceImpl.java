package eu.isygoit.service.impl;

import eu.isygoit.dto.request.GenerateMacRequestDto;
import eu.isygoit.dto.request.SignRequestDto;
import eu.isygoit.dto.request.VerifyMacRequestDto;
import eu.isygoit.dto.request.VerifyRequestDto;
import eu.isygoit.dto.response.GenerateMacResponseDto;
import eu.isygoit.dto.response.SignResponseDto;
import eu.isygoit.dto.response.VerifyMacResponseDto;
import eu.isygoit.dto.response.VerifyResponseDto;
import eu.isygoit.service.ISigningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.UUID;

import eu.isygoit.enums.IEnumKeyPurpose;
import eu.isygoit.model.KmsKey;
import eu.isygoit.repository.KmsKeyRepository;
import eu.isygoit.service.ICryptoService;
import lombok.RequiredArgsConstructor;

/**
 * The type Signing service.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SigningServiceImpl implements ISigningService {

    private final KmsKeyRepository kmsKeyRepository;
    private final ICryptoService cryptoService;

    @Override
    public SignResponseDto sign(String tenant, SignRequestDto request) {
        log.info("Signing message for tenant: {} with keyId: {} using algorithm: {}",
                tenant, request.getKeyId(), request.getAlgorithm());

        KmsKey kmsKey = kmsKeyRepository.findByTenantAndKeyId(tenant, request.getKeyId())
                .orElseThrow(() -> new RuntimeException("KMS Key not found"));

        if (!kmsKey.isEnabled()) {
            throw new RuntimeException("KMS Key is not enabled");
        }

        if (kmsKey.getKeyPurpose() != IEnumKeyPurpose.Types.SIGN_VERIFY) {
            throw new RuntimeException("KMS Key is not authorized for signing");
        }

        byte[] message = Base64.getDecoder().decode(request.getMessage());
        byte[] signature = cryptoService.signData(message, kmsKey.getKeyMaterial(), request.getAlgorithm().meaning());

        return SignResponseDto.builder()
                .signature(Base64.getEncoder().encodeToString(signature))
                .keyId(kmsKey.getKeyId())
                .build();
    }

    @Override
    public VerifyResponseDto verify(String tenant, VerifyRequestDto request) {
        log.info("Verifying signature for tenant: {} with keyId: {}", tenant, request.getKeyId());

        KmsKey kmsKey = kmsKeyRepository.findByTenantAndKeyId(tenant, request.getKeyId())
                .orElseThrow(() -> new RuntimeException("KMS Key not found"));

        if (!kmsKey.isEnabled()) {
            throw new RuntimeException("KMS Key is not enabled");
        }

        byte[] message = Base64.getDecoder().decode(request.getMessage());
        byte[] signature = Base64.getDecoder().decode(request.getSignature());
        boolean valid = cryptoService.verifySignature(message, signature, kmsKey.getKeyMaterial(), request.getAlgorithm().meaning());

        return VerifyResponseDto.builder()
                .valid(valid)
                .build();
    }

    @Override
    public GenerateMacResponseDto generateMac(String tenant, GenerateMacRequestDto request) {
        log.info("Generating MAC for tenant: {} with keyId: {}", tenant, request.getKeyId());

        KmsKey kmsKey = kmsKeyRepository.findByTenantAndKeyId(tenant, request.getKeyId())
                .orElseThrow(() -> new RuntimeException("KMS Key not found"));

        if (!kmsKey.isEnabled()) {
            throw new RuntimeException("KMS Key is not enabled");
        }

        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance(request.getMacAlgorithm());
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(kmsKey.getKeyMaterial(), request.getMacAlgorithm());
            mac.init(keySpec);
            byte[] message = Base64.getDecoder().decode(request.getMessage());
            byte[] macBytes = mac.doFinal(message);

            return GenerateMacResponseDto.builder()
                    .mac(Base64.getEncoder().encodeToString(macBytes))
                    .keyId(kmsKey.getKeyId())
                    .build();
        } catch (Exception e) {
            log.error("MAC generation failed", e);
            throw new RuntimeException("MAC generation failed", e);
        }
    }

    @Override
    public VerifyMacResponseDto verifyMac(String tenant, VerifyMacRequestDto request) {
        log.info("Verifying MAC for tenant: {} with keyId: {}", tenant, request.getKeyId());

        GenerateMacResponseDto response = generateMac(tenant, GenerateMacRequestDto.builder()
                .keyId(request.getKeyId())
                .message(request.getMessage())
                .macAlgorithm(request.getMacAlgorithm())
                .build());

        boolean valid = response.getMac().equals(request.getMac());

        return VerifyMacResponseDto.builder()
                .macValid(valid)
                .build();
    }
}

