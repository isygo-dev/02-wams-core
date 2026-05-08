package eu.isygoit.service.impl;

import eu.isygoit.dto.request.GenerateDataKeyPairRequestDto;
import eu.isygoit.dto.request.GenerateDataKeyRequestDto;
import eu.isygoit.dto.response.DataKeyPairResponseDto;
import eu.isygoit.dto.response.DataKeyResponseDto;
import eu.isygoit.enums.IEnumKeyPurpose;
import eu.isygoit.model.KmsKey;
import eu.isygoit.repository.KmsKeyRepository;
import eu.isygoit.service.ICryptoService;
import eu.isygoit.service.IDataKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.Map;

/**
 * The type Data key service.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DataKeyServiceImpl implements IDataKeyService {

    private final KmsKeyRepository kmsKeyRepository;
    private final ICryptoService cryptoService;

    @Override
    public DataKeyResponseDto generateDataKey(String tenant, GenerateDataKeyRequestDto request) {
        log.info("Generating data key for tenant: {} keyId: {} keySize: {}",
                tenant, request.getKeyId(), request.getKeySize());

        KmsKey kmsKey = kmsKeyRepository.findByTenantAndKeyId(tenant, request.getKeyId())
                .orElseThrow(() -> new RuntimeException("KMS Key not found"));

        if (!kmsKey.isEnabled()) {
            throw new RuntimeException("KMS Key is not enabled");
        }

        if (kmsKey.getKeyPurpose() != IEnumKeyPurpose.Types.ENCRYPT_DECRYPT) {
            throw new RuntimeException("KMS Key is not authorized for data key generation");
        }

        Map<String, byte[]> dataKeyMap = cryptoService.generateDataKey(kmsKey.getKeyMaterial(), request.getKeySize());

        return DataKeyResponseDto.builder()
                .plaintextKey(Base64.getEncoder().encodeToString(dataKeyMap.get("plaintextKey")))
                .encryptedKey(Base64.getEncoder().encodeToString(dataKeyMap.get("encryptedKey")))
                .keyId(kmsKey.getKeyId())
                .build();
    }

    @Override
    public DataKeyResponseDto generateDataKeyWithoutPlaintext(String tenant, GenerateDataKeyRequestDto request) {
        DataKeyResponseDto response = generateDataKey(tenant, request);
        response.setPlaintextKey(null);
        return response;
    }

    @Override
    public DataKeyPairResponseDto generateDataKeyPair(String tenant, GenerateDataKeyPairRequestDto request) {
        log.info("Generating data key pair for tenant: {} keyId: {}", tenant, request.getKeyId());

        KmsKey kmsKey = kmsKeyRepository.findByTenantAndKeyId(tenant, request.getKeyId())
                .orElseThrow(() -> new RuntimeException("KMS Key not found"));

        if (!kmsKey.isEnabled()) {
            throw new RuntimeException("KMS Key is not enabled");
        }

        // AWS KMS logic for generating asymmetric data key pair
        try {
            String keyAlgo = request.getKeySpec().startsWith("RSA") ? "RSA" : "EC" ;
            java.security.KeyPairGenerator keyGen = java.security.KeyPairGenerator.getInstance(keyAlgo);

            if (keyAlgo.equals("RSA")) {
                int size = 2048;
                if (request.getKeySpec().contains("3072")) size = 3072;
                else if (request.getKeySpec().contains("4096")) size = 4096;
                keyGen.initialize(size);
            } else {
                // EC
                String curve = "secp256r1" ; // default for P256
                if (request.getKeySpec().contains("P384")) curve = "secp384r1" ;
                else if (request.getKeySpec().contains("P521")) curve = "secp521r1" ;
                keyGen.initialize(new java.security.spec.ECGenParameterSpec(curve));
            }

            java.security.KeyPair keyPair = keyGen.generateKeyPair();
            byte[] publicKey = keyPair.getPublic().getEncoded();
            byte[] privateKey = keyPair.getPrivate().getEncoded();

            byte[] encryptedPublicKey = cryptoService.encryptData(publicKey, kmsKey.getKeyMaterial(), request.getEncryptionContext());
            byte[] encryptedPrivateKey = cryptoService.encryptData(privateKey, kmsKey.getKeyMaterial(), request.getEncryptionContext());

            return DataKeyPairResponseDto.builder()
                    .publicKey(Base64.getEncoder().encodeToString(publicKey))
                    .privateKey(Base64.getEncoder().encodeToString(privateKey))
                    .encryptedPublicKey(Base64.getEncoder().encodeToString(encryptedPublicKey))
                    .encryptedPrivateKey(Base64.getEncoder().encodeToString(encryptedPrivateKey))
                    .keyId(kmsKey.getKeyId())
                    .keyArn(kmsKey.getKeyArn())
                    .keyVersion(kmsKey.getCurrentVersionId())
                    .build();
        } catch (Exception e) {
            log.error("Data key pair generation failed", e);
            throw new RuntimeException("Data key pair generation failed", e);
        }
    }

    @Override
    public DataKeyPairResponseDto generateDataKeyPairWithoutPlaintext(String tenant, GenerateDataKeyPairRequestDto request) {
        DataKeyPairResponseDto response = generateDataKeyPair(tenant, request);
        response.setPrivateKey(null);
        response.setPublicKey(null);
        return response;
    }
}
