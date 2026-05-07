package eu.isygoit.service;

import eu.isygoit.dto.request.ReplicateKeyRequestDto;
import eu.isygoit.dto.request.ReplicateKeyResponseDto;
import eu.isygoit.dto.request.UpdatePrimaryRegionRequestDto;
import eu.isygoit.dto.response.KeyMetadataResponseDto;
import jakarta.validation.Valid;

public interface IMultiRegionService {

    KeyMetadataResponseDto updatePrimaryRegion(String tenant, Long keyId, @Valid UpdatePrimaryRegionRequestDto request);

    ReplicateKeyResponseDto replicateKey(String tenant, Long keyId, @Valid ReplicateKeyRequestDto request);

    KeyMetadataResponseDto synchronizeMultiRegionKey(String tenant, Long keyId);
}
