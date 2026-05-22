package eu.isygoit.service.impl;

import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.enums.IEnumKeyUsage;
import eu.isygoit.model.KmsKey;
import eu.isygoit.model.KmsKeyVersion;
import eu.isygoit.repository.KmsAliasRepository;
import eu.isygoit.repository.KmsKeyRepository;
import eu.isygoit.repository.KmsKeyVersionRepository;
import eu.isygoit.service.ICryptoService;
import eu.isygoit.service.IEncryptionService;
import eu.isygoit.utils.CiphertextEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EncryptionService implements IEncryptionService {

    private final KmsKeyRepository kmsKeyRepository;
    private final KmsKeyVersionRepository kmsKeyVersionRepository; // new
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

        // Get the CURRENT version's key material
        String versionId = kmsKey.getCurrentVersionId();
        KmsKeyVersion version = kmsKeyVersionRepository
                .findByTenantAndKeyIdAndVersionId(tenant, kmsKey.getKeyId(), versionId)
                .orElseThrow(() -> new RuntimeException("Current version not found: " + versionId));

        byte[] keyMaterial;
        if (kmsKey.getKeySpec() != null && kmsKey.getKeySpec().isAsymmetric()) {
            if (version.getPublicKey() == null)
                throw new RuntimeException("Asymmetric key has no public key");
            keyMaterial = version.getPublicKey(); // use public key from version
        } else {
            keyMaterial = version.getKeyMaterial();
        }

        byte[] ciphertext = cryptoService.encryptData(
                plaintext,
                keyMaterial,
                kmsKey.getKeySpec(),
                request.getEncryptionAlgorithmSpec(),
                request.getEncryptionContext()
        );

        // Wrap ciphertext with version ID
        byte[] wrappedCiphertext = CiphertextEnvelope.wrap(versionId, ciphertext);

        return EncryptResponse.builder()
                .ciphertextBlob(Base64.getEncoder().encodeToString(wrappedCiphertext))
                .keyId(kmsKey.getKeyId())
                .keyVersionId(versionId)
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

        byte[] wrappedCiphertext = Base64.getDecoder().decode(request.getCiphertextBlob());

        // Extract version ID from envelope
        String versionId = CiphertextEnvelope.unwrapVersionId(wrappedCiphertext);
        byte[] ciphertext = CiphertextEnvelope.unwrapCiphertext(wrappedCiphertext);

        KmsKeyVersion version = null;
        if (versionId != null) {
            version = kmsKeyVersionRepository
                    .findByTenantAndKeyIdAndVersionId(tenant, keyId, versionId)
                    .orElse(null);
        }

        // Fallback: if version not found (or missing), try current version
        if (version == null) {
            version = kmsKeyVersionRepository
                    .findByTenantAndKeyIdAndVersionId(tenant, keyId, kmsKey.getCurrentVersionId())
                    .orElse(null);
        }

        // If still not found, try all versions (desc order) – optional but safe
        if (version == null) {
            List<KmsKeyVersion> allVersions = kmsKeyVersionRepository
                    .findByTenantAndKeyIdOrderByCreateDateDesc(tenant, keyId);
            for (KmsKeyVersion v : allVersions) {
                try {
                    byte[] plain = tryDecryptWithVersion(v, ciphertext, kmsKey, request);
                    return DecryptResponse.builder()
                            .plaintext(Base64.getEncoder().encodeToString(plain))
                            .keyId(kmsKey.getKeyId())
                            .keyVersionId(v.getVersionId())
                            .build();
                } catch (Exception ignored) {
                }
            }
            throw new RuntimeException("No key version could decrypt the data");
        }

        // Decrypt with the resolved version
        byte[] plaintext = decryptWithVersion(version, ciphertext, kmsKey, request);

        return DecryptResponse.builder()
                .plaintext(Base64.getEncoder().encodeToString(plaintext))
                .keyId(kmsKey.getKeyId())
                .keyVersionId(version.getVersionId())
                .build();
    }

    private byte[] decryptWithVersion(KmsKeyVersion version, byte[] ciphertext,
                                      KmsKey kmsKey, DecryptRequest request) {
        byte[] keyMaterial;
        if (kmsKey.getKeySpec() != null && kmsKey.getKeySpec().isAsymmetric()) {
            // Use private key from version
            keyMaterial = version.getKeyMaterial(); // private key in PKCS#8
        } else {
            keyMaterial = version.getKeyMaterial();
        }
        return cryptoService.decryptData(
                kmsKey.getTenant(),
                ciphertext,
                keyMaterial,
                kmsKey.getKeySpec(),
                request.getEncryptionAlgorithmSpec(),
                request.getEncryptionContext()
        );
    }

    private byte[] tryDecryptWithVersion(KmsKeyVersion version, byte[] ciphertext,
                                         KmsKey kmsKey, DecryptRequest request) {
        return decryptWithVersion(version, ciphertext, kmsKey, request);
    }

    @Override
    public ReEncryptResponse reEncrypt(String tenant, ReEncryptRequest request) {
        // Similar pattern: decrypt with source version, encrypt with current version
        throw new UnsupportedOperationException("reEncrypt not yet implemented");
    }
}