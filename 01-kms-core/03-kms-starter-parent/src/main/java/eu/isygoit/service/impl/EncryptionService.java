package eu.isygoit.service.impl;

import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.enums.IEnumKeyUsage;
import eu.isygoit.exception.*;
import eu.isygoit.model.KmsKey;
import eu.isygoit.model.KmsKeyVersion;
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

    @Override
    public EncryptResponse encrypt(String tenant, EncryptRequest request) {
        log.info("Encrypt for tenant {} keyId {}", tenant, request.getKeyId());

        KmsKey kmsKey = kmsKeyRepository.findByTenantAndKeyId(tenant, request.getKeyId())
                .orElseThrow(() -> new RuntimeException("KMS Key not found"));

        if (!kmsKey.isEnabled()) {
            throw new DisabledKeyException("KMS Key is not enabled");
        }

        if (kmsKey.getKeyUsage() != IEnumKeyUsage.Types.ENCRYPT_DECRYPT) {
            throw new KeyNotAllowedForUsageException("KMS Key is not authorized for encryption");
        }

        if (request.getEncryptionAlgorithmSpec() == null) {
            throw new WrongAlgorithmException("Encryption algorithm is required");
        }

        try {
            byte[] plaintext = Base64.getDecoder().decode(request.getPlaintext());

            KmsKeyVersion version = kmsKeyVersionRepository
                    .findByTenantAndKeyIdAndVersionId(tenant, kmsKey.getKeyId(), kmsKey.getCurrentVersionId())
                    .orElseGet(() -> {
                        log.warn("Current version {} not found for key {}, falling back to last active version",
                                kmsKey.getCurrentVersionId(), kmsKey.getKeyId());
                        KmsKeyVersion activeVersion = kmsKeyVersionRepository
                                .findFirstByTenantAndKeyIdAndKeyStatusOrderByCreateDateDesc(tenant, kmsKey.getKeyId(), IEnumKeyStatus.Types.ENABLED)
                                .orElseThrow(() -> new NoActiveVersionException(kmsKey.getKeyId()));
                        log.info("Using last active version {} for key {}", activeVersion.getVersionId(), kmsKey.getKeyId());
                        return activeVersion;
                    });

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
            byte[] wrappedCiphertext = CiphertextEnvelope.wrap(version.getVersionId(), ciphertext);

            return EncryptResponse.builder()
                    .ciphertextBlob(Base64.getEncoder().encodeToString(wrappedCiphertext))
                    .keyId(kmsKey.getKeyId())
                    .keyVersionId(version.getVersionId())
                    .build();
        } catch (Exception e) {
            log.error("Encrypt failed", e);
            throw new EncryptionException("Encrypt failed", e);
        }
    }

    @Override
    public DecryptResponse decrypt(String tenant, DecryptRequest request) {
        log.info("Decrypt for tenant {}", tenant);
        String keyId = request.getKeyId();
        if (keyId == null) throw new RuntimeException("keyId required");

        KmsKey kmsKey = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new RuntimeException("KMS Key not found"));

        if (!kmsKey.isEnabled()) {
            throw new DisabledKeyException("KMS Key is not enabled");
        }

        if (kmsKey.getKeyUsage() != IEnumKeyUsage.Types.ENCRYPT_DECRYPT) {
            throw new KeyNotAllowedForUsageException("KMS Key is not authorized for decryption");
        }

        if (request.getEncryptionAlgorithmSpec() == null) {
            throw new WrongAlgorithmException("Decryption algorithm is required");
        }

        try {
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

            // Fallback: if version not found (or disabled), try current version
            if (version == null || IEnumKeyStatus.Types.ENABLED != version.getKeyStatus()) {
                version = kmsKeyVersionRepository
                        .findByTenantAndKeyIdAndVersionId(tenant, keyId, kmsKey.getCurrentVersionId())
                        .orElse(null);
            }

            // If still not found or disabled, try all enabled versions (desc order) – optional but safe
            if (version == null || IEnumKeyStatus.Types.ENABLED != version.getKeyStatus()) {
                List<KmsKeyVersion> allVersions = kmsKeyVersionRepository
                        .findByTenantAndKeyIdAndKeyStatusOrderByCreateDateDesc(tenant, keyId, IEnumKeyStatus.Types.ENABLED);
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
        } catch (Exception e) {
            log.error("Decrypt failed", e);
            throw new DecryptionException("Decrypt failed", e);
        }
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