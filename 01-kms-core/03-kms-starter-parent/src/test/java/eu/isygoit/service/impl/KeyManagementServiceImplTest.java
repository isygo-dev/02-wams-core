package eu.isygoit.service.impl;

import eu.isygoit.dto.request.CreateKeyRequestDto;
import eu.isygoit.dto.response.CreateKeyResponseDto;
import eu.isygoit.dto.response.KeyMetadataResponseDto;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumKeyPurpose;
import eu.isygoit.model.KmsKey;
import eu.isygoit.model.KmsKeyVersion;
import eu.isygoit.repository.KmsAliasRepository;
import eu.isygoit.repository.KmsKeyRepository;
import eu.isygoit.repository.KmsKeyVersionRepository;
import eu.isygoit.repository.KmsTagRepository;
import eu.isygoit.service.ICryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KeyManagementServiceImplTest {

    @Mock
    private KmsKeyRepository kmsKeyRepository;
    @Mock
    private KmsKeyVersionRepository kmsKeyVersionRepository;
    @Mock
    private ICryptoService cryptoService;
    @Mock
    private KmsAliasRepository kmsAliasRepository;
    @Mock
    private KmsTagRepository kmsTagRepository;

    @InjectMocks
    private KeyManagementServiceImpl keyManagementService;

    private final String tenant = "test-tenant";
    private final Long keyId = 1L;

    @Test
    void testCreateKey_Success() {
        CreateKeyRequestDto request = CreateKeyRequestDto.builder()
                .keySpec(IEnumKeySpec.Types.AES_256)
                .purpose(IEnumKeyPurpose.Types.ENCRYPT_DECRYPT)
                .alias("test-alias")
                .build();

        when(cryptoService.generateKeyMaterial(any())).thenReturn(new byte[32]);
        when(kmsKeyRepository.save(any(KmsKey.class))).thenAnswer(invocation -> {
            KmsKey k = invocation.getArgument(0);
            k.setKeyId(keyId);
            return k;
        });

        CreateKeyResponseDto response = keyManagementService.createKey(tenant, request);

        assertNotNull(response);
        assertEquals(keyId, response.getKeyId());
        assertEquals(IEnumKeyStatus.Types.ENABLED, response.getStatus());
        verify(kmsKeyRepository, times(1)).save(any(KmsKey.class));
        verify(kmsKeyVersionRepository, times(1)).save(any(KmsKeyVersion.class));
    }

    @Test
    void testGetKeyMetadata_Success() {
        KmsKey key = KmsKey.builder()
                .keyId(keyId)
                .status(IEnumKeyStatus.Types.ENABLED)
                .keySpec(IEnumKeySpec.Types.AES_256)
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(key));

        KeyMetadataResponseDto response = keyManagementService.getKeyMetadata(tenant, keyId);

        assertNotNull(response);
        assertEquals(keyId, response.getKeyId());
        assertEquals(IEnumKeyStatus.Types.ENABLED, response.getStatus());
    }

    @Test
    void testEnableKey_Success() {
        KmsKey key = KmsKey.builder()
                .keyId(keyId)
                .status(IEnumKeyStatus.Types.DISABLED)
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(key));

        KeyMetadataResponseDto response = keyManagementService.enableKey(tenant, keyId);

        assertEquals(IEnumKeyStatus.Types.ENABLED, response.getStatus());
        verify(kmsKeyRepository).save(key);
    }

    @Test
    void testDisableKey_Success() {
        KmsKey key = KmsKey.builder()
                .keyId(keyId)
                .status(IEnumKeyStatus.Types.ENABLED)
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(key));

        KeyMetadataResponseDto response = keyManagementService.disableKey(tenant, keyId);

        assertEquals(IEnumKeyStatus.Types.DISABLED, response.getStatus());
        verify(kmsKeyRepository).save(key);
    }
}
