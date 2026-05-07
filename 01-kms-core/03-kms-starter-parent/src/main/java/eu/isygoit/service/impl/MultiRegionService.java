package eu.isygoit.service.impl;

import eu.isygoit.dto.request.ReplicateKeyRequestDto;
import eu.isygoit.dto.request.UpdatePrimaryRegionRequestDto;
import eu.isygoit.dto.response.KeyMetadataResponseDto;
import eu.isygoit.dto.response.ReplicateKeyResponseDto;
import eu.isygoit.service.IMultiRegionService;
import org.springframework.stereotype.Service;

@Service
public class MultiRegionService implements IMultiRegionService {

    @Override
    public KeyMetadataResponseDto updatePrimaryRegion(String tenant, Long keyId, UpdatePrimaryRegionRequestDto request) {
        return null;
    }

    @Override
    public ReplicateKeyResponseDto replicateKey(String tenant, Long keyId, ReplicateKeyRequestDto request) {
        return null;
    }

    @Override
    public KeyMetadataResponseDto synchronizeMultiRegionKey(String tenant, Long keyId) {
        return null;
    }
}
