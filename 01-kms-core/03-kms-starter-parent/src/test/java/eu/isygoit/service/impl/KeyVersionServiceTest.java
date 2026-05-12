package eu.isygoit.service.impl;

import eu.isygoit.dto.KmsDtos.ActiveVersionResponseDto;
import eu.isygoit.dto.KmsDtos.ListKeyVersionsResponse;
import eu.isygoit.enums.IEnumKeyExpirationModel;
import eu.isygoit.enums.IEnumKeyOrigin;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.model.KmsKeyVersion;
import eu.isygoit.repository.KmsKeyVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeyVersionServiceTest {

    private static final String TENANT = "tenant-1";
    private static final String KEY_ID = "key-1";

    @Mock
    private KmsKeyVersionRepository kmsKeyVersionRepository;

    @InjectMocks
    private KeyVersionServiceImpl service;

    private KmsKeyVersion version1;
    private KmsKeyVersion version2;

    @BeforeEach
    void setUp() {

        version1 = KmsKeyVersion.builder()
                .keyId(KEY_ID)
                .versionId("v1")
                .creationDate(LocalDateTime.now().minusDays(1))
                .keyStatus(IEnumKeyStatus.Types.ENABLED)
                .signingAlgorithm("RSA_SHA_256")
                .expirationModel(IEnumKeyExpirationModel.Types.NEVER_EXPIRES)
                .origin(IEnumKeyOrigin.Types.EXTERNAL)
                .build();

        version2 = KmsKeyVersion.builder()
                .keyId(KEY_ID)
                .versionId("v2")
                .creationDate(LocalDateTime.now())
                .keyStatus(IEnumKeyStatus.Types.ENABLED)
                .signingAlgorithm("RSA_SHA_512")
                .expirationModel(IEnumKeyExpirationModel.Types.ROTATION_BASED)
                .origin(IEnumKeyOrigin.Types.WAMS_KMS)
                .build();
    }

    @Test
    void shouldListKeyVersionsSuccessfully() {

        Page<KmsKeyVersion> page =
                new PageImpl<>(List.of(version1, version2));

        when(kmsKeyVersionRepository.findByTenantAndKeyId(
                eq(TENANT),
                eq(KEY_ID),
                any(Pageable.class)
        )).thenReturn(page);

        ListKeyVersionsResponse response =
                service.listKeyVersions(TENANT, KEY_ID, 10, null);

        assertNotNull(response);
        assertEquals(2, response.getVersions().size());
        assertFalse(response.getTruncated());
        assertNull(response.getNextMarker());

        ListKeyVersionsResponse.KeyVersion v1 =
                response.getVersions().get(0);

        assertEquals(KEY_ID, v1.getKeyId());
        assertEquals("v1", v1.getVersionId());
        assertEquals(IEnumKeyStatus.Types.ENABLED, v1.getStatus());
        assertEquals("RSA_SHA_256", v1.getSigningAlgorithm());
        assertEquals(IEnumKeyExpirationModel.Types.NEVER_EXPIRES, v1.getExpirationModel());
        assertEquals(IEnumKeyOrigin.Types.EXTERNAL, v1.getOrigin());
    }

    @Test
    void shouldReturnNextMarkerWhenPageHasNext() {

        Page<KmsKeyVersion> page =
                new PageImpl<>(
                        List.of(version1),
                        PageRequest.of(0, 1),
                        2
                );

        when(kmsKeyVersionRepository.findByTenantAndKeyId(
                eq(TENANT),
                eq(KEY_ID),
                any(Pageable.class)
        )).thenReturn(page);

        ListKeyVersionsResponse response =
                service.listKeyVersions(TENANT, KEY_ID, 1, null);

        assertTrue(response.getTruncated());
        assertEquals("1", response.getNextMarker());
    }

    @Test
    void shouldUseDefaultPageSizeWhenLimitNull() {

        Page<KmsKeyVersion> page =
                new PageImpl<>(List.of(version1));

        when(kmsKeyVersionRepository.findByTenantAndKeyId(
                eq(TENANT),
                eq(KEY_ID),
                any(Pageable.class)
        )).thenReturn(page);

        service.listKeyVersions(TENANT, KEY_ID, null, null);

        verify(kmsKeyVersionRepository).findByTenantAndKeyId(
                eq(TENANT),
                eq(KEY_ID),
                argThat(pageable -> pageable.getPageSize() == 100)
        );
    }

    @Test
    void shouldCapPageSizeTo1000() {

        Page<KmsKeyVersion> page =
                new PageImpl<>(List.of(version1));

        when(kmsKeyVersionRepository.findByTenantAndKeyId(
                eq(TENANT),
                eq(KEY_ID),
                any(Pageable.class)
        )).thenReturn(page);

        service.listKeyVersions(TENANT, KEY_ID, 5000, null);

        verify(kmsKeyVersionRepository).findByTenantAndKeyId(
                eq(TENANT),
                eq(KEY_ID),
                argThat(pageable -> pageable.getPageSize() == 1000)
        );
    }

    @Test
    void shouldUseMarkerAsPageNumber() {

        Page<KmsKeyVersion> page =
                new PageImpl<>(List.of(version1));

        when(kmsKeyVersionRepository.findByTenantAndKeyId(
                eq(TENANT),
                eq(KEY_ID),
                any(Pageable.class)
        )).thenReturn(page);

        service.listKeyVersions(TENANT, KEY_ID, 10, "2");

        verify(kmsKeyVersionRepository).findByTenantAndKeyId(
                eq(TENANT),
                eq(KEY_ID),
                argThat(pageable -> pageable.getPageNumber() == 2)
        );
    }

    @Test
    void shouldUseDescendingSortByCreationDate() {

        Page<KmsKeyVersion> page =
                new PageImpl<>(List.of(version1));

        when(kmsKeyVersionRepository.findByTenantAndKeyId(
                eq(TENANT),
                eq(KEY_ID),
                any(Pageable.class)
        )).thenReturn(page);

        service.listKeyVersions(TENANT, KEY_ID, 10, null);

        verify(kmsKeyVersionRepository).findByTenantAndKeyId(
                eq(TENANT),
                eq(KEY_ID),
                argThat(pageable -> {
                    Sort.Order order =
                            pageable.getSort().getOrderFor("creationDate");

                    return order != null
                            && order.getDirection() == Sort.Direction.DESC;
                })
        );
    }

    @Test
    void shouldReturnEmptyVersionsList() {

        Page<KmsKeyVersion> page =
                new PageImpl<>(List.of());

        when(kmsKeyVersionRepository.findByTenantAndKeyId(
                eq(TENANT),
                eq(KEY_ID),
                any(Pageable.class)
        )).thenReturn(page);

        ListKeyVersionsResponse response =
                service.listKeyVersions(TENANT, KEY_ID, 10, null);

        assertNotNull(response);
        assertTrue(response.getVersions().isEmpty());
        assertFalse(response.getTruncated());
        assertNull(response.getNextMarker());
    }

    @Test
    void shouldGetActiveVersionSuccessfully() {

        when(kmsKeyVersionRepository.findActiveVersionByKeyId(
                TENANT,
                KEY_ID
        )).thenReturn(Optional.of(version2));

        ActiveVersionResponseDto response =
                service.getActiveVersion(TENANT, KEY_ID);

        assertNotNull(response);
        assertEquals(KEY_ID, response.getKeyId());
        assertEquals("v2", response.getVersionId());
    }

    @Test
    void shouldThrowWhenNoActiveVersionFound() {

        when(kmsKeyVersionRepository.findActiveVersionByKeyId(
                TENANT,
                KEY_ID
        )).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.getActiveVersion(TENANT, KEY_ID)
        );

        assertEquals(
                "No active version found for key: " + KEY_ID,
                exception.getMessage()
        );
    }
}