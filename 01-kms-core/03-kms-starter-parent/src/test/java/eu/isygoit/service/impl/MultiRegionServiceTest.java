package eu.isygoit.service.impl;

import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.enums.IEnumKeyOrigin;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.enums.IEnumKeyUsage;
import eu.isygoit.exception.KeyNotFoundException;
import eu.isygoit.exception.KmsException;
import eu.isygoit.model.KmsKey;
import eu.isygoit.repository.KmsKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class for MultiRegionService.
 *
 * Covers:
 * - Updating primary region for multi-region keys
 * - Replicating keys to different regions
 * - Synchronizing multi-region keys
 * - Error handling and validation
 */
@ExtendWith(MockitoExtension.class)
public class MultiRegionServiceTest {

    private static final String TENANT = "test-tenant";
    private static final String KEY_ID = "primary-key-id";
    private static final String REPLICA_REGION = "us-west-2";
    private static final String NEW_PRIMARY_REGION = "eu-west-1";

    @Mock
    private KmsKeyRepository kmsKeyRepository;

    @InjectMocks
    private MultiRegionService multiRegionService;

    private KmsKey primaryKey;
    private KmsKey replicaKey;

    @BeforeEach
    void setUp() {
        primaryKey = KmsKey.builder()
                .keyId(KEY_ID)
                .tenant(TENANT)
                .keyWrn("wrn:wams:kms:us-east-1:123456789012:key:" + KEY_ID)
                .region("us-east-1")
                .keySpec(IEnumKeySpec.Types.RSA_2048)
                .keyUsage(IEnumKeyUsage.Types.ENCRYPT_DECRYPT)
                .keyStatus(IEnumKeyStatus.Types.ENABLED)
                .multiRegion(true)
                .primaryRegion("us-east-1")
                .replicaRegions("us-west-1,eu-west-1")
                .keyMaterial(new byte[]{1, 2, 3, 4, 5})
                .origin(IEnumKeyOrigin.Types.WAMS_KMS)
                .rotationEnabled(false)
                .description("Primary multi-region key")
                .build();

        replicaKey = KmsKey.builder()
                .keyId(UUID.randomUUID().toString())
                .tenant(TENANT)
                .keyWrn("wrn:wams:kms:us-west-2:123456789012:key:replica-key")
                .region(REPLICA_REGION)
                .keySpec(IEnumKeySpec.Types.RSA_2048)
                .keyUsage(IEnumKeyUsage.Types.ENCRYPT_DECRYPT)
                .keyStatus(IEnumKeyStatus.Types.ENABLED)
                .multiRegion(true)
                .primaryKeyId(primaryKey.getKeyId())
                .keyMaterial(primaryKey.getKeyMaterial())
                .origin(IEnumKeyOrigin.Types.WAMS_KMS)
                .rotationEnabled(false)
                .description("Replica key")
                .build();
    }

    // ==============================================================
    // Test: updatePrimaryRegion method
    // ==============================================================

    @Test
    void shouldUpdatePrimaryRegionSuccessfully() {
        UpdatePrimaryRegionRequest request = UpdatePrimaryRegionRequest.builder()
                .primaryRegion(NEW_PRIMARY_REGION)
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(primaryKey));
        when(kmsKeyRepository.save(any(KmsKey.class)))
                .thenReturn(primaryKey);

        UpdatePrimaryRegionResponse response = multiRegionService.updatePrimaryRegion(TENANT, KEY_ID, request);

        assertNotNull(response);
        verify(kmsKeyRepository, times(1)).save(any(KmsKey.class));
        verify(kmsKeyRepository, times(1)).findByTenantAndKeyId(TENANT, KEY_ID);
    }

    @Test
    void shouldThrowExceptionWhenKeyNotFoundForUpdatePrimaryRegion() {
        UpdatePrimaryRegionRequest request = UpdatePrimaryRegionRequest.builder()
                .primaryRegion(NEW_PRIMARY_REGION)
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, "invalid-key-id"))
                .thenReturn(Optional.empty());

        assertThrows(KeyNotFoundException.class, () ->
                multiRegionService.updatePrimaryRegion(TENANT, "invalid-key-id", request)
        );
    }

    @Test
    void shouldThrowExceptionWhenKeyIsNotMultiRegion() {
        UpdatePrimaryRegionRequest request = UpdatePrimaryRegionRequest.builder()
                .primaryRegion(NEW_PRIMARY_REGION)
                .build();

        primaryKey.setMultiRegion(false);
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(primaryKey));

        assertThrows(RuntimeException.class, () ->
                multiRegionService.updatePrimaryRegion(TENANT, KEY_ID, request)
        );
    }

    // ==============================================================
    // Test: replicateKey method
    // ==============================================================

    @Test
    void shouldReplicateKeySuccessfully() {
        ReplicateKeyRequest request = ReplicateKeyRequest.builder()
                .replicaRegion(REPLICA_REGION)
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(primaryKey));
        when(kmsKeyRepository.existsByTenantAndPrimaryKeyIdAndRegion(TENANT, KEY_ID, REPLICA_REGION))
                .thenReturn(false);
        when(kmsKeyRepository.save(any(KmsKey.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReplicateKeyResponse response = multiRegionService.replicateKey(TENANT, KEY_ID, request);

        assertNotNull(response);
        assertNotNull(response.getReplicaKeyMetadata());
        assertNotNull(response.getReplicaKeyMetadata().getKeyId());
        assertEquals(REPLICA_REGION, response.getReplicaRegion());
        assertTrue(response.getReplicaKeyMetadata().getMultiRegion());
        assertEquals("REPLICA", response.getReplicaKeyMetadata().getMultiRegionConfiguration());
    }

    @Test
    void shouldThrowExceptionWhenPrimaryKeyNotFound() {
        ReplicateKeyRequest request = ReplicateKeyRequest.builder()
                .replicaRegion(REPLICA_REGION)
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, "invalid-key-id"))
                .thenReturn(Optional.empty());

        assertThrows(KeyNotFoundException.class, () ->
                multiRegionService.replicateKey(TENANT, "invalid-key-id", request)
        );
    }

    @Test
    void shouldThrowExceptionWhenKeyIsNotMultiRegionForReplication() {
        ReplicateKeyRequest request = ReplicateKeyRequest.builder()
                .replicaRegion(REPLICA_REGION)
                .build();

        primaryKey.setMultiRegion(false);
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(primaryKey));

        assertThrows(KmsException.class, () ->
                multiRegionService.replicateKey(TENANT, KEY_ID, request)
        );
    }

    @Test
    void shouldThrowExceptionWhenReplicaAlreadyExists() {
        ReplicateKeyRequest request = ReplicateKeyRequest.builder()
                .replicaRegion(REPLICA_REGION)
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(primaryKey));
        when(kmsKeyRepository.existsByTenantAndPrimaryKeyIdAndRegion(TENANT, KEY_ID, REPLICA_REGION))
                .thenReturn(true);

        assertThrows(KmsException.class, () ->
                multiRegionService.replicateKey(TENANT, KEY_ID, request)
        );
    }

    @Test
    void shouldReplicateKeyToMultipleRegions() {
        String[] regions = {"us-west-1", "eu-west-1", "ap-southeast-1"};
        for (String region : regions) {
            ReplicateKeyRequest request = ReplicateKeyRequest.builder()
                    .replicaRegion(region)
                    .build();

            when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                    .thenReturn(Optional.of(primaryKey));
            when(kmsKeyRepository.existsByTenantAndPrimaryKeyIdAndRegion(TENANT, KEY_ID, region))
                    .thenReturn(false);
            when(kmsKeyRepository.save(any(KmsKey.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ReplicateKeyResponse response = multiRegionService.replicateKey(TENANT, KEY_ID, request);

            assertNotNull(response);
            assertEquals(region, response.getReplicaRegion());
        }
    }

    // ==============================================================
    // Test: synchronizeMultiRegionKey method
    // ==============================================================

    @Test
    void shouldSynchronizeReplicaKeySuccessfully() {
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, replicaKey.getKeyId()))
                .thenReturn(Optional.of(replicaKey));
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, primaryKey.getKeyId()))
                .thenReturn(Optional.of(primaryKey));
        when(kmsKeyRepository.save(any(KmsKey.class)))
                .thenReturn(replicaKey);

        SynchronizeMultiRegionKeyResponse response = multiRegionService.synchronizeMultiRegionKey(TENANT, replicaKey.getKeyId());

        assertNotNull(response);
        verify(kmsKeyRepository, times(2)).findByTenantAndKeyId(any(), any());
        verify(kmsKeyRepository, times(1)).save(any(KmsKey.class));
    }

    @Test
    void shouldThrowExceptionWhenReplicaKeyNotFound() {
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, "invalid-replica-key"))
                .thenReturn(Optional.empty());

        assertThrows(KeyNotFoundException.class, () ->
                multiRegionService.synchronizeMultiRegionKey(TENANT, "invalid-replica-key")
        );
    }

    @Test
    void shouldThrowExceptionWhenKeyIsNotReplica() {
        primaryKey.setPrimaryKeyId(null); // Primary key has no primaryKeyId
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(primaryKey));

        assertThrows(KmsException.class, () ->
                multiRegionService.synchronizeMultiRegionKey(TENANT, KEY_ID)
        );
    }

    @Test
    void shouldThrowExceptionWhenPrimaryKeyNotFoundDuringSynchronization() {
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, replicaKey.getKeyId()))
                .thenReturn(Optional.of(replicaKey));
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, primaryKey.getKeyId()))
                .thenReturn(Optional.empty());

        assertThrows(KeyNotFoundException.class, () ->
                multiRegionService.synchronizeMultiRegionKey(TENANT, replicaKey.getKeyId())
        );
    }

    @Test
    void shouldSynchronizeKeyMaterialWhenOriginIsWamsKms() {
        primaryKey.setOrigin(IEnumKeyOrigin.Types.WAMS_KMS);
        replicaKey.setKeyMaterial(new byte[]{9, 8, 7});


        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, replicaKey.getKeyId()))
                .thenReturn(Optional.of(replicaKey));
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, primaryKey.getKeyId()))
                .thenReturn(Optional.of(primaryKey));
        when(kmsKeyRepository.save(any(KmsKey.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SynchronizeMultiRegionKeyResponse response = multiRegionService.synchronizeMultiRegionKey(TENANT, replicaKey.getKeyId());

        assertNotNull(response);
        assertEquals(replicaKey.getKeyMaterial(), primaryKey.getKeyMaterial());
    }

    @Test
    void shouldSynchronizeExternalKeyProperties() {
        primaryKey.setOrigin(IEnumKeyOrigin.Types.EXTERNAL);
        primaryKey.setImportDate(LocalDateTime.now());
        primaryKey.setValidTo(LocalDateTime.now().plusYears(1));

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, replicaKey.getKeyId()))
                .thenReturn(Optional.of(replicaKey));
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, primaryKey.getKeyId()))
                .thenReturn(Optional.of(primaryKey));
        when(kmsKeyRepository.save(any(KmsKey.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SynchronizeMultiRegionKeyResponse response = multiRegionService.synchronizeMultiRegionKey(TENANT, replicaKey.getKeyId());

        assertNotNull(response);
        assertTrue(replicaKey.getImported());
    }

    @Test
    void shouldSyncRotationSettings() {
        primaryKey.setRotationEnabled(true);
        primaryKey.setRotationPeriodInDays(90);

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, replicaKey.getKeyId()))
                .thenReturn(Optional.of(replicaKey));
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, primaryKey.getKeyId()))
                .thenReturn(Optional.of(primaryKey));
        when(kmsKeyRepository.save(any(KmsKey.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SynchronizeMultiRegionKeyResponse response = multiRegionService.synchronizeMultiRegionKey(TENANT, replicaKey.getKeyId());

        assertNotNull(response);
        assertTrue(replicaKey.getRotationEnabled());
        assertEquals(90, replicaKey.getRotationPeriodInDays());
    }

    @Test
    void shouldHandleNullPrimaryKeyIdInReplica() {
        replicaKey.setPrimaryKeyId(null);
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, replicaKey.getKeyId()))
                .thenReturn(Optional.of(replicaKey));

        assertThrows(KmsException.class, () ->
                multiRegionService.synchronizeMultiRegionKey(TENANT, replicaKey.getKeyId())
        );
    }

    @Test
    void shouldHandleMultipleSynchronizations() {
        for (int i = 0; i < 5; i++) {
            when(kmsKeyRepository.findByTenantAndKeyId(TENANT, replicaKey.getKeyId()))
                    .thenReturn(Optional.of(replicaKey));
            when(kmsKeyRepository.findByTenantAndKeyId(TENANT, primaryKey.getKeyId()))
                    .thenReturn(Optional.of(primaryKey));
            when(kmsKeyRepository.save(any(KmsKey.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            SynchronizeMultiRegionKeyResponse response = multiRegionService.synchronizeMultiRegionKey(TENANT, replicaKey.getKeyId());
            assertNotNull(response);
        }
    }

    @Test
    void shouldValidateReplicaKeyProperties() {

        // Mock setup
        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(primaryKey));
        when(kmsKeyRepository.existsByTenantAndPrimaryKeyIdAndRegion(TENANT, KEY_ID, REPLICA_REGION))
                .thenReturn(false);
        when(kmsKeyRepository.save(any(KmsKey.class)))
                .thenAnswer(invocation -> {
                    KmsKey saved = invocation.getArgument(0);
                    assertNotNull(saved.getKeyWrn());
                    assertEquals(IEnumKeyStatus.Types.ENABLED, saved.getKeyStatus());
                    return saved;
                });

        ReplicateKeyResponse finalResponse = multiRegionService.replicateKey(TENANT, KEY_ID,
                ReplicateKeyRequest.builder().replicaRegion(REPLICA_REGION).build());
        
        assertNotNull(finalResponse);
    }

    @Test
    void shouldHandleUpdatePrimaryRegionWithNullRegion() {
        UpdatePrimaryRegionRequest request = UpdatePrimaryRegionRequest.builder()
                .primaryRegion(null)
                .build();

        when(kmsKeyRepository.findByTenantAndKeyId(TENANT, KEY_ID))
                .thenReturn(Optional.of(primaryKey));
        when(kmsKeyRepository.save(any(KmsKey.class)))
                .thenReturn(primaryKey);

        UpdatePrimaryRegionResponse response = multiRegionService.updatePrimaryRegion(TENANT, KEY_ID, request);
        assertNotNull(response);
    }

    @Test
    void shouldHandleReplicateKeyWithNullRegion() {
        ReplicateKeyRequest request = ReplicateKeyRequest.builder()
                .replicaRegion(null)
                .build();

        KmsException exception = assertThrows(KmsException.class, () ->
                multiRegionService.replicateKey(TENANT, KEY_ID, request)
        );

        assertEquals("Replica region must be specified", exception.getMessage());

        verifyNoInteractions(kmsKeyRepository);
    }
}

