package eu.isygoit.service.impl;

import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.enums.IEnumKeyUsage;
import eu.isygoit.exception.AliasNotFoundException;
import eu.isygoit.model.KmsAlias;
import eu.isygoit.model.KmsKey;
import eu.isygoit.repository.KmsAliasRepository;
import eu.isygoit.repository.KmsKeyRepository;
import eu.isygoit.service.ICryptoService;
import eu.isygoit.service.IDataKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.KeyGenerator;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

/**
 * Data key service - supports hybrid encryption for RSA KEKs.
 * Fixed: Uses public key for wrapping data keys when KEK is asymmetric.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DataKeyService implements IDataKeyService {

    private final KmsAliasRepository kmsAliasRepository;
    private final KmsKeyRepository kmsKeyRepository;
    private final ICryptoService cryptoService;

    @Override
    public GenerateDataKeyResponse generateDataKey(String tenant, GenerateDataKeyRequest request) {
        log.info("Generating data key for tenant: {} keyId: {}", tenant, request.getKeyId());

        KmsKey kmsKey = kmsKeyRepository.findByTenantAndKeyId(tenant, request.getKeyId())
                .orElseThrow(() -> new RuntimeException("KMS Key not found"));

        if (!kmsKey.isEnabled()) {
            throw new RuntimeException("KMS Key is not enabled");
        }
        if (kmsKey.getKeyUsage() != IEnumKeyUsage.Types.ENCRYPT_DECRYPT) {
            throw new RuntimeException("KMS Key is not authorized for data key generation");
        }

        // 1. Generate a plaintext AES data key
        int keySizeBits = (request.getKeySize() != null) ? request.getKeySize() : 256;
        byte[] plaintextKey = generateAesKey(keySizeBits);

        // 2. Encrypt the data key using the KEK
        byte[] kekMaterial;
        if (kmsKey.getKeySpec() != null && kmsKey.getKeySpec().isAsymmetric()) {
            if (kmsKey.getPublicKey() == null) {
                throw new RuntimeException("Asymmetric KEK has no public key for wrapping");
            }
            kekMaterial = kmsKey.getPublicKey();   // use public key for wrapping
        } else {
            kekMaterial = kmsKey.getKeyMaterial(); // symmetric key
        }

        byte[] encryptedKey = cryptoService.encryptData(
                plaintextKey,
                kekMaterial,
                kmsKey.getKeySpec(),
                request.getEncryptionContext()
        );

        return GenerateDataKeyResponse.builder()
                .plaintext(Base64.getEncoder().encodeToString(plaintextKey))
                .ciphertextBlob(Base64.getEncoder().encodeToString(encryptedKey))
                .keyId(kmsKey.getKeyId())
                .build();
    }

    @Override
    public String resolveKeyId(String tenant, String keyIdOrAlias) {
        if (keyIdOrAlias == null || keyIdOrAlias.isBlank()) {
            throw new InvalidParameterException("KeyId is required");
        }
        if (keyIdOrAlias.startsWith("wrn:")) {
            String[] parts = keyIdOrAlias.split(":");
            if (parts.length < 2) {
                throw new InvalidParameterException("Invalid ARN format");
            }
            return parts[parts.length - 1];
        }
        if (keyIdOrAlias.startsWith("alias:")) {
            String aliasName = keyIdOrAlias.substring("alias:".length());
            return kmsAliasRepository.findByTenantAndAliasName(tenant, aliasName)
                    .map(KmsAlias::getTargetKeyId)
                    .orElseThrow(() -> new AliasNotFoundException("Alias not found: " + keyIdOrAlias));
        }
        return keyIdOrAlias;
    }

    @Override
    public GenerateDataKeyWithoutPlaintextResponse generateDataKeyWithoutPlaintext(
            String tenant, GenerateDataKeyWithoutPlaintextRequest request) {
        GenerateDataKeyResponse base = generateDataKey(tenant, toBaseRequest(request));
        return GenerateDataKeyWithoutPlaintextResponse.builder()
                .ciphertextBlob(base.getCiphertextBlob())
                .keyId(base.getKeyId())
                .build();
    }

    @Override
    public GenerateDataKeyPairResponse generateDataKeyPair(String tenant, GenerateDataKeyPairRequest request) {
        log.info("Generating data key pair for tenant: {} keyId: {}", tenant, request.getKeyId());

        KmsKey kmsKey = kmsKeyRepository.findByTenantAndKeyId(tenant, request.getKeyId())
                .orElseThrow(() -> new RuntimeException("KMS Key not found"));

        if (!kmsKey.isEnabled()) {
            throw new RuntimeException("KMS Key is not enabled");
        }

        try {
            // 1. Generate ephemeral key pair (RSA or EC)
            String algorithm = request.getKeyPairSpec().name().startsWith("RSA") ? "RSA" : "ECC";
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm);

            if ("RSA".equals(algorithm)) {
                int size = 2048;
                if (request.getKeyPairSpec().name().contains("3072")) size = 3072;
                else if (request.getKeyPairSpec().name().contains("4096")) size = 4096;
                keyGen.initialize(size);
            } else {
                String curve = "secp256r1";
                if (request.getKeyPairSpec().name().contains("P384")) curve = "secp384r1";
                else if (request.getKeyPairSpec().name().contains("P521")) curve = "secp521r1";
                keyGen.initialize(new ECGenParameterSpec(curve));
            }

            KeyPair pair = keyGen.generateKeyPair();
            byte[] publicKeyBytes = pair.getPublic().getEncoded();
            byte[] privateKeyBytes = pair.getPrivate().getEncoded();

            // 2. Encrypt the ephemeral private key using the KEK (public key if asymmetric)
            byte[] kekMaterial;
            if (kmsKey.getKeySpec() != null && kmsKey.getKeySpec().isAsymmetric()) {
                if (kmsKey.getPublicKey() == null) {
                    throw new RuntimeException("Asymmetric KEK has no public key");
                }
                kekMaterial = kmsKey.getPublicKey();
            } else {
                kekMaterial = kmsKey.getKeyMaterial();
            }

            byte[] encryptedPrivateKey = cryptoService.encryptData(
                    privateKeyBytes,
                    kekMaterial,
                    kmsKey.getKeySpec(),
                    request.getEncryptionContext()
            );

            return GenerateDataKeyPairResponse.builder()
                    .publicKey(Base64.getEncoder().encodeToString(publicKeyBytes))
                    .privateKeyCiphertextBlob(Base64.getEncoder().encodeToString(encryptedPrivateKey))
                    .keyId(kmsKey.getKeyId())
                    .keyPairSpec(request.getKeyPairSpec())
                    .encryptionAlgorithmSpec(algorithm)
                    .keyVersionId(kmsKey.getCurrentVersionId())
                    .build();

        } catch (Exception e) {
            log.error("Data key pair generation failed", e);
            throw new RuntimeException("Data key pair generation failed", e);
        }
    }

    @Override
    public GenerateDataKeyPairWithoutPlaintextResponse generateDataKeyPairWithoutPlaintext(
            String tenant, GenerateDataKeyPairWithoutPlaintextRequest request) {
        GenerateDataKeyPairResponse base = generateDataKeyPair(tenant, mapToBaseRequest(request));
        return GenerateDataKeyPairWithoutPlaintextResponse.builder()
                .publicKey(base.getPublicKey())
                .privateKeyCiphertextBlob(base.getPrivateKeyCiphertextBlob())
                .keyId(base.getKeyId())
                .keyVersionId(base.getKeyVersionId())
                .keyPairSpec(base.getKeyPairSpec())
                .encryptionAlgorithmSpec(base.getEncryptionAlgorithmSpec())
                .build();
    }

    @Override
    public GenerateRandomResponse generateRandom(GenerateRandomRequest request) {
        byte[] randomBytes = new byte[request.getNumberOfBytes()];
        new SecureRandom().nextBytes(randomBytes);
        String plaintext = Base64.getEncoder().encodeToString(randomBytes);
        return GenerateRandomResponse.builder().plaintext(plaintext).build();
    }

    private GenerateDataKeyPairRequest mapToBaseRequest(GenerateDataKeyPairWithoutPlaintextRequest request) {
        return GenerateDataKeyPairRequest.builder()
                .keyId(request.getKeyId())
                .keyPairSpec(request.getKeyPairSpec())
                .encryptionContext(request.getEncryptionContext())
                .build();
    }

    private GenerateDataKeyRequest toBaseRequest(GenerateDataKeyWithoutPlaintextRequest request) {
        return GenerateDataKeyRequest.builder()
                .keyId(request.getKeyId())
                .keySize(request.getKeySize())
                .encryptionContext(request.getEncryptionContext())
                .build();
    }

    private byte[] generateAesKey(int keySizeBits) {
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(keySizeBits);
            return kg.generateKey().getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate AES data key", e);
        }
    }
}