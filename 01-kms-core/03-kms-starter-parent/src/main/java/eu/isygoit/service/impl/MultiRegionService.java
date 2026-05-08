package eu.isygoit.service.impl;

import eu.isygoit.dto.request.ReplicateKeyRequestDto;
import eu.isygoit.dto.request.UpdatePrimaryRegionRequestDto;
import eu.isygoit.dto.response.KeyMetadataResponseDto;
import eu.isygoit.dto.response.ReplicateKeyResponseDto;
import eu.isygoit.exception.KeyNotFoundException;
import eu.isygoit.mapper.KmsKeyMapper;
import eu.isygoit.model.KmsKey;
import eu.isygoit.repository.KmsKeyRepository;
import eu.isygoit.service.IMultiRegionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MultiRegionService implements IMultiRegionService {

    private final KmsKeyRepository kmsKeyRepository;
    private final KmsKeyMapper kmsKeyMapper;

    @Override
    public KeyMetadataResponseDto updatePrimaryRegion(String tenant, Long keyId, UpdatePrimaryRegionRequestDto request) {
        log.info("Updating primary region for key: {} to: {}", keyId, request.getPrimaryRegion());
        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        if (Boolean.FALSE.equals(key.getMultiRegion())) {
            throw new RuntimeException("Key is not a multi-region key");
        }

        key.setPrimaryRegion(request.getPrimaryRegion());
        kmsKeyRepository.save(key);

        return kmsKeyMapper.toKeyMetadataResponseDto(key);
    }

    @Override
    public ReplicateKeyResponseDto replicateKey(String tenant, Long keyId, ReplicateKeyRequestDto request) {
        log.info("Replicating key: {} to region: {}", keyId, request.getReplicaRegion());
        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        if (Boolean.FALSE.equals(key.getMultiRegion())) {
            throw new RuntimeException("Key is not a multi-region key");
        }

        // Simulating replication by adding to replicaRegions
        String currentReplicas = key.getReplicaRegions();
        if (currentReplicas == null || currentReplicas.isEmpty()) {
            key.setReplicaRegions(request.getReplicaRegion());
        } else if (!currentReplicas.contains(request.getReplicaRegion())) {
            key.setReplicaRegions(currentReplicas + "," + request.getReplicaRegion());
        }

        kmsKeyRepository.save(key);

        return ReplicateKeyResponseDto.builder()
                .primaryKeyId(key.getKeyId().toString())
                .replicaKeyId(key.getKeyId().toString()) // In real AWS, it's the same ID but in different region
                .primaryRegion(key.getPrimaryRegion())
                .replicaRegion(request.getReplicaRegion())
                .status("COMPLETED")
                .replicatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public KeyMetadataResponseDto synchronizeMultiRegionKey(String tenant, Long keyId) {
        log.info("Synchronizing multi-region key: {}", keyId);
        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        // Logic to sync metadata across regions would go here
        // For now, we just return the current state

        return kmsKeyMapper.toKeyMetadataResponseDto(key);
    }
}
