package eu.isygoit.service;

import eu.isygoit.dto.KmsDtos.*;

/**
 * The interface Data key service.
 */
public interface IDataKeyService {

    /**
     * Generate data key.
     *
     * @param tenant  the tenant
     * @param request the request
     * @return the data key response dto
     */
    GenerateDataKeyResponse generateDataKey(
            String tenant,
            GenerateDataKeyRequest request);

    GenerateDataKeyWithoutPlaintextResponse generateDataKeyWithoutPlaintext(
            String tenant,
            GenerateDataKeyWithoutPlaintextRequest request);

    GenerateDataKeyPairResponse generateDataKeyPair(
            String tenant,
            GenerateDataKeyPairRequest request);

    GenerateDataKeyPairWithoutPlaintextResponse generateDataKeyPairWithoutPlaintext(
            String tenant,
            GenerateDataKeyPairWithoutPlaintextRequest request);

    GenerateRandomResponse generateRandom(GenerateRandomRequest request);
}

