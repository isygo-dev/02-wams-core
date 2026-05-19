package eu.isygoit.service.impl;

import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.enums.*;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SigningServiceTest {

    private static final String TENANT = "tenant-1";
    private static final String KEY_ID = "key-1";

    @Mock
    private KmsKeyRepository kmsKeyRepository;

    @Mock
    private ICryptoService cryptoService;

    @InjectMocks
    private SigningService service;

    private KmsKey kmsKey;

    @BeforeEach
    void setUp() {

        kmsKey = KmsKey.builder()
                .keyId(KEY_ID)
                .tenant(TENANT)
                .keyMaterial("secret-key-material".getBytes())
                .currentVersionId("v1")
                .keyUsage(IEnumKeyUsage.Types.SIGN_VERIFY)
                .keyStatus(IEnumKeyStatus.Types.ENABLED)
                .build();
    }

    @Test
    void shouldSignSuccessfully() {

        String message = Base64.getEncoder()
                .encodeToString("hello".getBytes());

        SignRequest request = SignRequest.builder()
                .keyId(KEY_ID)
                .message(message)
                .signingAlgorithm(IEnumSignatureAlgorithm.ECDSA_SHA_256)
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(kmsKey));

        when(cryptoService.signData(any(), any(), any()))
                .thenReturn("signature".getBytes());

        SignResponse response = service.sign(TENANT, request);

        assertNotNull(response);
        assertEquals(KEY_ID, response.getKeyId());
        assertEquals("v1", response.getKeyVersionId());

        byte[] decoded = Base64.getDecoder()
                .decode(response.getSignature());

        assertEquals("signature", new String(decoded));

        verify(cryptoService).signData(
                any(),
                eq(kmsKey.getKeyMaterial()),
                eq(IEnumSignatureAlgorithm.ECDSA_SHA_256)
        );
    }

    @Test
    void shouldThrowWhenSignKeyNotFound() {

        SignRequest request = SignRequest.builder()
                .keyId(KEY_ID)
                .message(Base64.getEncoder().encodeToString("hello".getBytes()))
                .signingAlgorithm(IEnumSignatureAlgorithm.ECDSA_SHA_256)
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.sign(TENANT, request)
        );

        assertEquals("KMS Key not found", exception.getMessage());
    }

    @Test
    void shouldThrowWhenSignKeyDisabled() {

        kmsKey.setKeyStatus(IEnumKeyStatus.Types.DISABLED);

        SignRequest request = SignRequest.builder()
                .keyId(KEY_ID)
                .message(Base64.getEncoder().encodeToString("hello".getBytes()))
                .signingAlgorithm(IEnumSignatureAlgorithm.ECDSA_SHA_256)
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(kmsKey));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.sign(TENANT, request)
        );

        assertEquals("KMS Key is not enabled", exception.getMessage());
    }

    @Test
    void shouldThrowWhenKeyUsageInvalidForSigning() {

        kmsKey.setKeyUsage(IEnumKeyUsage.Types.ENCRYPT_DECRYPT);

        SignRequest request = SignRequest.builder()
                .keyId(KEY_ID)
                .message(Base64.getEncoder().encodeToString("hello".getBytes()))
                .signingAlgorithm(IEnumSignatureAlgorithm.ECDSA_SHA_256)
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(kmsKey));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.sign(TENANT, request)
        );

        assertEquals(
                "KMS Key is not authorized for signing",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenSigningAlgorithmMissing() {

        SignRequest request = SignRequest.builder()
                .keyId(KEY_ID)
                .message(Base64.getEncoder().encodeToString("hello".getBytes()))
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(kmsKey));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.sign(TENANT, request)
        );

        assertEquals(
                "Signing algorithm is required",
                exception.getMessage()
        );
    }

    @Test
    void shouldVerifySuccessfully() {

        String message = Base64.getEncoder()
                .encodeToString("hello".getBytes());

        String signature = Base64.getEncoder()
                .encodeToString("signature".getBytes());

        VerifyRequest request = VerifyRequest.builder()
                .keyId(KEY_ID)
                .message(message)
                .signature(signature)
                .signingAlgorithm(IEnumSignatureAlgorithm.ECDSA_SHA_256)
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(kmsKey));

        when(cryptoService.verifySignature(any(), any(), any(), any()))
                .thenReturn(true);

        VerifyResponse response = service.verify(TENANT, request);

        assertNotNull(response);
        assertTrue(response.isValid());
        assertEquals(KEY_ID, response.getKeyId());

        verify(cryptoService).verifySignature(
                any(),
                any(),
                eq(kmsKey.getKeyMaterial()),
                eq(IEnumSignatureAlgorithm.ECDSA_SHA_256)
        );
    }

    @Test
    void shouldReturnInvalidSignature() {

        VerifyRequest request = VerifyRequest.builder()
                .keyId(KEY_ID)
                .message(Base64.getEncoder().encodeToString("hello".getBytes()))
                .signature(Base64.getEncoder().encodeToString("bad-signature".getBytes()))
                .signingAlgorithm(IEnumSignatureAlgorithm.ECDSA_SHA_256)
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(kmsKey));

        when(cryptoService.verifySignature(any(), any(), any(), any()))
                .thenReturn(false);

        VerifyResponse response = service.verify(TENANT, request);

        assertFalse(response.isValid());
    }

    @Test
    void shouldThrowWhenVerifyKeyDisabled() {

        kmsKey.setKeyStatus(IEnumKeyStatus.Types.DISABLED);

        VerifyRequest request = VerifyRequest.builder()
                .keyId(KEY_ID)
                .message(Base64.getEncoder().encodeToString("hello".getBytes()))
                .signature(Base64.getEncoder().encodeToString("signature".getBytes()))
                .signingAlgorithm(IEnumSignatureAlgorithm.ECDSA_SHA_256)
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(kmsKey));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.verify(TENANT, request)
        );

        assertEquals("KMS Key is not enabled", exception.getMessage());
    }

    @Test
    void shouldThrowWhenVerifyAlgorithmMissing() {

        VerifyRequest request = VerifyRequest.builder()
                .keyId(KEY_ID)
                .message(Base64.getEncoder().encodeToString("hello".getBytes()))
                .signature(Base64.getEncoder().encodeToString("signature".getBytes()))
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(kmsKey));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.verify(TENANT, request)
        );

        assertEquals("Algorithm is required", exception.getMessage());
    }

    @Test
    void shouldGenerateMacSuccessfully() {

        GenerateMacRequest request = GenerateMacRequest.builder()
                .keyId(KEY_ID)
                .message(Base64.getEncoder().encodeToString("hello".getBytes()))
                .macAlgorithm(IEnumMacAlgorithm.HMAC_SHA_224)
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(kmsKey));

        GenerateMacResponse response =
                service.generateMac(TENANT, request);

        assertNotNull(response);
        assertNotNull(response.getMac());
        assertEquals(KEY_ID, response.getKeyId());
    }

    @Test
    void shouldThrowWhenGenerateMacKeyDisabled() {

        kmsKey.setKeyStatus(IEnumKeyStatus.Types.DISABLED);

        GenerateMacRequest request = GenerateMacRequest.builder()
                .keyId(KEY_ID)
                .message(Base64.getEncoder().encodeToString("hello".getBytes()))
                .macAlgorithm(IEnumMacAlgorithm.HMAC_SHA_224)
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(kmsKey));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.generateMac(TENANT, request)
        );

        assertEquals("KMS Key is not enabled", exception.getMessage());
    }

    @Test
    void shouldVerifyMacSuccessfully() {

        String message = Base64.getEncoder()
                .encodeToString("hello".getBytes());

        GenerateMacRequest generateRequest = GenerateMacRequest.builder()
                .keyId(KEY_ID)
                .message(message)
                .macAlgorithm(IEnumMacAlgorithm.HMAC_SHA_224)
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(kmsKey));

        GenerateMacResponse generated =
                service.generateMac(TENANT, generateRequest);

        VerifyMacRequest verifyRequest = VerifyMacRequest.builder()
                .keyId(KEY_ID)
                .message(message)
                .mac(generated.getMac())
                .macAlgorithm(IEnumMacAlgorithm.HMAC_SHA_224)
                .build();

        VerifyMacResponse response =
                service.verifyMac(TENANT, verifyRequest);

        assertNotNull(response);
        assertTrue(response.getMacValid());
        assertEquals(KEY_ID, response.getKeyId());
    }

    @Test
    void shouldReturnInvalidMac() {

        VerifyMacRequest request = VerifyMacRequest.builder()
                .keyId(KEY_ID)
                .message(Base64.getEncoder().encodeToString("hello".getBytes()))
                .mac(Base64.getEncoder().encodeToString("invalid".getBytes()))
                .macAlgorithm(IEnumMacAlgorithm.HMAC_SHA_224)
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(kmsKey));

        VerifyMacResponse response =
                service.verifyMac(TENANT, request);

        assertFalse(response.getMacValid());
    }

    @Test
    void shouldThrowWhenVerifyMacKeyDisabled() {

        kmsKey.setKeyStatus(IEnumKeyStatus.Types.DISABLED);

        VerifyMacRequest request = VerifyMacRequest.builder()
                .keyId(KEY_ID)
                .message(Base64.getEncoder().encodeToString("hello".getBytes()))
                .mac(Base64.getEncoder().encodeToString("invalid".getBytes()))
                .macAlgorithm(IEnumMacAlgorithm.HMAC_SHA_224)
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(kmsKey));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.verifyMac(TENANT, request)
        );

        assertEquals("KMS Key is not enabled", exception.getMessage());
    }
}