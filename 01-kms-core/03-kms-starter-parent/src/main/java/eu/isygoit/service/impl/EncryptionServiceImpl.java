package eu.isygoit.service.impl;

import eu.isygoit.dto.request.DecryptRequestDto;
import eu.isygoit.dto.request.EncryptRequestDto;
import eu.isygoit.dto.request.ReEncryptRequestDto;
import eu.isygoit.dto.response.DecryptResponseDto;
import eu.isygoit.dto.response.EncryptResponseDto;
import eu.isygoit.dto.response.ReEncryptResponseDto;
import eu.isygoit.enums.IEnumKeyPurpose;
import eu.isygoit.model.KmsKey;
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
public class EncryptionServiceImpl implements IEncryptionService {

    private final KmsKeyRepository kmsKeyRepository;
    private final ICryptoService cryptoService;

    @Override
    public EncryptResponseDto encrypt(String tenant, EncryptRequestDto request) {
        log.info("Encrypting data for tenant: {} with keyId: {}", tenant, request.getKeyId());

        KmsKey kmsKey = kmsKeyRepository.findByTenantAndKeyId(tenant, request.getKeyId())
                .orElseThrow(() -> new RuntimeException("KMS Key not found"));

        if (!kmsKey.isEnabled()) {
            throw new RuntimeException("KMS Key is not enabled");
        }

        if (kmsKey.getKeyPurpose() != IEnumKeyPurpose.Types.ENCRYPT_DECRYPT) {
            throw new RuntimeException("KMS Key is not authorized for encryption");
        }

        byte[] plaintext = Base64.getDecoder().decode(request.getPlaintext());
        byte[] ciphertext = cryptoService.encryptData(plaintext, kmsKey.getKeyMaterial(), request.getEncryptionContext());

        return EncryptResponseDto.builder()
                .ciphertext(Base64.getEncoder().encodeToString(ciphertext))
                .keyId(kmsKey.getKeyId())
                .keyVersion(kmsKey.getCurrentVersionId())
                .build();
    }

    @Override
    public DecryptResponseDto decrypt(String tenant, DecryptRequestDto request) {
        log.info("Decrypting data for tenant: {}", tenant);

        Long keyId = request.getKeyId();
        // In AWS KMS, if keyId is not provided, it should be inferable from the ciphertext
        // For our implementation, we'll assume it's provided for now or we could store it in the blob.
        if (keyId == null) {
            throw new RuntimeException("keyId is required for decryption in this implementation");
        }

        KmsKey kmsKey = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new RuntimeException("KMS Key not found"));

        if (!kmsKey.isEnabled()) {
            throw new RuntimeException("KMS Key is not enabled");
        }

        byte[] ciphertext = Base64.getDecoder().decode(request.getCiphertext());
        byte[] plaintext = cryptoService.decryptData(tenant, ciphertext, kmsKey.getKeyMaterial(), request.getEncryptionContext());

        return DecryptResponseDto.builder()
                .plaintext(Base64.getEncoder().encodeToString(plaintext))
                .keyId(kmsKey.getKeyId())
                .keyVersion(kmsKey.getCurrentVersionId())
                .build();
    }

    @Override
    public ReEncryptResponseDto reEncrypt(String tenant, ReEncryptRequestDto request) {
        log.info("Re-encrypting data for tenant: {} to destination key: {}",
                tenant, request.getDestinationKeyId());

        // Decrypt with source key (if provided or implicit)
        DecryptResponseDto decryptResponse = decrypt(tenant, DecryptRequestDto.builder()
                .keyId(request.getSourceKeyId())
                .ciphertext(request.getCiphertextBlob())
                .encryptionContext(request.getSourceEncryptionContext())
                .build());

        // Encrypt with destination key
        EncryptResponseDto encryptResponse = encrypt(tenant, EncryptRequestDto.builder()
                .keyId(request.getDestinationKeyId())
                .plaintext(decryptResponse.getPlaintext())
                .encryptionContext(request.getDestinationEncryptionContext())
                .build());

        return ReEncryptResponseDto.builder()
                .ciphertext(encryptResponse.getCiphertext())
                .sourceKeyId(request.getSourceKeyId())
                .destinationKeyId(request.getDestinationKeyId())
                .destinationKeyVersion(encryptResponse.getKeyVersion())
                .build();
    }
}

