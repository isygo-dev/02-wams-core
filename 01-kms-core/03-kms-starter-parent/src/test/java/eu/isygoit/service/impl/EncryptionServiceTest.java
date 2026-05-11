package eu.isygoit.service.impl;

import eu.isygoit.dto.KmsDtos.DecryptRequest;
import eu.isygoit.dto.KmsDtos.DecryptResponse;
import eu.isygoit.dto.KmsDtos.EncryptRequest;
import eu.isygoit.dto.KmsDtos.EncryptResponse;
import eu.isygoit.dto.KmsDtos.ReEncryptRequest;
import eu.isygoit.dto.KmsDtos.ReEncryptResponse;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.enums.IEnumKeyUsage;
import eu.isygoit.exception.KeyNotFoundException;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EncryptionServiceTest {

    private static final String TENANT = "tenant-1";
    private static final String KEY_ID = "key-1";
    private static final String DEST_KEY_ID = "key-2";

    @Mock
    private KmsKeyRepository kmsKeyRepository;

    @Mock
    private ICryptoService cryptoService;

    @InjectMocks
    private EncryptionServiceImpl service;

    private KmsKey kmsKey;

    @BeforeEach
    void setUp() {
        kmsKey = KmsKey.builder()
                .keyId(KEY_ID)
                .tenant(TENANT)
                .keyMaterial(new byte[]{1, 2, 3})
                .currentVersionId("v1")
                .keyUsage(IEnumKeyUsage.Types.ENCRYPT_DECRYPT)
                .keyStatus(IEnumKeyStatus.Types.ENABLED)
                .build();
    }

    @Test
    void shouldEncryptSuccessfully() {

        String plaintext = Base64.getEncoder()
                .encodeToString("hello".getBytes());

        EncryptRequest request = EncryptRequest.builder()
                .keyId(KEY_ID)
                .plaintext(plaintext)
                .encryptionContext(Map.of("env", "test"))
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(kmsKey));

        when(cryptoService.encryptData(any(), any(), any()))
                .thenReturn("encrypted".getBytes());

        EncryptResponse response = service.encrypt(TENANT, request);

        assertNotNull(response);
        assertEquals(KEY_ID, response.getKeyId());
        assertEquals("v1", response.getKeyVersionId());

        byte[] decoded = Base64.getDecoder()
                .decode(response.getCiphertextBlob());

        assertEquals("encrypted", new String(decoded));

        verify(cryptoService).encryptData(
                any(),
                eq(kmsKey.getKeyMaterial()),
                eq(request.getEncryptionContext())
        );
    }

    @Test
    void shouldThrowWhenEncryptKeyNotFound() {

        EncryptRequest request = EncryptRequest.builder()
                .keyId(KEY_ID)
                .plaintext(Base64.getEncoder().encodeToString("hello".getBytes()))
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.encrypt(TENANT, request)
        );

        assertEquals("KMS Key not found", exception.getMessage());
    }

    @Test
    void shouldThrowWhenEncryptKeyDisabled() {

        kmsKey.setKeyStatus(IEnumKeyStatus.Types.DISABLED);

        EncryptRequest request = EncryptRequest.builder()
                .keyId(KEY_ID)
                .plaintext(Base64.getEncoder().encodeToString("hello".getBytes()))
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(kmsKey));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.encrypt(TENANT, request)
        );

        assertEquals("KMS Key is not enabled", exception.getMessage());
    }

    @Test
    void shouldThrowWhenKeyUsageInvalidForEncryption() {

        kmsKey.setKeyUsage(IEnumKeyUsage.Types.SIGN_VERIFY);

        EncryptRequest request = EncryptRequest.builder()
                .keyId(KEY_ID)
                .plaintext(Base64.getEncoder().encodeToString("hello".getBytes()))
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(kmsKey));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.encrypt(TENANT, request)
        );

        assertEquals(
                "KMS Key is not authorized for encryption",
                exception.getMessage()
        );
    }

    @Test
    void shouldDecryptSuccessfully() {

        String ciphertext = Base64.getEncoder()
                .encodeToString("encrypted".getBytes());

        DecryptRequest request = DecryptRequest.builder()
                .keyId(KEY_ID)
                .ciphertextBlob(ciphertext)
                .encryptionContext(Map.of("env", "test"))
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(kmsKey));

        when(cryptoService.decryptData(any(), any(), any(), any()))
                .thenReturn("hello".getBytes());

        DecryptResponse response = service.decrypt(TENANT, request);

        assertNotNull(response);
        assertEquals(KEY_ID, response.getKeyId());
        assertEquals("v1", response.getKeyVersionId());

        String decoded = new String(
                Base64.getDecoder().decode(response.getPlaintext())
        );

        assertEquals("hello", decoded);

        verify(cryptoService).decryptData(
                eq(TENANT),
                any(),
                eq(kmsKey.getKeyMaterial()),
                eq(request.getEncryptionContext())
        );
    }

    @Test
    void shouldThrowWhenDecryptKeyIdMissing() {

        DecryptRequest request = DecryptRequest.builder()
                .ciphertextBlob(Base64.getEncoder()
                        .encodeToString("encrypted".getBytes()))
                .build();

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.decrypt(TENANT, request)
        );

        assertEquals(
                "keyId is required for decryption in this implementation",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenDecryptKeyNotFound() {

        DecryptRequest request = DecryptRequest.builder()
                .keyId(KEY_ID)
                .ciphertextBlob(Base64.getEncoder()
                        .encodeToString("encrypted".getBytes()))
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.decrypt(TENANT, request)
        );

        assertEquals("KMS Key not found", exception.getMessage());
    }

    @Test
    void shouldThrowWhenDecryptKeyDisabled() {

        kmsKey.setKeyStatus(IEnumKeyStatus.Types.DISABLED);

        DecryptRequest request = DecryptRequest.builder()
                .keyId(KEY_ID)
                .ciphertextBlob(Base64.getEncoder()
                        .encodeToString("encrypted".getBytes()))
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(kmsKey));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.decrypt(TENANT, request)
        );

        assertEquals("KMS Key is not enabled", exception.getMessage());
    }

    @Test
    void shouldReEncryptSuccessfully() {

        KmsKey destinationKey = KmsKey.builder()
                .keyId(DEST_KEY_ID)
                .tenant(TENANT)
                .keyMaterial(new byte[]{4, 5, 6})
                .currentVersionId("v2")
                .keyUsage(IEnumKeyUsage.Types.ENCRYPT_DECRYPT)
                .keyStatus(IEnumKeyStatus.Types.ENABLED)
                .build();

        ReEncryptRequest request = ReEncryptRequest.builder()
                .sourceKeyId(KEY_ID)
                .destinationKeyId(DEST_KEY_ID)
                .ciphertextBlob(Base64.getEncoder()
                        .encodeToString("encrypted".getBytes()))
                .sourceEncryptionContext(Map.of("src", "1"))
                .destinationEncryptionContext(Map.of("dest", "2"))
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(kmsKey));

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, DEST_KEY_ID))
                .thenReturn(Optional.of(destinationKey));

        when(cryptoService.decryptData(any(), any(), any(), any()))
                .thenReturn("plain".getBytes());

        when(cryptoService.encryptData(any(), any(), any()))
                .thenReturn("reencrypted".getBytes());

        ReEncryptResponse response =
                service.reEncrypt(TENANT, request);

        assertNotNull(response);
        assertEquals(KEY_ID, response.getSourceKeyId());
        assertEquals(DEST_KEY_ID, response.getDestinationKeyId());
        assertEquals("v2", response.getDestinationKeyVersionId());

        String decoded = new String(
                Base64.getDecoder().decode(response.getCiphertextBlob())
        );

        assertEquals("reencrypted", decoded);
    }

    @Test
    void shouldThrowWhenSourceKeyNotFound() {

        ReEncryptRequest request = ReEncryptRequest.builder()
                .sourceKeyId(KEY_ID)
                .destinationKeyId(DEST_KEY_ID)
                .ciphertextBlob(Base64.getEncoder()
                        .encodeToString("encrypted".getBytes()))
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                KeyNotFoundException.class,
                () -> service.reEncrypt(TENANT, request)
        );
    }

    @Test
    void shouldThrowWhenSourceKeyDisabled() {

        kmsKey.setKeyStatus(IEnumKeyStatus.Types.DISABLED);

        ReEncryptRequest request = ReEncryptRequest.builder()
                .sourceKeyId(KEY_ID)
                .destinationKeyId(DEST_KEY_ID)
                .ciphertextBlob(Base64.getEncoder()
                        .encodeToString("encrypted".getBytes()))
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(kmsKey));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.reEncrypt(TENANT, request)
        );

        assertEquals("Source key is not enabled", exception.getMessage());
    }

    @Test
    void shouldThrowWhenDestinationKeyNotFound() {

        ReEncryptRequest request = ReEncryptRequest.builder()
                .sourceKeyId(KEY_ID)
                .destinationKeyId(DEST_KEY_ID)
                .ciphertextBlob(Base64.getEncoder()
                        .encodeToString("encrypted".getBytes()))
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(kmsKey));

        when(cryptoService.decryptData(any(), any(), any(), any()))
                .thenReturn("plain".getBytes());

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, DEST_KEY_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                KeyNotFoundException.class,
                () -> service.reEncrypt(TENANT, request)
        );
    }

    @Test
    void shouldThrowWhenDestinationKeyDisabled() {

        KmsKey destinationKey = KmsKey.builder()
                .keyId(DEST_KEY_ID)
                .tenant(TENANT)
                .keyMaterial(new byte[]{4, 5, 6})
                .currentVersionId("v2")
                .keyStatus(IEnumKeyStatus.Types.DISABLED)
                .build();

        ReEncryptRequest request = ReEncryptRequest.builder()
                .sourceKeyId(KEY_ID)
                .destinationKeyId(DEST_KEY_ID)
                .ciphertextBlob(Base64.getEncoder()
                        .encodeToString("encrypted".getBytes()))
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(kmsKey));

        when(cryptoService.decryptData(any(), any(), any(), any()))
                .thenReturn("plain".getBytes());

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, DEST_KEY_ID))
                .thenReturn(Optional.of(destinationKey));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.reEncrypt(TENANT, request)
        );

        assertEquals(
                "Destination key is not enabled",
                exception.getMessage()
        );
    }
}