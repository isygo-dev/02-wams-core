package eu.isygoit.service.impl;

import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.dto.data.KeyPairMaterial;
import eu.isygoit.enums.*;
import eu.isygoit.exception.*;
import eu.isygoit.model.*;
import eu.isygoit.repository.*;
import eu.isygoit.service.ICryptoService;
import eu.isygoit.validator.KeyPolicyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KeyManagementService - Realistic User Stories")
class KeyManagementServiceTest {

    @Mock
    private KmsKeyRepository kmsKeyRepository;
    @Mock
    private KmsKeyVersionRepository kmsKeyVersionRepository;
    @Mock
    private KmsAliasRepository kmsAliasRepository;
    @Mock
    private KmsTagRepository kmsTagRepository;
    @Mock
    private KmsKeyPolicyRepository kmsKeyPolicyRepository;
    @Mock
    private CustomKeyStoreRepository customKeyStoreRepository;
    @Mock
    private ICryptoService cryptoService;
    @Mock
    private KeyPolicyValidator keyPolicyValidator;

    @InjectMocks
    private KeyManagementService keyManagementService;

    private final String tenant = "acme-corp";
    private final String keyId = "123e4567-e89b-12d3-a456-426614174000";

    private KmsKey createMockKey(IEnumKeyStatus.Types status, IEnumKeyOrigin.Types origin) {
        return KmsKey.builder()
                .tenant(tenant)
                .keyId(keyId)
                .keyWrn("wrn:wams:kms:us-east-1:" + tenant + ":key/" + keyId)
                .keySpec(IEnumKeySpec.Types.SYMMETRIC_DEFAULT)
                .keyUsage(IEnumKeyUsage.Types.ENCRYPT_DECRYPT)
                .keyStatus(status)
                .origin(origin)
                .rotationEnabled(false)
                .keyMaterial("encryptedKeyMaterial".getBytes())
                .publicKey(null)
                .primaryKeyAlias("alias:my-key")
                .description("Test key")
                .build();
    }

    // =========================================================================
    // User Story 1: Create a new KMS key
    // =========================================================================

    @Nested
    @DisplayName("Story 1: Create a new KMS key")
    class CreateKeyStory {

        @Test
        @DisplayName("Successfully create a symmetric key with default settings")
        void createSymmetricKey() {
            CreateKeyRequest request = CreateKeyRequest.builder()
                    .keySpec(IEnumKeySpec.Types.SYMMETRIC_DEFAULT)
                    .keyUsage(IEnumKeyUsage.Types.ENCRYPT_DECRYPT)
                    .description("Payment encryption key")
                    .keyAlias("alias:payment-key")
                    .origin(IEnumKeyOrigin.Types.WAMS_KMS)
                    .rotationEnabled(false)
                    .build();

            KeyPairMaterial keyMaterial = new KeyPairMaterial("aesKey".getBytes(), null);
            when(cryptoService.generateKeyMaterial(eq(IEnumKeySpec.Types.SYMMETRIC_DEFAULT)))
                    .thenReturn(keyMaterial);
            when(kmsKeyRepository.save(any(KmsKey.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(kmsKeyVersionRepository.save(any(KmsKeyVersion.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(kmsAliasRepository.save(any(KmsAlias.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            CreateKeyResponse response = keyManagementService.createKey(tenant, request);

            assertThat(response.getKeyMetadata().getKeyId()).isNotNull();
            assertThat(response.getKeyMetadata().getKeySpec()).isEqualTo(IEnumKeySpec.Types.SYMMETRIC_DEFAULT);
            assertThat(response.getKeyMetadata().getKeyStatus()).isEqualTo(IEnumKeyStatus.Types.ENABLED);
            assertThat(response.getKeyMetadata().getKeyAlias()).isEqualTo("alias:payment-key");

            verify(kmsKeyRepository).save(any(KmsKey.class));
            verify(kmsKeyVersionRepository).save(any(KmsKeyVersion.class));
            verify(kmsAliasRepository).save(any(KmsAlias.class));
        }

        @Test
        @DisplayName("Create a BYOK key – starts in PENDING_IMPORT state")
        void createByokKey() {
            CreateKeyRequest request = CreateKeyRequest.builder()
                    .keySpec(IEnumKeySpec.Types.RSA_2048)
                    .keyUsage(IEnumKeyUsage.Types.ENCRYPT_DECRYPT)
                    .origin(IEnumKeyOrigin.Types.EXTERNAL)
                    .expirationModel(IEnumKeyExpirationModel.Types.KEY_MATERIAL_EXPIRES)
                    .validTo(LocalDateTime.now().plusDays(365))
                    .build();

            KeyPairMaterial keyMaterial = new KeyPairMaterial("publicKey".getBytes(), "privateKey".getBytes());
            when(cryptoService.generateKeyMaterial(IEnumKeySpec.Types.RSA_2048)).thenReturn(keyMaterial);
            when(kmsKeyRepository.save(any(KmsKey.class))).thenAnswer(inv -> inv.getArgument(0));

            CreateKeyResponse response = keyManagementService.createKey(tenant, request);

            assertThat(response.getKeyMetadata().getKeyStatus()).isEqualTo(IEnumKeyStatus.Types.PENDING_IMPORT);
            assertThat(response.getKeyMetadata().getOrigin()).isEqualTo(IEnumKeyOrigin.Types.EXTERNAL);
        }
    }

    // =========================================================================
    // Story 2: Describe and list keys
    // =========================================================================

    @Nested
    @DisplayName("Story 2: Describe and list keys")
    class DescribeAndListKeysStory {

        @Test
        @DisplayName("Retrieve metadata of an existing key")
        void describeKey() {
            KmsKey key = createMockKey(IEnumKeyStatus.Types.ENABLED, IEnumKeyOrigin.Types.WAMS_KMS);
            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(key));
            when(kmsTagRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Collections.emptyList());
            when(kmsKeyPolicyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.empty());

            DescribeKeyResponse response = keyManagementService.describeKey(tenant, keyId, null);

            assertThat(response.getKeyMetadata().getKeyId()).isEqualTo(keyId);
            assertThat(response.getKeyMetadata().getKeyStatus()).isEqualTo(IEnumKeyStatus.Types.ENABLED);
        }

        @Test
        @DisplayName("List keys with pagination")
        void listKeys() {
            Page<KmsKey> page = new PageImpl<>(List.of(createMockKey(IEnumKeyStatus.Types.ENABLED, IEnumKeyOrigin.Types.WAMS_KMS)));
            when(kmsKeyRepository.findByTenant(eq(tenant), any(Pageable.class))).thenReturn(page);

            ListKeysResponse response = keyManagementService.listKeys(tenant, 10, null);

            assertThat(response.getKeys()).hasSize(1);
            assertThat(response.getTotalElements()).isEqualTo(1);
            assertThat(response.getTruncated()).isFalse();
        }
    }

    // =========================================================================
    // Story 3: Enable / Disable a key
    // =========================================================================

    @Nested
    @DisplayName("Story 3: Enable and disable a key")
    class EnableDisableKeyStory {

        @Test
        @DisplayName("Enable a disabled key")
        void enableKey() {
            KmsKey key = createMockKey(IEnumKeyStatus.Types.DISABLED, IEnumKeyOrigin.Types.WAMS_KMS);
            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(key));
            // Simulate that after saving, the key status becomes ENABLED
            when(kmsKeyRepository.save(any(KmsKey.class))).thenAnswer(inv -> {
                KmsKey saved = inv.getArgument(0);
                saved.setKeyStatus(IEnumKeyStatus.Types.ENABLED);
                return saved;
            });

            EnableKeyResponse response = keyManagementService.enableKey(tenant, keyId);

            assertThat(response.getKeyStatus()).isEqualTo(IEnumKeyStatus.Types.ENABLED);
        }

        @Test
        @DisplayName("Disable an enabled key")
        void disableKey() {
            KmsKey key = createMockKey(IEnumKeyStatus.Types.ENABLED, IEnumKeyOrigin.Types.WAMS_KMS);
            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(key));
            when(kmsKeyRepository.save(any(KmsKey.class))).thenAnswer(inv -> {
                KmsKey saved = inv.getArgument(0);
                saved.setKeyStatus(IEnumKeyStatus.Types.DISABLED);
                return saved;
            });

            DisableKeyResponse response = keyManagementService.disableKey(tenant, keyId);

            assertThat(response.getStatus()).isEqualTo(IEnumKeyStatus.Types.DISABLED);
        }

        @Test
        @DisplayName("Cannot disable a key that is pending deletion")
        void cannotDisablePendingDeletion() {
            KmsKey key = createMockKey(IEnumKeyStatus.Types.PENDING_DELETION, IEnumKeyOrigin.Types.WAMS_KMS);
            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(key));

            assertThatThrownBy(() -> keyManagementService.disableKey(tenant, keyId))
                    .isInstanceOf(InvalidKeyStateException.class)
                    .hasMessageContaining("Cannot disable key scheduled for deletion");
        }
    }

    // =========================================================================
    // Story 4: Schedule and cancel key deletion
    // =========================================================================

    @Nested
    @DisplayName("Story 4: Schedule and cancel key deletion")
    class KeyDeletionStory {

        @Test
        @DisplayName("Schedule key deletion with default window (7 days)")
        void scheduleDeletion() {
            KmsKey key = createMockKey(IEnumKeyStatus.Types.ENABLED, IEnumKeyOrigin.Types.WAMS_KMS);
            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(key));
            when(kmsKeyRepository.save(any(KmsKey.class))).thenAnswer(inv -> inv.getArgument(0));

            ScheduleKeyDeletionResponse response = keyManagementService.scheduleKeyDeletion(tenant, keyId, null);

            assertThat(response.getKeyStatus()).isEqualTo(IEnumKeyStatus.Types.PENDING_DELETION);
            assertThat(response.getPendingWindowInDays()).isEqualTo(7);
            assertThat(response.getDeletionDate()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("Cancel scheduled deletion")
        void cancelDeletion() {
            KmsKey key = createMockKey(IEnumKeyStatus.Types.PENDING_DELETION, IEnumKeyOrigin.Types.WAMS_KMS);
            key.setPendingDeletionWindowDays(7);
            key.setDeletionDate(LocalDateTime.now().plusDays(7));
            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(key));
            when(kmsKeyRepository.save(any(KmsKey.class))).thenAnswer(inv -> inv.getArgument(0));

            CancelKeyDeletionResponse response = keyManagementService.cancelKeyDeletion(tenant, keyId);

            assertThat(response.getKeyStatus()).isEqualTo(IEnumKeyStatus.Types.DISABLED);
        }
    }

    // =========================================================================
    // Story 5: Rotate a key
    // =========================================================================

    @Nested
    @DisplayName("Story 5: Rotate a key")
    class RotateKeyStory {

        @Test
        @DisplayName("Manually rotate a symmetric key")
        void rotateKey() {
            KmsKey key = createMockKey(IEnumKeyStatus.Types.ENABLED, IEnumKeyOrigin.Types.WAMS_KMS);
            key.setRotationEnabled(true);
            key.setRotationPeriodInDays(365);
            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(key));

            KeyPairMaterial newKeyMaterial = new KeyPairMaterial("newAesKey".getBytes(), null);
            when(cryptoService.generateKeyMaterial(key.getKeySpec())).thenReturn(newKeyMaterial);
            when(kmsKeyVersionRepository.save(any(KmsKeyVersion.class))).thenAnswer(inv -> inv.getArgument(0));
            when(kmsKeyRepository.save(any(KmsKey.class))).thenReturn(key);

            RotateKeyResponse response = keyManagementService.rotateKey(tenant, keyId);

            assertThat(response.getNewVersionId()).isNotNull();
            assertThat(response.getKeyId()).isEqualTo(keyId);
            verify(kmsKeyVersionRepository).save(any(KmsKeyVersion.class));
            verify(kmsKeyRepository).save(key);
            assertThat(key.getLastRotationDate()).isNotNull();
        }

        @Test
        @DisplayName("Cannot rotate an external (BYOK) key")
        void cannotRotateExternalKey() {
            KmsKey key = createMockKey(IEnumKeyStatus.Types.ENABLED, IEnumKeyOrigin.Types.EXTERNAL);
            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(key));

            assertThatThrownBy(() -> keyManagementService.rotateKey(tenant, keyId))
                    .isInstanceOf(OperationNotAllowedException.class)
                    .hasMessageContaining("Cannot rotate key with origin: EXTERNAL");
        }
    }

    // =========================================================================
    // Story 6: Update key description, alias, and rotation settings
    // =========================================================================

    @Nested
    @DisplayName("Story 6: Update key metadata")
    class UpdateKeyMetadataStory {

        @Test
        @DisplayName("Update description and alias")
        void updateDescriptionAndAlias() {
            KmsKey key = createMockKey(IEnumKeyStatus.Types.ENABLED, IEnumKeyOrigin.Types.WAMS_KMS);
            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(key));
            when(kmsKeyRepository.save(any(KmsKey.class))).thenReturn(key);
            when(kmsAliasRepository.findByTenantAndAliasName(eq(tenant), eq("alias:new-alias")))
                    .thenReturn(Optional.empty());
            when(kmsAliasRepository.save(any(KmsAlias.class))).thenAnswer(inv -> inv.getArgument(0));
            // No tag modifications → no call to existsByTenantAndKeyId – so we don't stub it

            UpdateKeyDescriptionRequest request = UpdateKeyDescriptionRequest.builder()
                    .description("Updated description")
                    .keyAlias("alias:new-alias")
                    .rotationEnabled(true)
                    .rotationPeriodInDays(180)
                    .build();

            UpdateKeyDescriptionResponse response = keyManagementService.updateKeyDescription(tenant, keyId, request);

            assertThat(response.getKeyMetadata().getDescription()).isEqualTo("Updated description");
            assertThat(response.getKeyMetadata().getKeyAlias()).isEqualTo("alias:new-alias");
            assertThat(response.getKeyMetadata().getRotationEnabled()).isTrue();
            assertThat(response.getKeyMetadata().getRotationPeriodInDays()).isEqualTo(180);
        }
    }

    // =========================================================================
    // Story 7: BYOK – Import key material
    // =========================================================================

    @Nested
    @DisplayName("Story 7: Bring Your Own Key (BYOK) – import key material")
    class ByokImportStory {

        @Test
        @DisplayName("Get parameters for import, then import key material")
        void importKeyMaterialFlow() throws Exception {
            // 1. Key exists in PENDING_IMPORT state
            KmsKey key = createMockKey(IEnumKeyStatus.Types.PENDING_IMPORT, IEnumKeyOrigin.Types.EXTERNAL);
            key.setKeyMaterial(null);
            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(key));

            // 2. getParametersForImport
            KeyPairMaterial wrappingKey = new KeyPairMaterial("publicKey".getBytes(), "privateKey".getBytes());
            byte[] importToken = "importToken123".getBytes();
            when(cryptoService.generateWrappingKey()).thenReturn(wrappingKey);
            when(cryptoService.generateImportToken()).thenReturn(importToken);
            // First save in getParametersForImport
            when(kmsKeyRepository.save(any(KmsKey.class))).thenReturn(key);

            ImportParametersResponse paramsResponse = keyManagementService.getParametersForImport(tenant, keyId);
            assertThat(paramsResponse.getImportToken()).isEqualTo(importToken);

            // 3. importKeyMaterial
            byte[] encryptedKeyMaterial = "encryptedMaterial".getBytes();
            when(cryptoService.decryptWithPrivateKey(any(), eq(encryptedKeyMaterial)))
                    .thenReturn("decryptedMaterial".getBytes());

            ImportKeyMaterialRequest importRequest = ImportKeyMaterialRequest.builder()
                    .importToken(Base64.getEncoder().encodeToString(importToken))
                    .encryptedKeyMaterial(Base64.getEncoder().encodeToString(encryptedKeyMaterial))
                    .validTo(LocalDateTime.now().plusDays(365))
                    .expirationModel(IEnumKeyExpirationModel.Types.KEY_MATERIAL_EXPIRES)
                    .build();

            // Second save inside importKeyMaterial
            when(kmsKeyRepository.save(any(KmsKey.class))).thenReturn(key);
            KeyDescriptionResponse importResponse = keyManagementService.importKeyMaterial(tenant, keyId, importRequest);

            assertThat(importResponse.getStatus()).isEqualTo(IEnumKeyStatus.Types.ENABLED);
            // Verify that save was called twice overall (not exactly once)
            verify(kmsKeyRepository, times(2)).save(any(KmsKey.class));
            assertThat(key.getKeyMaterial()).isEqualTo("decryptedMaterial".getBytes());
            assertThat(key.getImportToken()).isNull();
        }

        @Test
        @DisplayName("Delete imported key material (return to PENDING_IMPORT)")
        void deleteImportedMaterial() {
            KmsKey key = createMockKey(IEnumKeyStatus.Types.ENABLED, IEnumKeyOrigin.Types.EXTERNAL);
            key.setKeyMaterial("someMaterial".getBytes());
            key.setImportDate(LocalDateTime.now());
            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(key));
            when(kmsKeyRepository.save(any(KmsKey.class))).thenReturn(key);

            KeyDescriptionResponse response = keyManagementService.deleteImportedKeyMaterial(tenant, keyId);

            assertThat(response.getStatus()).isEqualTo(IEnumKeyStatus.Types.PENDING_IMPORT);
            assertThat(key.getKeyMaterial()).isNull();
            assertThat(key.getImportDate()).isNull();
        }
    }

    // =========================================================================
    // Story 8: Manage aliases
    // =========================================================================

    @Nested
    @DisplayName("Story 8: Manage aliases")
    class AliasManagementStory {

        @Test
        @DisplayName("Create a new alias pointing to a key")
        void createAlias() {
            CreateAliasRequest request = CreateAliasRequest.builder()
                    .aliasName("alias:my-new-alias")
                    .targetKeyId(keyId)
                    .build();

            KmsKey key = createMockKey(IEnumKeyStatus.Types.ENABLED, IEnumKeyOrigin.Types.WAMS_KMS);
            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(key));
            when(kmsAliasRepository.findByTenantAndAliasName(tenant, request.getAliasName()))
                    .thenReturn(Optional.empty());
            when(kmsAliasRepository.save(any(KmsAlias.class))).thenAnswer(inv -> inv.getArgument(0));

            AliasResponse response = keyManagementService.createAlias(tenant, request);

            assertThat(response.getAliasName()).isEqualTo("alias:my-new-alias");
            assertThat(response.getTargetKeyId()).isEqualTo(keyId);
        }

        @Test
        @DisplayName("Update an existing alias to point to another key")
        void updateAlias() {
            String aliasName = "alias:my-alias";
            String newTargetKeyId = "new-key-id";
            UpdateAliasRequest request = UpdateAliasRequest.builder()
                    .targetKeyId(newTargetKeyId)
                    .aliasName(aliasName)
                    .build();

            KmsAlias alias = KmsAlias.builder()
                    .tenant(tenant)
                    .aliasName(aliasName)
                    .targetKeyId(keyId)
                    .build();
            KmsKey newKey = createMockKey(IEnumKeyStatus.Types.ENABLED, IEnumKeyOrigin.Types.WAMS_KMS);
            newKey.setKeyId(newTargetKeyId);
            when(kmsAliasRepository.findByTenantAndAliasName(tenant, aliasName)).thenReturn(Optional.of(alias));
            when(kmsKeyRepository.findByTenantAndKeyId(tenant, newTargetKeyId)).thenReturn(Optional.of(newKey));
            when(kmsAliasRepository.save(any(KmsAlias.class))).thenReturn(alias);

            AliasResponse response = keyManagementService.updateAlias(tenant, aliasName, request);

            assertThat(response.getTargetKeyId()).isEqualTo(newTargetKeyId);
        }

        @Test
        @DisplayName("Delete an alias")
        void deleteAlias() {
            String aliasName = "alias:to-delete";
            KmsAlias alias = KmsAlias.builder().tenant(tenant).aliasName(aliasName).build();
            when(kmsAliasRepository.findByTenantAndAliasName(tenant, aliasName)).thenReturn(Optional.of(alias));

            keyManagementService.deleteAlias(tenant, aliasName);

            verify(kmsAliasRepository).delete(alias);
        }
    }

    // =========================================================================
    // Story 9: Manage tags
    // =========================================================================

    @Nested
    @DisplayName("Story 9: Manage tags on a key")
    class TagManagementStory {

        @Test
        @DisplayName("Add tags to a key")
        void tagResource() {
            KmsKey key = createMockKey(IEnumKeyStatus.Types.ENABLED, IEnumKeyOrigin.Types.WAMS_KMS);
            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(key));
            when(kmsTagRepository.save(any(KmsTag.class))).thenAnswer(inv -> inv.getArgument(0));

            TagResourceRequest request = TagResourceRequest.builder()
                    .tags(List.of(
                            ListResourceTagsResponse.Tag.builder().tagKey("Environment").tagValue("Production").build(),
                            ListResourceTagsResponse.Tag.builder().tagKey("CostCenter").tagValue("12345").build()
                    ))
                    .build();

            keyManagementService.tagResource(tenant, keyId, request);

            verify(kmsTagRepository, times(2)).save(any(KmsTag.class));
        }

        @Test
        @DisplayName("List tags for a key")
        void listResourceTags() {
            KmsTag tag1 = KmsTag.builder().tagKey("Env").tagValue("Prod").build();
            KmsTag tag2 = KmsTag.builder().tagKey("Team").tagValue("Security").build();
            Page<KmsTag> page = new PageImpl<>(List.of(tag1, tag2));
            when(kmsTagRepository.findByTenantAndKeyId(eq(tenant), eq(keyId), any(Pageable.class))).thenReturn(page);

            ListResourceTagsResponse response = keyManagementService.listResourceTags(tenant, keyId, 10, null);

            assertThat(response.getTags()).hasSize(2);
            assertThat(response.getTags().get(0).getTagKey()).isEqualTo("Env");
        }

        @Test
        @DisplayName("Remove tags from a key")
        void untagResource() {
            UntagResourceRequest request = UntagResourceRequest.builder()
                    .tagKeys(List.of("Environment", "CostCenter"))
                    .build();

            keyManagementService.untagResource(tenant, keyId, request);

            verify(kmsTagRepository).deleteByTenantAndKeyIdAndTagKeyIn(eq(tenant), eq(keyId), eq(request.getTagKeys()));
        }
    }

    // =========================================================================
    // Story 10: Key policies
    // =========================================================================

    @Nested
    @DisplayName("Story 10: Key policies")
    class KeyPolicyStory {

        @Test
        @DisplayName("Attach a policy to a key during creation")
        void createKeyWithPolicy() {
            Map<String, Object> policyMap = Map.of(
                    "Version", "2012-10-17",
                    "Statement", List.of(Map.of("Effect", "Allow", "Principal", "*", "Action", "kms:*", "Resource", "*"))
            );
            CreateKeyRequest request = CreateKeyRequest.builder()
                    .keySpec(IEnumKeySpec.Types.SYMMETRIC_DEFAULT)
                    .keyUsage(IEnumKeyUsage.Types.ENCRYPT_DECRYPT)
                    .policy(policyMap)
                    .bypassPolicyLockoutSafetyCheck(false)
                    .build();

            KeyPairMaterial keyMaterial = new KeyPairMaterial("aesKey".getBytes(), null);
            when(cryptoService.generateKeyMaterial(any())).thenReturn(keyMaterial);
            when(kmsKeyRepository.save(any(KmsKey.class))).thenAnswer(inv -> inv.getArgument(0));
            when(kmsKeyVersionRepository.save(any(KmsKeyVersion.class))).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(keyPolicyValidator).validatePolicyLockout(anyString(), anyBoolean(), anyString());
            when(kmsKeyPolicyRepository.save(any(KmsKeyPolicy.class))).thenAnswer(inv -> inv.getArgument(0));

            keyManagementService.createKey(tenant, request);

            verify(keyPolicyValidator).validatePolicyLockout(anyString(), eq(false), eq(tenant));
            verify(kmsKeyPolicyRepository).save(any(KmsKeyPolicy.class));
        }
    }

    // =========================================================================
    // Story 11: Key usage stats
    // =========================================================================

    @Nested
    @DisplayName("Story 11: Key usage statistics")
    class KeyUsageStatsStory {

        @Test
        @DisplayName("Retrieve usage counts and last used date")
        void getKeyUsageStats() {
            KmsKey key = createMockKey(IEnumKeyStatus.Types.ENABLED, IEnumKeyOrigin.Types.WAMS_KMS);
            when(kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)).thenReturn(Optional.of(key));
            when(cryptoService.getEncryptCount(tenant, keyId)).thenReturn(100L);
            when(cryptoService.getDecryptCount(tenant, keyId)).thenReturn(50L);
            when(cryptoService.getLastUsedDate(tenant, keyId)).thenReturn(LocalDateTime.now().minusDays(1));

            KeyUsageStatsResponse response = keyManagementService.getKeyUsageStats(tenant, keyId);

            assertThat(response.getEncryptCount()).isEqualTo(100L);
            assertThat(response.getDecryptCount()).isEqualTo(50L);
            assertThat(response.getLastUsedDate()).isNotNull();
        }
    }
}