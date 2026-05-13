package eu.isygoit.service.impl;

import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.model.KmsKey;
import eu.isygoit.model.KmsKeyVersion;
import eu.isygoit.repository.KmsKeyRepository;
import eu.isygoit.repository.KmsKeyVersionRepository;
import eu.isygoit.service.ICryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeyManagementServiceImplTest {

    @Mock
    private KmsKeyRepository kmsKeyRepository;
    @Mock
    private KmsKeyVersionRepository kmsKeyVersionRepository;
    @Mock
    private ICryptoService cryptoService;

    @InjectMocks
    private KeyManagementServiceImpl keyManagementService;

    private final String tenant = "test-tenant";

    @BeforeEach
    void setUp() {
    }

    @Test
    void createKey_Success() {
        CreateKeyRequest request = CreateKeyRequest.builder()
                .keySpec(IEnumKeySpec.Types.RSA_2048)
                .keyUsage(null)
                .build();

        byte[] material = new byte[]{1,2,3};
        when(cryptoService.generateKeyMaterial(any())).thenReturn(material);

        KmsKey savedKey = KmsKey.builder()
                .keyId("key-1")
                .keyArn("arn:aws:kms:::key/key-1")
                .keySpec(IEnumKeySpec.Types.RSA_2048)
                .keyStatus(IEnumKeyStatus.Types.ENABLED)
                .creationDate(LocalDateTime.now())
                .currentVersionId("v-1")
                .keyMaterial(material)
                .build();

        when(kmsKeyRepository.save(any(KmsKey.class))).thenReturn(savedKey);

        KmsKeyVersion savedVersion = KmsKeyVersion.builder()
                .keyId("key-1")
                .versionId("v-1")
                .creationDate(LocalDateTime.now())
                .activationDate(LocalDateTime.now())
                .keyMaterial(material)
                .build();

        when(kmsKeyVersionRepository.save(any(KmsKeyVersion.class))).thenReturn(savedVersion);

        CreateKeyResponse resp = keyManagementService.createKey(tenant, request);

        assertNotNull(resp);
        assertNotNull(resp.getKeyMetadata());
        assertEquals("key-1", resp.getKeyMetadata().getKeyId());
        verify(kmsKeyRepository).save(any(KmsKey.class));
        verify(kmsKeyVersionRepository).save(any(KmsKeyVersion.class));
    }

    @Test
    void describeKey_Found() {
        KmsKey key = KmsKey.builder()
                .keyId("k1")
                .keyArn("arn:aws:kms:::key/k1")
                .keySpec(IEnumKeySpec.Types.RSA_2048)
                .keyUsage(null)
                .keyStatus(IEnumKeyStatus.Types.ENABLED)
                .currentVersionId("v-1")
                .creationDate(LocalDateTime.now())
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(eq(tenant), eq("k1"))).thenReturn(Optional.of(key));

        DescribeKeyResponse resp = keyManagementService.describeKey(tenant, "k1", null);

        assertNotNull(resp);
        assertNotNull(resp.getKeyMetadata());
        assertEquals("k1", resp.getKeyMetadata().getKeyId());
        assertEquals(IEnumKeyStatus.Types.ENABLED, resp.getKeyMetadata().getStatus());
    }

    @Test
    void describeKey_NotFound_ShouldThrow() {
        when(kmsKeyRepository.findByTenantAndKeyId(anyString(), anyString())).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> keyManagementService.describeKey(tenant, "no", null));
    }

    @Test
    void listKeys_ReturnsPage() {
        KmsKey key = KmsKey.builder()
                .keyId("k1")
                .keyArn("arn:aws:kms:::key/k1")
                .creationDate(LocalDateTime.now())
                .build();

        when(kmsKeyRepository.findByTenant(eq(tenant), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(key)));

        ListKeysResponse resp = keyManagementService.listKeys(tenant, 10, null);

        assertNotNull(resp);
        assertNotNull(resp.getKeys());
        assertEquals(1, resp.getKeys().size());
        assertEquals("k1", resp.getKeys().get(0).getKeyId());
    }
}
