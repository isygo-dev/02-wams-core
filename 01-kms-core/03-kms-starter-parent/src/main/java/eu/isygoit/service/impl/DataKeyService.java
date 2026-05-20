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
        log.info("Generate data key for tenant {} keyId {}", tenant, request.getKeyId());

        KmsKey kmsKey = kmsKeyRepository.findByTenantAndKeyId(tenant, request.getKeyId())
                .orElseThrow(() -> new RuntimeException("KMS Key not found"));
        if (!kmsKey.isEnabled()) throw new RuntimeException("Key disabled");
        if (kmsKey.getKeyUsage() != IEnumKeyUsage.Types.ENCRYPT_DECRYPT)
            throw new RuntimeException("Key not allowed for data key generation");

        int keySize = (request.getKeySize() != null) ? request.getKeySize() : 256;
        byte[] plaintextKey = generateAesKey(keySize);

        // Use public key if asymmetric KEK
        byte[] kekMaterial;
        if (kmsKey.getKeySpec() != null && kmsKey.getKeySpec().isAsymmetric()) {
            if (kmsKey.getPublicKey() == null)
                throw new RuntimeException("Asymmetric KEK has no public key");
            kekMaterial = kmsKey.getPublicKey();
        } else {
            kekMaterial = kmsKey.getKeyMaterial();
        }

        byte[] encryptedKey = cryptoService.encryptData(
                plaintextKey,
                kekMaterial,
                kmsKey.getKeySpec(),
                request.getEncryptionAlgorithmSpec(),
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
        if (keyIdOrAlias == null || keyIdOrAlias.isBlank())
            throw new InvalidParameterException("KeyId required");
        if (keyIdOrAlias.startsWith("wrn:")) {
            String[] parts = keyIdOrAlias.split(":");
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
        log.info("Generate data key pair for tenant {} keyId {}", tenant, request.getKeyId());

        KmsKey kmsKey = kmsKeyRepository.findByTenantAndKeyId(tenant, request.getKeyId())
                .orElseThrow(() -> new RuntimeException("KMS Key not found"));
        if (!kmsKey.isEnabled()) throw new RuntimeException("Key disabled");

        try {
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
            byte[] publicKey = pair.getPublic().getEncoded();
            byte[] privateKey = pair.getPrivate().getEncoded();

            // Wrap the ephemeral private key with the KEK (public key if asymmetric)
            byte[] kekMaterial;
            if (kmsKey.getKeySpec() != null && kmsKey.getKeySpec().isAsymmetric()) {
                if (kmsKey.getPublicKey() == null)
                    throw new RuntimeException("Asymmetric KEK has no public key");
                kekMaterial = kmsKey.getPublicKey();
            } else {
                kekMaterial = kmsKey.getKeyMaterial();
            }

            byte[] encryptedPrivateKey = cryptoService.encryptData(
                    privateKey,
                    kekMaterial,
                    kmsKey.getKeySpec(),
                    request.getEncryptionAlgorithmSpec(),
                    request.getEncryptionContext()
            );

            return GenerateDataKeyPairResponse.builder()
                    .publicKey(Base64.getEncoder().encodeToString(publicKey))
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
        byte[] random = new byte[request.getNumberOfBytes()];
        new SecureRandom().nextBytes(random);
        return GenerateRandomResponse.builder()
                .plaintext(Base64.getEncoder().encodeToString(random))
                .build();
    }

    private GenerateDataKeyPairRequest mapToBaseRequest(GenerateDataKeyPairWithoutPlaintextRequest request) {
        return GenerateDataKeyPairRequest.builder()
                .keyId(request.getKeyId())
                .keyPairSpec(request.getKeyPairSpec())
                .encryptionContext(request.getEncryptionContext())
                .encryptionAlgorithmSpec(request.getEncryptionAlgorithmSpec())
                .build();
    }

    private GenerateDataKeyRequest toBaseRequest(GenerateDataKeyWithoutPlaintextRequest request) {
        return GenerateDataKeyRequest.builder()
                .keyId(request.getKeyId())
                .keySize(request.getKeySize())
                .encryptionContext(request.getEncryptionContext())
                .encryptionAlgorithmSpec(request.getEncryptionAlgorithmSpec())
                .build();
    }

    private byte[] generateAesKey(int bits) {
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(bits);
            return kg.generateKey().getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate AES key", e);
        }
    }
}