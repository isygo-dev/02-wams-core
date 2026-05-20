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
 * Fixed: For asymmetric keys (RSA), uses public key for encryption and private key for decryption.
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
    public EncryptResponse encrypt(String tenant, EncryptRequest request) {
        log.info("Encrypting data for tenant: {} with keyId: {}", tenant, request.getKeyId());

        KmsKey kmsKey = kmsKeyRepository.findByTenantAndKeyId(tenant, request.getKeyId())
                .orElseThrow(() -> new RuntimeException("KMS Key not found"));

        if (!kmsKey.isEnabled()) {
            throw new RuntimeException("KMS Key is not enabled");
        }
        if (kmsKey.getKeyUsage() != IEnumKeyUsage.Types.ENCRYPT_DECRYPT) {
            throw new RuntimeException("KMS Key is not authorized for encryption");
        }

        byte[] plaintext = Base64.getDecoder().decode(request.getPlaintext());

        // Choose correct key material based on key type
        byte[] keyMaterial;
        if (kmsKey.getKeySpec() != null && kmsKey.getKeySpec().isAsymmetric()) {
            // For asymmetric keys, use PUBLIC key for encryption
            if (kmsKey.getPublicKey() == null) {
                throw new RuntimeException("Asymmetric key has no public key material");
            }
            keyMaterial = kmsKey.getPublicKey();
        } else {
            // For symmetric keys, use the secret key material
            keyMaterial = kmsKey.getKeyMaterial();
        }

        byte[] ciphertext = cryptoService.encryptData(
                plaintext,
                keyMaterial,
                kmsKey.getKeySpec(),
                request.getEncryptionContext()
        );

        return EncryptResponse.builder()
                .ciphertextBlob(Base64.getEncoder().encodeToString(ciphertext))
                .keyId(kmsKey.getKeyId())
                .keyVersionId(kmsKey.getCurrentVersionId())
                .build();
    }

    @Override
    public DecryptResponse decrypt(String tenant, DecryptRequest request) {
        log.info("Decrypting data for tenant: {}", tenant);

        String keyId = request.getKeyId();
        if (keyId == null) {
            throw new RuntimeException("keyId is required for decryption in this implementation");
        }

        KmsKey kmsKey = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new RuntimeException("KMS Key not found"));

        if (!kmsKey.isEnabled()) {
            throw new RuntimeException("KMS Key is not enabled");
        }

        byte[] ciphertext = Base64.getDecoder().decode(request.getCiphertextBlob());

        // For decryption, always use private key material (asymmetric) or the symmetric key
        byte[] keyMaterial = kmsKey.getKeyMaterial();

        byte[] plaintext = cryptoService.decryptData(
                tenant,
                ciphertext,
                keyMaterial,
                kmsKey.getKeySpec(),
                request.getEncryptionContext()
        );

        return DecryptResponse.builder()
                .plaintext(Base64.getEncoder().encodeToString(plaintext))
                .keyId(kmsKey.getKeyId())
                .keyVersionId(kmsKey.getCurrentVersionId())
                .build();
    }

    @Override
    public ReEncryptResponse reEncrypt(String tenant, ReEncryptRequest request) {
        log.info("Re-encrypting data for tenant: {} from key: {} to key: {}",
                tenant, request.getSourceKeyId(), request.getDestinationKeyId());

        // 1. Validate source key
        KmsKey sourceKey = kmsKeyRepository.findByTenantAndKeyId(tenant, request.getSourceKeyId())
                .orElseThrow(() -> new KeyNotFoundException(request.getSourceKeyId()));

        if (!sourceKey.isEnabled()) {
            throw new RuntimeException("Source key is not enabled");
        }

        // 2. Decrypt using source private/symmetric key
        byte[] ciphertext = Base64.getDecoder().decode(request.getCiphertextBlob());
        byte[] plaintext = cryptoService.decryptData(
                tenant,
                ciphertext,
                sourceKey.getKeyMaterial(),  // private key for RSA, symmetric key for AES
                sourceKey.getKeySpec(),
                request.getSourceEncryptionContext()
        );

        // 3. Validate destination key
        KmsKey destinationKey = kmsKeyRepository.findByTenantAndKeyId(tenant, request.getDestinationKeyId())
                .orElseThrow(() -> new KeyNotFoundException(request.getDestinationKeyId()));

        if (!destinationKey.isEnabled()) {
            throw new RuntimeException("Destination key is not enabled");
        }

        // 4. Encrypt with destination key (public key if asymmetric)
        byte[] destKeyMaterial;
        if (destinationKey.getKeySpec() != null && destinationKey.getKeySpec().isAsymmetric()) {
            if (destinationKey.getPublicKey() == null) {
                throw new RuntimeException("Destination asymmetric key has no public key");
            }
            destKeyMaterial = destinationKey.getPublicKey();
        } else {
            destKeyMaterial = destinationKey.getKeyMaterial();
        }

        byte[] newCiphertext = cryptoService.encryptData(
                plaintext,
                destKeyMaterial,
                destinationKey.getKeySpec(),
                request.getDestinationEncryptionContext()
        );

        return ReEncryptResponse.builder()
                .ciphertextBlob(Base64.getEncoder().encodeToString(newCiphertext))
                .sourceKeyId(request.getSourceKeyId())
                .destinationKeyId(request.getDestinationKeyId())
                .destinationKeyVersionId(destinationKey.getCurrentVersionId())
                .build();
    }
}