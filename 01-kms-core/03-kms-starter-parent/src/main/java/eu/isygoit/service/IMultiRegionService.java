package eu.isygoit.service;

import eu.isygoit.dto.KmsDtos.*;
import jakarta.validation.Valid;

public interface IMultiRegionService {

    UpdatePrimaryRegionResponse updatePrimaryRegion(String tenant, String keyId, @Valid UpdatePrimaryRegionRequestDto request);

    ReplicateKeyResponse replicateKey(String tenant, String keyId, @Valid ReplicateKeyRequestDto request);

    SynchronizeMultiRegionKeyResponse synchronizeMultiRegionKey(String tenant, String keyId);
}
