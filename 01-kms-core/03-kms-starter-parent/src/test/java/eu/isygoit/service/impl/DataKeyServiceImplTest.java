package eu.isygoit.service.impl;

import eu.isygoit.dto.request.GenerateDataKeyPairRequestDto;
import eu.isygoit.dto.request.GenerateDataKeyRequestDto;
import eu.isygoit.dto.response.DataKeyPairResponseDto;
import eu.isygoit.dto.response.DataKeyResponseDto;
import eu.isygoit.enums.IEnumKeyPurpose;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.model.KmsKey;
import eu.isygoit.repository.KmsKeyRepository;
import eu.isygoit.service.ICryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DataKeyServiceImplTest {

    private final String tenant = "test-tenant" ;
    private final Long keyId = 1L;
    @Mock
    private KmsKeyRepository kmsKeyRepository;
    @Mock
    private ICryptoService cryptoService;
    @InjectMocks
    private DataKeyServiceImpl dataKeyService;
    private KmsKey testKey;

    @BeforeEach
    void setUp() {
        testKey = KmsKey.builder()
                .keyId(keyId)
                .tenant(tenant)
                .status(IEnumKeyStatus.Types.ENABLED)
                .keyPurpose(IEnumKeyPurpose.Types.ENCRYPT_DECRYPT)
                .keyMaterial(new byte[]{1, 2, 3})
                .keyArn("arn:wams:kms:us-east-1:123456789012:key/123")
                .currentVersionId("v1")
                .build();
    }

    @Test
    void testGenerateDataKey_Success() {
        GenerateDataKeyRequestDto request = GenerateDataKeyRequestDto.builder()
                .keyId(keyId)
                .keySize(256)
                .build();

        byte[] plaintextKey = new byte[32];
        byte[] encryptedKey = new byte[48];

        when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(testKey));
        when(cryptoService.generateDataKey(testKey.getKeyMaterial(), 256)).thenReturn(Map.of(
                "plaintextKey", plaintextKey,
                "encryptedKey", encryptedKey
        ));

        DataKeyResponseDto response = dataKeyService.generateDataKey(tenant, request);

        assertNotNull(response);
        assertEquals(Base64.getEncoder().encodeToString(plaintextKey), response.getPlaintextKey());
        assertEquals(Base64.getEncoder().encodeToString(encryptedKey), response.getEncryptedKey());
        assertEquals(keyId, response.getKeyId());
    }

    @Test
    void testGenerateDataKeyWithoutPlaintext_Success() {
        GenerateDataKeyRequestDto request = GenerateDataKeyRequestDto.builder()
                .keyId(keyId)
                .keySize(256)
                .build();

        byte[] plaintextKey = new byte[32];
        byte[] encryptedKey = new byte[48];

        when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(testKey));
        when(cryptoService.generateDataKey(testKey.getKeyMaterial(), 256)).thenReturn(Map.of(
                "plaintextKey", plaintextKey,
                "encryptedKey", encryptedKey
        ));

        DataKeyResponseDto response = dataKeyService.generateDataKeyWithoutPlaintext(tenant, request);

        assertNotNull(response);
        assertNull(response.getPlaintextKey());
        assertEquals(Base64.getEncoder().encodeToString(encryptedKey), response.getEncryptedKey());
    }

    @Test
    void testGenerateDataKeyPair_RSA_Success() {
        GenerateDataKeyPairRequestDto request = GenerateDataKeyPairRequestDto.builder()
                .keyId(keyId)
                .keySpec("RSA_2048")
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(testKey));
        when(cryptoService.encryptData(any(), any(), any())).thenReturn("encrypted".getBytes());

        DataKeyPairResponseDto response = dataKeyService.generateDataKeyPair(tenant, request);

        assertNotNull(response);
        assertNotNull(response.getPublicKey());
        assertNotNull(response.getPrivateKey());
        assertNotNull(response.getEncryptedPublicKey());
        assertNotNull(response.getEncryptedPrivateKey());
        assertEquals(keyId, response.getKeyId());
    }

    @Test
    void testGenerateDataKeyPair_EC_Success() {
        GenerateDataKeyPairRequestDto request = GenerateDataKeyPairRequestDto.builder()
                .keyId(keyId)
                .keySpec("ECC_NIST_P256")
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(testKey));
        when(cryptoService.encryptData(any(), any(), any())).thenReturn("encrypted".getBytes());

        DataKeyPairResponseDto response = dataKeyService.generateDataKeyPair(tenant, request);

        assertNotNull(response);
        assertNotNull(response.getPublicKey());
        assertNotNull(response.getPrivateKey());
        assertEquals(keyId, response.getKeyId());
    }
}
