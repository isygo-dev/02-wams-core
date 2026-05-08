package eu.isygoit.service.impl;

import eu.isygoit.dto.request.GenerateMacRequestDto;
import eu.isygoit.dto.request.SignRequestDto;
import eu.isygoit.dto.request.VerifyMacRequestDto;
import eu.isygoit.dto.request.VerifyRequestDto;
import eu.isygoit.dto.response.GenerateMacResponseDto;
import eu.isygoit.dto.response.SignResponseDto;
import eu.isygoit.dto.response.VerifyMacResponseDto;
import eu.isygoit.dto.response.VerifyResponseDto;
import eu.isygoit.enums.IEnumKeyPurpose;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.enums.IEnumSigningAlgorithm;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SigningServiceImplTest {

    private final String tenant = "test-tenant" ;
    private final Long keyId = 1L;
    @Mock
    private KmsKeyRepository kmsKeyRepository;
    @Mock
    private ICryptoService cryptoService;
    @InjectMocks
    private SigningServiceImpl signingService;
    private KmsKey testKey;

    @BeforeEach
    void setUp() {
        testKey = KmsKey.builder()
                .keyId(keyId)
                .tenant(tenant)
                .status(IEnumKeyStatus.Types.ENABLED)
                .keyPurpose(IEnumKeyPurpose.Types.SIGN_VERIFY)
                .keyMaterial(new byte[]{1, 2, 3})
                .build();
    }

    @Test
    void testSign_Success() {
        SignRequestDto request = SignRequestDto.builder()
                .keyId(keyId)
                .message(Base64.getEncoder().encodeToString("hello".getBytes()))
                .algorithm(IEnumSigningAlgorithm.Types.RSASSA_PSS_SHA256)
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(testKey));
        when(cryptoService.signData(any(), any(), eq(IEnumSigningAlgorithm.Types.RSASSA_PSS_SHA256.meaning())))
                .thenReturn("signature".getBytes());

        SignResponseDto response = signingService.sign(tenant, request);

        assertNotNull(response);
        assertEquals(Base64.getEncoder().encodeToString("signature".getBytes()), response.getSignature());
        assertEquals(keyId, response.getKeyId());
    }

    @Test
    void testVerify_Success() {
        VerifyRequestDto request = VerifyRequestDto.builder()
                .keyId(keyId)
                .message(Base64.getEncoder().encodeToString("hello".getBytes()))
                .signature(Base64.getEncoder().encodeToString("signature".getBytes()))
                .algorithm(IEnumSigningAlgorithm.Types.RSASSA_PSS_SHA256)
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(testKey));
        when(cryptoService.verifySignature(any(), any(), any(), eq(IEnumSigningAlgorithm.Types.RSASSA_PSS_SHA256.meaning())))
                .thenReturn(true);

        VerifyResponseDto response = signingService.verify(tenant, request);

        assertNotNull(response);
        assertTrue(response.getValid());
    }

    @Test
    void testGenerateMac_Success() {
        GenerateMacRequestDto request = GenerateMacRequestDto.builder()
                .keyId(keyId)
                .message(Base64.getEncoder().encodeToString("hello".getBytes()))
                .macAlgorithm("HmacSHA256")
                .build();

        // Need to set key material for actual Mac.getInstance in the service if we don't mock Mac itself
        // But the service uses javax.crypto.Mac directly.
        // We'll need a key material compatible with HmacSHA256 (32 bytes ideally, but any byte[] works for testing if JCA allows)
        testKey.setKeyMaterial(new byte[32]);

        when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(testKey));

        GenerateMacResponseDto response = signingService.generateMac(tenant, request);

        assertNotNull(response);
        assertNotNull(response.getMac());
        assertEquals(keyId, response.getKeyId());
    }

    @Test
    void testVerifyMac_Success() {
        String message = Base64.getEncoder().encodeToString("hello".getBytes());
        String algo = "HmacSHA256" ;
        testKey.setKeyMaterial(new byte[32]);

        when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(testKey));

        // First generate a MAC to verify it
        GenerateMacResponseDto genResponse = signingService.generateMac(tenant, GenerateMacRequestDto.builder()
                .keyId(keyId)
                .message(message)
                .macAlgorithm(algo)
                .build());

        VerifyMacRequestDto request = VerifyMacRequestDto.builder()
                .keyId(keyId)
                .message(message)
                .macAlgorithm(algo)
                .mac(genResponse.getMac())
                .build();

        VerifyMacResponseDto response = signingService.verifyMac(tenant, request);

        assertNotNull(response);
        assertTrue(response.getMacValid());
    }
}
