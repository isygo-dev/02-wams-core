package eu.isygoit.service.impl;

import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.enums.IEnumKeyUsage;
import eu.isygoit.model.KmsKey;
import eu.isygoit.repository.KmsKeyRepository;
import eu.isygoit.service.ICryptoService;
import eu.isygoit.service.ISigningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * The type Signing service.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SigningService implements ISigningService {

    private final KmsKeyRepository kmsKeyRepository;
    private final ICryptoService cryptoService;

    @Override
    public SignResponse sign(String tenant, SignRequest request) {

        log.info("Signing message for tenant: {} with keyId: {} using algorithm: {}",
                tenant,
                request.getKeyId(),
                request.getSigningAlgorithm());

        KmsKey kmsKey = kmsKeyRepository.findByTenantAndKeyId(
                        tenant,
                        request.getKeyId()
                )
                .orElseThrow(() -> new RuntimeException("KMS Key not found"));

        if (!kmsKey.isEnabled()) {
            throw new RuntimeException("KMS Key is not enabled");
        }

        if (kmsKey.getKeyUsage() != IEnumKeyUsage.Types.SIGN_VERIFY) {
            throw new RuntimeException("KMS Key is not authorized for signing");
        }

        if (request.getSigningAlgorithm() == null) {
            throw new RuntimeException("Signing algorithm is required");
        }

        byte[] message = Base64.getDecoder().decode(request.getMessage());

        byte[] signature = cryptoService.signData(
                message,
                kmsKey.getKeyMaterial(),
                request.getSigningAlgorithm()
        );

        return SignResponse.builder()
                .signature(Base64.getEncoder().encodeToString(signature))
                .keyId(kmsKey.getKeyId())
                .keyVersionId(kmsKey.getCurrentVersionId())
                .build();
    }

    @Override
    public VerifyResponse verify(String tenant, VerifyRequest request) {

        log.info("Verifying signature for tenant: {} with keyId: {}", tenant, request.getKeyId());

        KmsKey kmsKey = kmsKeyRepository.findByTenantAndKeyId(tenant, request.getKeyId())
                .orElseThrow(() -> new RuntimeException("KMS Key not found"));

        if (!kmsKey.isEnabled()) {
            throw new RuntimeException("KMS Key is not enabled");
        }

        if (request.getSigningAlgorithm() == null) {
            throw new RuntimeException("Algorithm is required");
        }

        byte[] message = Base64.getDecoder().decode(request.getMessage());
        byte[] signature = Base64.getDecoder().decode(request.getSignature());

        boolean valid = cryptoService.verifySignature(
                message,
                signature,
                kmsKey.getKeyMaterial(),
                request.getSigningAlgorithm()
        );

        return VerifyResponse.builder()
                .valid(valid)
                .keyId(kmsKey.getKeyId())
                .build();
    }

    @Override
    public GenerateMacResponse generateMac(String tenant, GenerateMacRequest request) {

        log.info("Generating MAC for tenant: {} with keyId: {}", tenant, request.getKeyId());

        KmsKey kmsKey = kmsKeyRepository.findByTenantAndKeyId(tenant, request.getKeyId())
                .orElseThrow(() -> new RuntimeException("KMS Key not found"));

        if (!kmsKey.isEnabled()) {
            throw new RuntimeException("KMS Key is not enabled");
        }

        try {
            Mac mac = Mac.getInstance(request.getMacAlgorithm());

            SecretKeySpec keySpec =
                    new SecretKeySpec(kmsKey.getKeyMaterial(), request.getMacAlgorithm());

            mac.init(keySpec);

            byte[] message = Base64.getDecoder().decode(request.getMessage());
            byte[] macBytes = mac.doFinal(message);

            return GenerateMacResponse.builder()
                    .mac(Base64.getEncoder().encodeToString(macBytes))
                    .keyId(kmsKey.getKeyId())
                    .build();

        } catch (Exception e) {
            log.error("MAC generation failed", e);
            throw new RuntimeException("MAC generation failed", e);
        }
    }

    @Override
    public VerifyMacResponse verifyMac(String tenant, VerifyMacRequest request) {

        log.info("Verifying MAC for tenant: {} with keyId: {}", tenant, request.getKeyId());

        KmsKey kmsKey = kmsKeyRepository.findByTenantAndKeyId(tenant, request.getKeyId())
                .orElseThrow(() -> new RuntimeException("KMS Key not found"));

        if (!kmsKey.isEnabled()) {
            throw new RuntimeException("KMS Key is not enabled");
        }

        try {
            Mac mac = Mac.getInstance(request.getMacAlgorithm());

            SecretKeySpec keySpec =
                    new SecretKeySpec(kmsKey.getKeyMaterial(), request.getMacAlgorithm());

            mac.init(keySpec);

            byte[] message = Base64.getDecoder().decode(request.getMessage());
            byte[] expectedMac = mac.doFinal(message);

            byte[] providedMac = Base64.getDecoder().decode(request.getMac());

            boolean valid = MessageDigest.isEqual(expectedMac, providedMac);

            return VerifyMacResponse.builder()
                    .macValid(valid)
                    .keyId(kmsKey.getKeyId())
                    .build();

        } catch (Exception e) {
            log.error("MAC verification failed", e);
            throw new RuntimeException("MAC verification failed", e);
        }
    }
}

