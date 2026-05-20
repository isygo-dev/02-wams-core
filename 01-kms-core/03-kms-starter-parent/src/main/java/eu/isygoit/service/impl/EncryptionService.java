package eu.isygoit.service.impl;

import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.enums.IEnumKeyUsage;
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
        log.info("Encrypt for tenant {} keyId {}", tenant, request.getKeyId());

        KmsKey kmsKey = kmsKeyRepository.findByTenantAndKeyId(tenant, request.getKeyId())
                .orElseThrow(() -> new RuntimeException("KMS Key not found"));
        if (!kmsKey.isEnabled()) throw new RuntimeException("Key disabled");
        if (kmsKey.getKeyUsage() != IEnumKeyUsage.Types.ENCRYPT_DECRYPT)
            throw new RuntimeException("Key not allowed for encryption");

        byte[] plaintext = Base64.getDecoder().decode(request.getPlaintext());

        // Select correct material: public key for asymmetric, symmetric key otherwise
        byte[] keyMaterial;
        if (kmsKey.getKeySpec() != null && kmsKey.getKeySpec().isAsymmetric()) {
            if (kmsKey.getPublicKey() == null)
                throw new RuntimeException("Asymmetric key has no public key");
            keyMaterial = kmsKey.getPublicKey();
        } else {
            keyMaterial = kmsKey.getKeyMaterial();
        }

        byte[] ciphertext = cryptoService.encryptData(
                plaintext,
                keyMaterial,
                kmsKey.getKeySpec(),
                request.getEncryptionAlgorithmSpec(),
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
        log.info("Decrypt for tenant {}", tenant);
        String keyId = request.getKeyId();
        if (keyId == null) throw new RuntimeException("keyId required");

        KmsKey kmsKey = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new RuntimeException("KMS Key not found"));
        if (!kmsKey.isEnabled()) throw new RuntimeException("Key disabled");

        byte[] ciphertext = Base64.getDecoder().decode(request.getCiphertextBlob());
        byte[] plaintext = cryptoService.decryptData(
                tenant,
                ciphertext,
                kmsKey.getKeyMaterial(),          // private key for asymmetric
                kmsKey.getKeySpec(),
                request.getEncryptionAlgorithmSpec(),
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
        // Similar to encrypt/decrypt; omitted for brevity – same pattern applies
        throw new UnsupportedOperationException("reEncrypt not yet implemented with new signature");
    }
}