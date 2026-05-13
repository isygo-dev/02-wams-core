package eu.isygoit.service.impl;

import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.enums.IEnumKeyUsage;
import eu.isygoit.exception.InvalidKeyStateException;
import eu.isygoit.exception.KeyNotFoundException;
import eu.isygoit.model.*;
import eu.isygoit.repository.*;
import eu.isygoit.service.ICryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeyManagementServiceTest {

    private static final String TENANT = "tenant-1";
    private static final String KEY_ID = "key-1";

    @Mock
    private CustomKeyStoreRepository customKeyStoreRepository;

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
    private KeyManagementService keyManagementService;

    private KmsKey key;

    @BeforeEach
    void setUp() {
        key = KmsKey.builder()
                .keyId(KEY_ID)
                .tenant(TENANT)
                .keyArn("arn:test")
                .keySpec(IEnumKeySpec.Types.RSA_2048)
                .keyUsage(IEnumKeyUsage.Types.ENCRYPT_DECRYPT)
                .keyStatus(IEnumKeyStatus.Types.ENABLED)
                .currentVersionId("v1")
                .rotationEnabled(false)
                .keyAlias("alias/test")
                .description("desc")
                .creationDate(LocalDateTime.now())
                .keyMaterial(new byte[]{1, 2, 3})
                .build();
    }

    @Test
    void shouldCreateKey() {
        CreateKeyRequest request = CreateKeyRequest.builder()
                .keySpec(IEnumKeySpec.Types.RSA_2048)
                .keyUsage(IEnumKeyUsage.Types.ENCRYPT_DECRYPT)
                .alias("alias/test")
                .description("description")
                .build();

        when(cryptoService.generateKeyMaterial(any())).thenReturn(new byte[]{1, 2, 3});
        when(kmsKeyRepository.save(any())).thenAnswer(invocation -> {
            KmsKey saved = invocation.getArgument(0);
            saved.setKeyId(KEY_ID);
            return saved;
        });

        CreateKeyResponse response = keyManagementService.createKey(TENANT, request);

        assertNotNull(response);
        assertNotNull(response.getKeyMetadata());
        assertEquals(KEY_ID, response.getKeyMetadata().getKeyId());
        assertEquals(IEnumKeyStatus.Types.ENABLED, response.getKeyMetadata().getStatus());

        verify(cryptoService).generateKeyMaterial(any());
        verify(kmsKeyRepository).save(any(KmsKey.class));
        verify(kmsKeyVersionRepository).save(any(KmsKeyVersion.class));
    }

    @Test
    void shouldThrowRuntimeExceptionWhenCreateKeyFails() {
        CreateKeyRequest request = CreateKeyRequest.builder()
                .keySpec(IEnumKeySpec.Types.RSA_2048)
                .build();

        when(cryptoService.generateKeyMaterial(any()))
                .thenThrow(new RuntimeException("boom"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> keyManagementService.createKey(TENANT, request));

        assertTrue(exception.getMessage().contains("Failed to create key"));
    }

    @Test
    void shouldDescribeKey() {
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        DescribeKeyResponse response = keyManagementService.describeKey(TENANT, KEY_ID, List.of());

        assertNotNull(response);
        assertEquals(KEY_ID, response.getKeyMetadata().getKeyId());
        assertEquals(key.getKeyArn(), response.getKeyMetadata().getArn());
    }

    @Test
    void shouldThrowWhenDescribeKeyNotFound() {
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.empty());

        assertThrows(KeyNotFoundException.class,
                () -> keyManagementService.describeKey(TENANT, KEY_ID, List.of()));
    }

    @Test
    void shouldListKeys() {
        Page<KmsKey> page = new PageImpl<>(List.of(key));

        when(kmsKeyRepository.findByTenant(eq(TENANT), any(Pageable.class)))
                .thenReturn(page);

        ListKeysResponse response = keyManagementService.listKeys(TENANT, 10, "0");

        assertEquals(1, response.getKeys().size());
        assertEquals(KEY_ID, response.getKeys().get(0).getKeyId());
    }

    @Test
    void shouldEnableKey() {
        key.setKeyStatus(IEnumKeyStatus.Types.DISABLED);

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        EnableKeyResponse response = keyManagementService.enableKey(TENANT, KEY_ID);

        assertEquals(IEnumKeyStatus.Types.ENABLED, response.getKeyStatus());
        verify(kmsKeyRepository).save(key);
    }

    @Test
    void shouldDisableKey() {
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        DisableKeyResponse response = keyManagementService.disableKey(TENANT, KEY_ID);

        assertEquals(IEnumKeyStatus.Types.DISABLED, response.getStatus());
        verify(kmsKeyRepository).save(key);
    }

    @Test
    void shouldThrowWhenDisablePendingDeletionKey() {
        key.setKeyStatus(IEnumKeyStatus.Types.PENDING_DELETION);

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        assertThrows(InvalidKeyStateException.class,
                () -> keyManagementService.disableKey(TENANT, KEY_ID));
    }

    @Test
    void shouldScheduleKeyDeletion() {
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        ScheduleKeyDeletionResponse response =
                keyManagementService.scheduleKeyDeletion(TENANT, KEY_ID, 10);

        assertEquals(IEnumKeyStatus.Types.PENDING_DELETION, response.getKeyStatus());
        assertEquals(10, response.getPendingWindowInDays());
        assertNotNull(response.getDeletionDate());
    }

    @Test
    void shouldThrowForInvalidDeletionWindow() {
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        assertThrows(IllegalArgumentException.class,
                () -> keyManagementService.scheduleKeyDeletion(TENANT, KEY_ID, 2));
    }

    @Test
    void shouldRotateKey() {
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        when(cryptoService.generateKeyMaterial(any()))
                .thenReturn(new byte[]{4, 5, 6});

        RotateKeyResponse response = keyManagementService.rotateKey(TENANT, KEY_ID);

        assertEquals(KEY_ID, response.getKeyId());
        assertNotNull(response.getNewVersionId());

        verify(kmsKeyVersionRepository).save(any(KmsKeyVersion.class));
        verify(kmsKeyRepository).save(key);
    }

    @Test
    void shouldThrowWhenRotateDisabledKey() {
        key.setKeyStatus(IEnumKeyStatus.Types.DISABLED);

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        assertThrows(InvalidKeyStateException.class,
                () -> keyManagementService.rotateKey(TENANT, KEY_ID));
    }

    @Test
    void shouldUpdateDescriptionAndAlias() {
        UpdateKeyDescriptionRequest request = UpdateKeyDescriptionRequest.builder()
                .alias("new-alias")
                .description("new-description")
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        when(kmsKeyRepository.save(any())).thenReturn(key);

        UpdateKeyDescriptionResponse response =
                keyManagementService.updateKeyDescription(TENANT, KEY_ID, request);

        assertEquals("new-alias", response.getKeyMetadata().getAlias());
        assertEquals("new-description", response.getKeyMetadata().getDescription());
    }

    @Test
    void shouldCancelKeyDeletion() {
        key.setKeyStatus(IEnumKeyStatus.Types.PENDING_DELETION);

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        CancelKeyDeletionResponse response =
                keyManagementService.cancelKeyDeletion(TENANT, KEY_ID);

        assertEquals(IEnumKeyStatus.Types.DISABLED, response.getKeyStatus());
    }

    @Test
    void shouldThrowWhenCancelDeletionForNonPendingKey() {
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        assertThrows(InvalidKeyStateException.class,
                () -> keyManagementService.cancelKeyDeletion(TENANT, KEY_ID));
    }

    @Test
    void shouldUpdateKeyRotation() {
        UpdateKeyRotationRequestDto request = UpdateKeyRotationRequestDto.builder()
                .enableRotation(true)
                .rotationPeriodDays(30)
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        KeyRotationStatusResponseDto response =
                keyManagementService.updateKeyRotation(TENANT, KEY_ID, request);

        assertTrue(response.getRotationEnabled());
        assertEquals(30, response.getRotationPeriodDays());
    }

    @Test
    void shouldGetRotationStatus() {
        key.setRotationEnabled(true);

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        GetKeyRotationStatusResponse response =
                keyManagementService.getKeyRotationStatus(TENANT, KEY_ID);

        assertTrue(response.getRotationEnabled());
    }

    @Test
    void shouldGetPublicKey() {
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        when(cryptoService.extractPublicKey(any(), any()))
                .thenReturn(new byte[]{9, 8, 7});

        GetPublicKeyResponse response = keyManagementService.getPublicKey(TENANT, KEY_ID);

        assertEquals(KEY_ID, response.getKeyId());
        assertNotNull(response.getPublicKey());
    }

    @Test
    void shouldCreateAlias() {
        CreateAliasRequestDto request = CreateAliasRequestDto.builder()
                .aliasName("alias/test")
                .targetKeyId(KEY_ID)
                .build();

        when(kmsAliasRepository.findByTenantAndAliasName(TENANT, "alias/test"))
                .thenReturn(Optional.empty());

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        when(kmsAliasRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AliasResponseDto response = keyManagementService.createAlias(TENANT, request);

        assertEquals("alias/test", response.getAliasName());
        assertEquals(KEY_ID, response.getTargetKeyId());
    }

    @Test
    void shouldThrowWhenAliasAlreadyExists() {
        CreateAliasRequestDto request = CreateAliasRequestDto.builder()
                .aliasName("alias/test")
                .targetKeyId(KEY_ID)
                .build();

        when(kmsAliasRepository.findByTenantAndAliasName(TENANT, "alias/test"))
                .thenReturn(Optional.of(KmsAlias.builder().build()));

        assertThrows(IllegalArgumentException.class,
                () -> keyManagementService.createAlias(TENANT, request));
    }

    @Test
    void shouldUpdateAlias() {
        KmsAlias alias = KmsAlias.builder()
                .aliasName("alias/test")
                .keyId(KEY_ID)
                .build();

        UpdateAliasRequestDto request = UpdateAliasRequestDto.builder()
                .targetKeyId(KEY_ID)
                .build();

        when(kmsAliasRepository.findByTenantAndAliasName(TENANT, "alias/test"))
                .thenReturn(Optional.of(alias));

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        AliasResponseDto response =
                keyManagementService.updateAlias(TENANT, "alias/test", request);

        assertEquals(KEY_ID, response.getTargetKeyId());
    }

    @Test
    void shouldDeleteAlias() {
        KmsAlias alias = KmsAlias.builder().aliasName("alias/test").build();

        when(kmsAliasRepository.findByTenantAndAliasName(TENANT, "alias/test"))
                .thenReturn(Optional.of(alias));

        keyManagementService.deleteAlias(TENANT, "alias/test");

        verify(kmsAliasRepository).delete(alias);
    }

    @Test
    void shouldListAliases() {
        KmsAlias alias = KmsAlias.builder()
                .aliasName("alias/test")
                .keyId(KEY_ID)
                .build();

        Page<KmsAlias> page = new PageImpl<>(List.of(alias));

        when(kmsAliasRepository.findByTenant(eq(TENANT), any(Pageable.class)))
                .thenReturn(page);

        ListAliasesResponseDto response = keyManagementService.listAliases(TENANT, 10, "0");

        assertEquals(1, response.getAliases().size());
    }

    @Test
    void shouldListAliasesForKey() {
        KmsAlias alias = KmsAlias.builder()
                .aliasName("alias/test")
                .keyId(KEY_ID)
                .build();

        when(kmsAliasRepository.findByTenantAndKeyId(eq(TENANT), eq(KEY_ID), any(Pageable.class)))
                .thenReturn(List.of(alias));

        ListAliasesResponseDto response =
                keyManagementService.listAliasesForKey(TENANT, KEY_ID, 10, "0");

        assertEquals(1, response.getAliases().size());
    }

    @Test
    void shouldTagResource() {
        Map<String, String> tags = new HashMap<>();
        tags.put("env", "prod");
        tags.put("team", "security");

        TagResourceRequestDto request = TagResourceRequestDto.builder()
                .tags(tags)
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        Object response = keyManagementService.tagResource(TENANT, KEY_ID, request);

        assertNotNull(response);
        verify(kmsTagRepository, times(2)).save(any(KmsTag.class));
    }

    @Test
    void shouldUntagResource() {
        UntagResourceRequestDto request = UntagResourceRequestDto.builder()
                .tagKeys(List.of("env"))
                .build();

        Object response = keyManagementService.untagResource(TENANT, KEY_ID, request);

        assertNotNull(response);

        verify(kmsTagRepository)
                .deleteByTenantAndKeyIdAndTagKeyIn(TENANT, KEY_ID, List.of("env"));
    }

    @Test
    void shouldListResourceTags() {
        KmsTag tag = KmsTag.builder()
                .tagKey("env")
                .tagValue("prod")
                .build();

        when(kmsTagRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(List.of(tag));

        ListTagsResponseDto response = keyManagementService.listResourceTags(TENANT, KEY_ID);

        assertEquals(1, response.getTags().size());
        assertEquals("env", response.getTags().get(0).getTagKey());
    }

    @Test
    void shouldGetImportParameters() {
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        when(cryptoService.generateWrappingKey())
                .thenReturn(new byte[]{1});

        when(cryptoService.generateImportToken())
                .thenReturn(new byte[]{2});

        ImportParametersResponseDto response =
                keyManagementService.getParametersForImport(TENANT, KEY_ID);

        assertEquals(KEY_ID, response.getKeyId());
        assertEquals(24, response.getValidityPeriodHours());
    }

    @Test
    void shouldImportKeyMaterial() {
        ImportKeyMaterialRequestDto request = ImportKeyMaterialRequestDto.builder()
                .encryptedKeyMaterial(new byte[]{1})
                .importToken(new byte[]{2})
                .expirationDate(LocalDateTime.now().plusDays(1))
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        when(cryptoService.decryptKeyMaterial(any(), any(), any()))
                .thenReturn(new byte[]{9});

        KeyDescriptionResponseDto response =
                keyManagementService.importKeyMaterial(TENANT, KEY_ID, request);

        assertEquals(KEY_ID, response.getKeyId());
        assertTrue(key.getImported());
    }

    @Test
    void shouldDeleteImportedKeyMaterial() {
        key.setImported(true);

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        KeyDescriptionResponseDto response =
                keyManagementService.deleteImportedKeyMaterial(TENANT, KEY_ID);

        assertEquals(KEY_ID, response.getKeyId());
        assertNull(key.getKeyMaterial());
    }

    @Test
    void shouldThrowWhenDeleteNonImportedMaterial() {
        key.setImported(false);

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        assertThrows(InvalidKeyStateException.class,
                () -> keyManagementService.deleteImportedKeyMaterial(TENANT, KEY_ID));
    }

    @Test
    void shouldValidateKey() {
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        when(cryptoService.validateKeyIntegrity(any(), any()))
                .thenReturn(true);

        assertDoesNotThrow(() -> keyManagementService.isValidKey(TENANT, KEY_ID));
    }

    @Test
    void shouldThrowWhenKeyPendingDeletionDuringValidation() {
        key.setKeyStatus(IEnumKeyStatus.Types.PENDING_DELETION);

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        assertThrows(InvalidKeyStateException.class,
                () -> keyManagementService.isValidKey(TENANT, KEY_ID));
    }

    @Test
    void shouldThrowWhenKeyMaterialMissing() {
        key.setKeyMaterial(null);

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        assertThrows(InvalidKeyStateException.class,
                () -> keyManagementService.isValidKey(TENANT, KEY_ID));
    }

    @Test
    void shouldThrowWhenKeyIntegrityFails() {
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        when(cryptoService.validateKeyIntegrity(any(), any()))
                .thenReturn(false);

        assertThrows(InvalidKeyStateException.class,
                () -> keyManagementService.isValidKey(TENANT, KEY_ID));
    }

    @Test
    void shouldDeleteKey() {
        key.setKeyStatus(IEnumKeyStatus.Types.PENDING_DELETION);

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        keyManagementService.deleteKey(TENANT, KEY_ID);

        verify(kmsKeyVersionRepository).deleteByTenantAndKeyId(TENANT, KEY_ID);
        verify(kmsAliasRepository).deleteByTenantAndKeyId(TENANT, KEY_ID);
        verify(kmsTagRepository).deleteByTenantAndKeyId(TENANT, KEY_ID);
        verify(kmsKeyRepository).delete(key);
    }

    @Test
    void shouldThrowWhenDeleteKeyNotPendingDeletion() {
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        assertThrows(InvalidKeyStateException.class,
                () -> keyManagementService.deleteKey(TENANT, KEY_ID));
    }

    @Test
    void shouldListKeyRotations() {
        KmsKeyVersion version = KmsKeyVersion.builder()
                .versionId("v2")
                .rotationDate(LocalDateTime.now())
                .build();

        Page<KmsKeyVersion> page = new PageImpl<>(List.of(version));

        when(kmsKeyVersionRepository.findByTenantAndKeyIdAndRotationDateIsNotNull(
                eq(TENANT), eq(KEY_ID), any(Pageable.class)))
                .thenReturn(page);

        ListKeyRotationsResponseDto response =
                keyManagementService.listKeyRotations(TENANT, KEY_ID, 10, "0");

        assertEquals(1, response.getRotations().size());
        assertEquals("v2", response.getRotations().get(0).getVersionId());
    }

    @Test
    void shouldGetKeyUsageStats() {
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(key));

        when(cryptoService.getEncryptCount(KEY_ID)).thenReturn(100L);
        when(cryptoService.getDecryptCount(KEY_ID)).thenReturn(50L);
        when(cryptoService.getLastUsedDate(KEY_ID)).thenReturn(LocalDateTime.now());

        KeyUsageStatsResponseDto response =
                keyManagementService.getKeyUsageStats(TENANT, KEY_ID);

        assertEquals(100L, response.getEncryptCount());
        assertEquals(50L, response.getDecryptCount());
        assertNotNull(response.getLastUsedDate());
    }

    @Test
    void shouldCountKeysInCustomStore() {
        List<KmsKey> keys = List.of(
                KmsKey.builder().keyId("k1").build(),
                KmsKey.builder().keyId("k2").build(),
                KmsKey.builder().keyId("k3").build(),
                KmsKey.builder().keyId("k4").build(),
                KmsKey.builder().keyId("k5").build()
        );
        when(customKeyStoreRepository.findByTenantAndId(TENANT, 1L))
                .thenReturn(Optional.of(CustomKeyStore.builder()
                        .id(1L)
                        .keys(keys)
                        .build()));

        int count = keyManagementService.countKeysInCustomKeyStore(TENANT, 1L);

        assertEquals(5, count);
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

        CreateKeyResponse resp = keyManagementService.createKey(TENANT, request);

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

        when(kmsKeyRepository.findByTenantAndKeyId(eq(TENANT), eq("k1"))).thenReturn(Optional.of(key));

        DescribeKeyResponse resp = keyManagementService.describeKey(TENANT, "k1", null);

        assertNotNull(resp);
        assertNotNull(resp.getKeyMetadata());
        assertEquals("k1", resp.getKeyMetadata().getKeyId());
        assertEquals(IEnumKeyStatus.Types.ENABLED, resp.getKeyMetadata().getStatus());
    }

    @Test
    void describeKey_NotFound_ShouldThrow() {
        when(kmsKeyRepository.findByTenantAndKeyId(anyString(), anyString())).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> keyManagementService.describeKey(TENANT, "no", null));
    }

    @Test
    void listKeys_ReturnsPage() {
        KmsKey key = KmsKey.builder()
                .keyId("k1")
                .keyArn("arn:aws:kms:::key/k1")
                .creationDate(LocalDateTime.now())
                .build();

        when(kmsKeyRepository.findByTenant(eq(TENANT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(key)));

        ListKeysResponse resp = keyManagementService.listKeys(TENANT, 10, null);

        assertNotNull(resp);
        assertNotNull(resp.getKeys());
        assertEquals(1, resp.getKeys().size());
        assertEquals("k1", resp.getKeys().get(0).getKeyId());
    }
}
