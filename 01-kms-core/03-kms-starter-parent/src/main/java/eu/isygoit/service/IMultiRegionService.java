package eu.isygoit.service;

import eu.isygoit.dto.KmsDtos;
import eu.isygoit.dto.KmsDtos.ReplicateKeyResponse;
import eu.isygoit.dto.KmsDtos.SynchronizeMultiRegionKeyResponse;
import eu.isygoit.dto.KmsDtos.UpdatePrimaryRegionResponse;
import jakarta.validation.Valid;

public interface IMultiRegionService {

    UpdatePrimaryRegionResponse updatePrimaryRegion(String tenant, String keyId, @Valid KmsDtos.UpdatePrimaryRegionRequest request);

    ReplicateKeyResponse replicateKey(String tenant, String keyId, @Valid KmsDtos.ReplicateKeyRequest request);

    SynchronizeMultiRegionKeyResponse synchronizeMultiRegionKey(String tenant, String keyId);
}
