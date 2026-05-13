package eu.isygoit.service.impl;

import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.enums.IEnumKeyUsage;
import eu.isygoit.exception.KeyNotFoundException;
import eu.isygoit.model.KmsKey;
import eu.isygoit.repository.KmsAliasRepository;
import eu.isygoit.repository.KmsKeyRepository;
import eu.isygoit.service.ICryptoService;
import eu.isygoit.service.IEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;

/**
 * The type Encryption service.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EncryptionService implements IEncryptionService {

    private final KmsKeyRepository kmsKeyRepository;
    private final ICryptoService cryptoService;
    private final KmsAliasRepository kmsAliasRepository;

    @Override
    public EncryptResponse encrypt(
            String tenant,
            EncryptRequest request) {

        log.info("Encrypting data for tenant: {} with keyId: {}",
                tenant,
                request.getKeyId());

        KmsKey kmsKey = kmsKeyRepository.findByTenantAndKeyId(
                        tenant,
                        request.getKeyId()
                )
                .orElseThrow(() -> new RuntimeException("KMS Key not found"));

        if (!kmsKey.isEnabled()) {
            throw new RuntimeException("KMS Key is not enabled");
        }

        if (kmsKey.getKeyUsage() != IEnumKeyUsage.Types.ENCRYPT_DECRYPT) {
            throw new RuntimeException(
                    "KMS Key is not authorized for encryption"
            );
        }

        byte[] plaintext =
                Base64.getDecoder().decode(request.getPlaintext());

        byte[] ciphertext = cryptoService.encryptData(
                plaintext,
                kmsKey.getKeyMaterial(),
                request.getEncryptionContext()
        );

        return EncryptResponse.builder()
                .ciphertextBlob(Base64.getEncoder().encodeToString(ciphertext))
                .keyId(kmsKey.getKeyId())
                .keyVersionId(kmsKey.getCurrentVersionId())
                .build();
    }

    @Override
    public DecryptResponse decrypt(
            String tenant,
            DecryptRequest request) {

        log.info("Decrypting data for tenant: {}", tenant);

        String keyId = request.getKeyId();

        if (keyId == null) {
            throw new RuntimeException(
                    "keyId is required for decryption in this implementation"
            );
        }

        KmsKey kmsKey = kmsKeyRepository.findByTenantAndKeyId(
                        tenant,
                        keyId
                )
                .orElseThrow(() -> new RuntimeException("KMS Key not found"));

        if (!kmsKey.isEnabled()) {
            throw new RuntimeException("KMS Key is not enabled");
        }

        byte[] ciphertext =
                Base64.getDecoder().decode(request.getCiphertextBlob());

        byte[] plaintext = cryptoService.decryptData(
                tenant,
                ciphertext,
                kmsKey.getKeyMaterial(),
                request.getEncryptionContext()
        );

        return DecryptResponse.builder()
                .plaintext(Base64.getEncoder().encodeToString(plaintext))
                .keyId(kmsKey.getKeyId())
                .keyVersionId(kmsKey.getCurrentVersionId())
                .build();
    }

    @Override
    public ReEncryptResponse reEncrypt(
            String tenant,
            ReEncryptRequest request) {

        log.info("Re-encrypting data for tenant: {} from key: {} to key: {}",
                tenant,
                request.getSourceKeyId(),
                request.getDestinationKeyId());

        // 1. Validate source key
        KmsKey sourceKey = kmsKeyRepository.findByTenantAndKeyId(
                        tenant,
                        request.getSourceKeyId()
                )
                .orElseThrow(() -> new KeyNotFoundException(request.getSourceKeyId()));

        if (!sourceKey.isEnabled()) {
            throw new RuntimeException("Source key is not enabled");
        }

        // 2. Decrypt
        byte[] ciphertext = Base64.getDecoder().decode(request.getCiphertextBlob());

        byte[] plaintext = cryptoService.decryptData(
                tenant,
                ciphertext,
                sourceKey.getKeyMaterial(),
                request.getSourceEncryptionContext()
        );

        // 3. Validate destination key
        KmsKey destinationKey = kmsKeyRepository.findByTenantAndKeyId(
                        tenant,
                        request.getDestinationKeyId()
                )
                .orElseThrow(() -> new KeyNotFoundException(request.getDestinationKeyId()));

        if (!destinationKey.isEnabled()) {
            throw new RuntimeException("Destination key is not enabled");
        }

        // 4. Encrypt with destination key
        byte[] newCiphertext = cryptoService.encryptData(
                plaintext,
                destinationKey.getKeyMaterial(),
                request.getDestinationEncryptionContext()
        );

        // 5. Build response
        return ReEncryptResponse.builder()
                .ciphertextBlob(Base64.getEncoder().encodeToString(newCiphertext))
                .sourceKeyId(request.getSourceKeyId())
                .destinationKeyId(request.getDestinationKeyId())
                .destinationKeyVersionId(destinationKey.getCurrentVersionId())
                .build();
    }
}

