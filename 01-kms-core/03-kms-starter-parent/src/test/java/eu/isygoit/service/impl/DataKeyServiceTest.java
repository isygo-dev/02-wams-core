package eu.isygoit.service.impl;

import eu.isygoit.dto.KmsDtos.GenerateDataKeyPairRequest;
import eu.isygoit.dto.KmsDtos.GenerateDataKeyRequest;
import eu.isygoit.dto.KmsDtos.GenerateDataKeyResponse;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.enums.IEnumKeyUsage;
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
class DataKeyServiceTest {

    private static final String TENANT = "tenant-1";
    private static final String KEY_ID = "key-1";

    @Mock
    private KmsKeyRepository kmsKeyRepository;

    @Mock
    private ICryptoService cryptoService;

    @InjectMocks
    private DataKeyService service;

    private KmsKey kmsKey;

    @BeforeEach
    void setUp() {
        kmsKey = new KmsKey();
        kmsKey.setKeyId(KEY_ID);
        kmsKey.setTenant(TENANT);
        kmsKey.setKeyStatus(IEnumKeyStatus.Types.ENABLED);
        kmsKey.setKeyUsage(IEnumKeyUsage.Types.ENCRYPT_DECRYPT);
        kmsKey.setKeyMaterial(new byte[]{1, 2, 3});
    }

    @Test
    void shouldGenerateDataKeySuccessfully() {
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID)).thenReturn(Optional.of(kmsKey));
        when(cryptoService.generateDataKey(any(), any())).thenReturn(Map.of(
                "plaintextKey", "plain".getBytes(),
                "encryptedKey", "enc".getBytes()
        ));

        GenerateDataKeyRequest req = GenerateDataKeyRequest.builder()
                .keyId(KEY_ID)
                .keySize(32)
                .build();

        GenerateDataKeyResponse resp = service.generateDataKey(TENANT, req);
        assertNotNull(resp);
        assertEquals(KEY_ID, resp.getKeyId());
        assertEquals(Base64.getEncoder().encodeToString("plain".getBytes()), resp.getPlaintext());
        assertEquals(Base64.getEncoder().encodeToString("enc".getBytes()), resp.getCiphertextBlob());
    }

    @Test
    void shouldThrowWhenKeyNotFound() {
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID)).thenReturn(Optional.empty());
        GenerateDataKeyRequest req = GenerateDataKeyRequest.builder().keyId(KEY_ID).keySize(16).build();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.generateDataKey(TENANT, req));
        assertEquals("KMS Key not found", ex.getMessage());
    }

    @Test
    void shouldThrowWhenKeyDisabled() {
        kmsKey.setKeyStatus(IEnumKeyStatus.Types.DISABLED);
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID)).thenReturn(Optional.of(kmsKey));
        GenerateDataKeyRequest req = GenerateDataKeyRequest.builder().keyId(KEY_ID).keySize(16).build();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.generateDataKey(TENANT, req));
        assertEquals("KMS Key is not enabled", ex.getMessage());
    }

    @Test
    void shouldGenerateDataKeyPairSuccessfully() {
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID)).thenReturn(Optional.of(kmsKey));
        when(cryptoService.encryptData(any(), any(), any(), any(), any())).thenReturn("encPriv".getBytes());

        GenerateDataKeyPairRequest req = GenerateDataKeyPairRequest.builder()
                .keyId(KEY_ID)
                .keyPairSpec(IEnumKeySpec.Types.RSA_2048)
                .build();

        var resp = service.generateDataKeyPair(TENANT, req);
        assertNotNull(resp);
        assertEquals(KEY_ID, resp.getKeyId());
        assertNotNull(resp.getPrivateKeyCiphertextBlob());
    }
}
