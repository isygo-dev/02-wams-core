package eu.isygoit.service.impl;

import eu.isygoit.dto.KmsDtos.ActiveVersionResponse;
import eu.isygoit.dto.KmsDtos.DisableKeyVersionResponse;
import eu.isygoit.dto.KmsDtos.EnableKeyVersionResponse;
import eu.isygoit.dto.KmsDtos.ListKeyVersionsResponse;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.exception.InvalidKeyStateException;
import eu.isygoit.exception.KeyNotFoundException;
import eu.isygoit.model.KmsKeyVersion;
import eu.isygoit.repository.KmsKeyVersionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KeyVersionService - Realistic User Stories")
class KeyVersionServiceTest {

    private final String tenant = "acme-corp";
    private final String keyId = "123e4567-e89b-12d3-a456-426614174000";
    private final String versionId = "v-1a2b3c4d";
    private final String anotherVersionId = "v-5e6f7g8h";
    @Mock
    private KmsKeyVersionRepository kmsKeyVersionRepository;
    @InjectMocks
    private KeyVersionService keyVersionService;

    // Helper: create a mock KmsKeyVersion
    private KmsKeyVersion createMockVersion(String keyId, String versionId, IEnumKeyStatus.Types status) {
        return KmsKeyVersion.builder()
                .tenant(tenant)
                .keyId(keyId)
                .versionId(versionId)
                .keyStatus(status)
                .createDate(LocalDateTime.now())
                .keyMaterial("material".getBytes())
                .build();
    }

    // =========================================================================
    // User Story 1: List key versions (pagination)
    // =========================================================================

    @Nested
    @DisplayName("Story 1: List key versions")
    class ListKeyVersionsStory {

        @Test
        @DisplayName("Successfully list all versions of a key with pagination")
        void listKeyVersions() {
            // Given
            KmsKeyVersion version1 = createMockVersion(keyId, "v-1", IEnumKeyStatus.Types.ENABLED);
            KmsKeyVersion version2 = createMockVersion(keyId, "v-2", IEnumKeyStatus.Types.DISABLED);
            Page<KmsKeyVersion> page = new PageImpl<>(List.of(version1, version2));
            when(kmsKeyVersionRepository.findByTenantAndKeyId(eq(tenant), eq(keyId), any(Pageable.class)))
                    .thenReturn(page);

            // When
            ListKeyVersionsResponse response = keyVersionService.listKeyVersions(tenant, keyId, 10, null);

            // Then
            assertThat(response.getVersions()).hasSize(2);
            assertThat(response.getVersions().get(0).getVersionId()).isEqualTo("v-1");
            assertThat(response.getVersions().get(1).getVersionId()).isEqualTo("v-2");
            assertThat(response.getTotalElements()).isEqualTo(2);
            verify(kmsKeyVersionRepository).findByTenantAndKeyId(eq(tenant), eq(keyId), any(Pageable.class));
        }

        @Test
        @DisplayName("List versions when there are none")
        void listEmptyVersions() {
            Page<KmsKeyVersion> emptyPage = new PageImpl<>(List.of());
            when(kmsKeyVersionRepository.findByTenantAndKeyId(eq(tenant), eq(keyId), any(Pageable.class)))
                    .thenReturn(emptyPage);

            ListKeyVersionsResponse response = keyVersionService.listKeyVersions(tenant, keyId, 10, null);

            assertThat(response.getVersions()).isEmpty();
            assertThat(response.getTotalElements()).isZero();
        }
    }

    // =========================================================================
    // User Story 2: Disable a key version
    // =========================================================================

    @Nested
    @DisplayName("Story 2: Disable a key version")
    class DisableKeyVersionStory {

        @Test
        @DisplayName("Disable an enabled version successfully")
        void disableEnabledVersion() {
            // Given
            KmsKeyVersion version = createMockVersion(keyId, versionId, IEnumKeyStatus.Types.ENABLED);
            when(kmsKeyVersionRepository.findByTenantAndKeyIdAndVersionId(tenant, keyId, versionId))
                    .thenReturn(Optional.of(version));
            when(kmsKeyVersionRepository.save(any(KmsKeyVersion.class))).thenReturn(version);

            // When
            DisableKeyVersionResponse response = keyVersionService.disableKeyVersion(tenant, keyId, versionId);

            // Then
            assertThat(response.getKeyId()).isEqualTo(keyId);
            assertThat(response.getKeyVersionId()).isEqualTo(versionId);
            assertThat(response.getStatus()).isEqualTo(IEnumKeyStatus.Types.DISABLED);
            verify(kmsKeyVersionRepository).save(version);
        }

        @Test
        @DisplayName("Throw exception when trying to disable an already disabled version")
        void disableAlreadyDisabledVersion() {
            KmsKeyVersion version = createMockVersion(keyId, versionId, IEnumKeyStatus.Types.DISABLED);
            when(kmsKeyVersionRepository.findByTenantAndKeyIdAndVersionId(tenant, keyId, versionId))
                    .thenReturn(Optional.of(version));

            assertThatThrownBy(() -> keyVersionService.disableKeyVersion(tenant, keyId, versionId))
                    .isInstanceOf(InvalidKeyStateException.class)
                    .hasMessageContaining("Key already disabled");
            verify(kmsKeyVersionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throw exception when version does not exist")
        void disableNonExistentVersion() {
            when(kmsKeyVersionRepository.findByTenantAndKeyIdAndVersionId(tenant, keyId, versionId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> keyVersionService.disableKeyVersion(tenant, keyId, versionId))
                    .isInstanceOf(KeyNotFoundException.class);
        }
    }

    // =========================================================================
    // User Story 3: Enable a key version
    // =========================================================================

    @Nested
    @DisplayName("Story 3: Enable a key version")
    class EnableKeyVersionStory {

        @Test
        @DisplayName("Enable a disabled version successfully")
        void enableDisabledVersion() {
            KmsKeyVersion version = createMockVersion(keyId, versionId, IEnumKeyStatus.Types.DISABLED);
            when(kmsKeyVersionRepository.findByTenantAndKeyIdAndVersionId(tenant, keyId, versionId))
                    .thenReturn(Optional.of(version));
            when(kmsKeyVersionRepository.save(any(KmsKeyVersion.class))).thenReturn(version);

            EnableKeyVersionResponse response = keyVersionService.enableKeyVersion(tenant, keyId, versionId);

            assertThat(response.getStatus()).isEqualTo(IEnumKeyStatus.Types.ENABLED);
            verify(kmsKeyVersionRepository).save(version);
        }

        @Test
        @DisplayName("Throw exception when trying to enable an already enabled version")
        void enableAlreadyEnabledVersion() {
            KmsKeyVersion version = createMockVersion(keyId, versionId, IEnumKeyStatus.Types.ENABLED);
            when(kmsKeyVersionRepository.findByTenantAndKeyIdAndVersionId(tenant, keyId, versionId))
                    .thenReturn(Optional.of(version));

            assertThatThrownBy(() -> keyVersionService.enableKeyVersion(tenant, keyId, versionId))
                    .isInstanceOf(InvalidKeyStateException.class)
                    .hasMessageContaining("Key already enabled");
            verify(kmsKeyVersionRepository, never()).save(any());
        }
    }

    // =========================================================================
    // User Story 4: Get the active version of a key
    // =========================================================================

    @Nested
    @DisplayName("Story 4: Get the current active version")
    class GetActiveVersionStory {

        @Test
        @DisplayName("Get the active (latest enabled) version")
        void getActiveVersion() {
            KmsKeyVersion activeVersion = createMockVersion(keyId, "v-latest", IEnumKeyStatus.Types.ENABLED);
            when(kmsKeyVersionRepository.findFirstByTenantAndKeyIdAndKeyStatusOrderByCreateDateDesc(
                    tenant, keyId, IEnumKeyStatus.Types.ENABLED))
                    .thenReturn(Optional.of(activeVersion));

            ActiveVersionResponse response = keyVersionService.getActiveVersion(tenant, keyId);

            assertThat(response.getKeyId()).isEqualTo(keyId);
            assertThat(response.getVersionId()).isEqualTo("v-latest");
        }

        @Test
        @DisplayName("Throw exception when no active version exists")
        void noActiveVersionFound() {
            when(kmsKeyVersionRepository.findFirstByTenantAndKeyIdAndKeyStatusOrderByCreateDateDesc(
                    tenant, keyId, IEnumKeyStatus.Types.ENABLED))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> keyVersionService.getActiveVersion(tenant, keyId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("No active version found");
        }
    }

    // =========================================================================
    // User Story 5: Complete version lifecycle (enable/disable/activate)
    // =========================================================================

    @Nested
    @DisplayName("Story 5: Version lifecycle (realistic user journey)")
    class VersionLifecycleStory {

        @Test
        @DisplayName("User story: A key rotation creates a new version, which can be disabled and re-enabled")
        void versionLifecycle() {
            // 1. Initially, version v1 is active (enabled)
            KmsKeyVersion v1 = createMockVersion(keyId, "v1", IEnumKeyStatus.Types.ENABLED);
            when(kmsKeyVersionRepository.findFirstByTenantAndKeyIdAndKeyStatusOrderByCreateDateDesc(
                    tenant, keyId, IEnumKeyStatus.Types.ENABLED))
                    .thenReturn(Optional.of(v1));

            ActiveVersionResponse active = keyVersionService.getActiveVersion(tenant, keyId);
            assertThat(active.getVersionId()).isEqualTo("v1");

            // 2. Security team disables the old version v1 (e.g., due to compromise suspicion)
            when(kmsKeyVersionRepository.findByTenantAndKeyIdAndVersionId(tenant, keyId, "v1"))
                    .thenReturn(Optional.of(v1));
            when(kmsKeyVersionRepository.save(any())).thenReturn(v1);
            DisableKeyVersionResponse disableResp = keyVersionService.disableKeyVersion(tenant, keyId, "v1");
            assertThat(disableResp.getStatus()).isEqualTo(IEnumKeyStatus.Types.DISABLED);

            // 3. After investigation, the version is cleared and re-enabled
            v1.setKeyStatus(IEnumKeyStatus.Types.DISABLED);
            when(kmsKeyVersionRepository.findByTenantAndKeyIdAndVersionId(tenant, keyId, "v1"))
                    .thenReturn(Optional.of(v1));
            EnableKeyVersionResponse enableResp = keyVersionService.enableKeyVersion(tenant, keyId, "v1");
            assertThat(enableResp.getStatus()).isEqualTo(IEnumKeyStatus.Types.ENABLED);

            verify(kmsKeyVersionRepository, times(2)).save(v1);
        }
    }
}