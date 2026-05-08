package eu.isygoit.service.impl;

import eu.isygoit.dto.request.DecryptRequestDto;
import eu.isygoit.dto.request.EncryptRequestDto;
import eu.isygoit.dto.request.ReEncryptRequestDto;
import eu.isygoit.dto.response.DecryptResponseDto;
import eu.isygoit.dto.response.EncryptResponseDto;
import eu.isygoit.dto.response.ReEncryptResponseDto;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EncryptionServiceImplTest {

    @Mock
    private KmsKeyRepository kmsKeyRepository;

    @Mock
    private ICryptoService cryptoService;

    @InjectMocks
    private EncryptionServiceImpl encryptionService;

    private KmsKey testKey;
    private final String tenant = "test-tenant";
    private final Long keyId = 1L;

    @BeforeEach
    void setUp() {
        testKey = KmsKey.builder()
                .keyId(keyId)
                .tenant(tenant)
                .status(IEnumKeyStatus.Types.ENABLED)
                .keyPurpose(IEnumKeyPurpose.Types.ENCRYPT_DECRYPT)
                .keyMaterial(new byte[]{1, 2, 3})
                .currentVersionId("v1")
                .build();
    }

    @Test
    void testEncrypt_Success() {
        EncryptRequestDto request = EncryptRequestDto.builder()
                .keyId(keyId)
                .plaintext(Base64.getEncoder().encodeToString("hello".getBytes()))
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(testKey));
        when(cryptoService.encryptData(any(), any(), any())).thenReturn("encrypted".getBytes());

        EncryptResponseDto response = encryptionService.encrypt(tenant, request);

        assertNotNull(response);
        assertEquals(Base64.getEncoder().encodeToString("encrypted".getBytes()), response.getCiphertext());
        assertEquals(keyId, response.getKeyId());
        assertEquals("v1", response.getKeyVersion());
    }

    @Test
    void testEncrypt_KeyNotFound() {
        EncryptRequestDto request = EncryptRequestDto.builder().keyId(keyId).build();
        when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> encryptionService.encrypt(tenant, request));
    }

    @Test
    void testEncrypt_KeyDisabled() {
        testKey.setStatus(IEnumKeyStatus.Types.DISABLED);
        EncryptRequestDto request = EncryptRequestDto.builder().keyId(keyId).build();
        when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(testKey));

        assertThrows(RuntimeException.class, () -> encryptionService.encrypt(tenant, request));
    }

    @Test
    void testDecrypt_Success() {
        DecryptRequestDto request = DecryptRequestDto.builder()
                .keyId(keyId)
                .ciphertext(Base64.getEncoder().encodeToString("encrypted".getBytes()))
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(testKey));
        when(cryptoService.decryptData(any(), any(), any())).thenReturn("hello".getBytes());

        DecryptResponseDto response = encryptionService.decrypt(tenant, request);

        assertNotNull(response);
        assertEquals(Base64.getEncoder().encodeToString("hello".getBytes()), response.getPlaintext());
        assertEquals(keyId, response.getKeyId());
    }

    @Test
    void testReencrypt_Success() {
        ReEncryptRequestDto request = ReEncryptRequestDto.builder()
                .sourceKeyId(keyId)
                .destinationKeyId(2L)
                .ciphertextBlob(Base64.getEncoder().encodeToString("encrypted".getBytes()))
                .build();

        KmsKey destKey = KmsKey.builder()
                .keyId(2L)
                .tenant(tenant)
                .status(IEnumKeyStatus.Types.ENABLED)
                .keyPurpose(IEnumKeyPurpose.Types.ENCRYPT_DECRYPT)
                .keyMaterial(new byte[]{4, 5, 6})
                .currentVersionId("v2")
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(testKey));
        when(kmsKeyRepository.findByTenantAndKeyId(tenant, 2L)).thenReturn(Optional.of(destKey));
        
        when(cryptoService.decryptData(any(), eq(testKey.getKeyMaterial()), any())).thenReturn("hello".getBytes());
        when(cryptoService.encryptData(any(), eq(destKey.getKeyMaterial()), any())).thenReturn("re-encrypted".getBytes());

        ReEncryptResponseDto response = encryptionService.reencrypt(tenant, request);

        assertNotNull(response);
        assertEquals(Base64.getEncoder().encodeToString("re-encrypted".getBytes()), response.getCiphertext());
        assertEquals(keyId, response.getSourceKeyId());
        assertEquals(2L, response.getDestinationKeyId());
    }
}
