package eu.isygoit.service.impl;

import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.enums.IEnumKeyOrigin;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.exception.KeyNotFoundException;
import eu.isygoit.exception.KmsException;
import eu.isygoit.model.KmsKey;
import eu.isygoit.repository.KmsKeyRepository;
import eu.isygoit.service.IMultiRegionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MultiRegionService implements IMultiRegionService {

    private final KmsKeyRepository kmsKeyRepository;

    @Override
    public UpdatePrimaryRegionResponse updatePrimaryRegion(String tenant, String keyId, UpdatePrimaryRegionRequest request) {
        log.info("Updating primary region for key: {} to: {}", keyId, request.getPrimaryRegion());
        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        if (Boolean.FALSE.equals(key.getMultiRegion())) {
            throw new RuntimeException("Key is not a multi-region key");
        }

        key.setPrimaryRegion(request.getPrimaryRegion());
        key.validateBeforeSave();
        kmsKeyRepository.save(key);

        return UpdatePrimaryRegionResponse.builder().build();
    }

    @Override
    public ReplicateKeyResponse replicateKey(String tenant, String keyId, ReplicateKeyRequest request) {
        log.info("Replicating key: {} to region: {}", keyId, request.getReplicaRegion());
        if (!StringUtils.hasText(request.getReplicaRegion())) {
            throw new KmsException("Replica region must be specified");
        }
        // Find the primary multi-region key
        KmsKey primaryKey = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        if (Boolean.FALSE.equals(primaryKey.getMultiRegion())) {
            throw new KmsException("Key is not a multi-region key");
        }

        // Check if replica already exists in the target region
        boolean replicaExists = kmsKeyRepository.existsByTenantAndPrimaryKeyIdAndRegion(tenant, primaryKey.getKeyId(), request.getReplicaRegion());
        if (replicaExists) {
            throw new KmsException("Replica already exists in region: " + request.getReplicaRegion());
        }

        // Create a new key entity for the replica
        KmsKey replicaKey = new KmsKey();
        replicaKey.setTenant(tenant);
        replicaKey.setKeyId(UUID.randomUUID().toString());
        replicaKey.setKeyWrn(generateWrn(replicaKey.getKeyId(), request.getReplicaRegion()));
        replicaKey.setRegion(request.getReplicaRegion());
        replicaKey.setKeyStatus(IEnumKeyStatus.Types.ENABLED);
        replicaKey.setDescription(primaryKey.getDescription());
        replicaKey.setKeySpec(primaryKey.getKeySpec());
        replicaKey.setKeyUsage(primaryKey.getKeyUsage());
        replicaKey.setOrigin(primaryKey.getOrigin());
        replicaKey.setMultiRegion(true);
        replicaKey.setPrimaryKeyId(primaryKey.getKeyId());
        replicaKey.setKeyMaterial(primaryKey.getKeyMaterial());
        replicaKey.setImportDate(primaryKey.getImportDate());
        replicaKey.setValidTo(primaryKey.getValidTo());
        replicaKey.setExpirationModel(primaryKey.getExpirationModel());
        replicaKey.setRotationEnabled(false);
        replicaKey.setKeyStoreId(primaryKey.getKeyStoreId());

        replicaKey.validateBeforeSave();
        kmsKeyRepository.save(replicaKey);

        CreateKeyResponse.KeyMetadata replicaMetadata = CreateKeyResponse.KeyMetadata.builder()
                .tenant(tenant)
                .keyId(replicaKey.getKeyId())
                .wrn(replicaKey.getKeyWrn())
                .createDate(replicaKey.getCreateDate())
                .description(replicaKey.getDescription())
                .keySpec(replicaKey.getKeySpec())
                .keyUsage(replicaKey.getKeyUsage())
                .keyStatus(replicaKey.getKeyStatus())
                .origin(replicaKey.getOrigin())
                .multiRegion(replicaKey.getMultiRegion())
                .multiRegionConfiguration(replicaKey.isPrimaryKey() ? "PRIMARY" : "REPLICA")
                .build();

        return ReplicateKeyResponse.builder()
                .replicaKeyMetadata(replicaMetadata)
                .replicaRegion(request.getReplicaRegion())
                .build();
    }

    @Override
    public SynchronizeMultiRegionKeyResponse synchronizeMultiRegionKey(String tenant, String keyId) {
        log.info("Synchronizing multi-region key: {}", keyId);

        // Fetch the replica key
        KmsKey replicaKey = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        // Validate it's a replica
        if (replicaKey.getPrimaryKeyId() == null) {
            throw new KmsException("Key is not a multi-region replica. You can only synchronize replica keys.");
        }

        // Fetch the primary key
        KmsKey primaryKey = kmsKeyRepository.findByTenantAndKeyId(tenant, replicaKey.getPrimaryKeyId())
                .orElseThrow(() -> new KeyNotFoundException(replicaKey.getPrimaryKeyId()));

        // Synchronize shared properties based on WAMS KMS specification
        // 1. Key material (if origin is WAMS_KMS, copy the entire material)
        if (IEnumKeyOrigin.Types.WAMS_KMS.equals(primaryKey.getOrigin())) {
            replicaKey.setKeyMaterial(primaryKey.getKeyMaterial());
        }

        // 2. Key spec and encryption algorithms
        replicaKey.setKeySpec(primaryKey.getKeySpec());
        replicaKey.setKeyUsage(primaryKey.getKeyUsage());

        // 3. Automatic key rotation settings (only meaningful for primary, but replica inherits)
        replicaKey.setRotationEnabled(primaryKey.getRotationEnabled());
        replicaKey.setRotationPeriodInDays(primaryKey.getRotationPeriodInDays());

        // 4. For imported keys (EXTERNAL origin), sync key material identifier and description
        if (IEnumKeyOrigin.Types.EXTERNAL.equals(primaryKey.getOrigin())) {
            replicaKey.setImportDate(primaryKey.getImportDate());
            replicaKey.setValidTo(primaryKey.getValidTo());
            replicaKey.setExpirationModel(primaryKey.getExpirationModel());
            // Note: actual key material must be imported separately per WAMS requirements
        }

        // 5. Optional: synchronize description? WAMS says it's independent, but many implementations sync it
        // replicaKey.setDescription(primaryKey.getDescription()); // Uncomment if needed

        // Save the updated replica key
        replicaKey.validateBeforeSave();
        kmsKeyRepository.save(replicaKey);

        log.info("Successfully synchronized replica key {} with primary key {}", keyId, primaryKey.getKeyId());

        // Return empty response (WAMS KMS does not return any data)
        return SynchronizeMultiRegionKeyResponse.builder().build();
    }

    private String generateWrn(String keyId, String region) {
        return String.format("wrn:wams:kms:%s:123456789012:key:%s", region, keyId);
    }
}
