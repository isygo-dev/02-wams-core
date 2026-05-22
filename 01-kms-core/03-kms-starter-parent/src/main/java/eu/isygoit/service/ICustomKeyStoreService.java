package eu.isygoit.service;

import eu.isygoit.dto.KmsDtos.CreateCustomKeyStoreRequest;
import eu.isygoit.dto.KmsDtos.DescribeCustomKeyStoreResponse;
import eu.isygoit.dto.KmsDtos.ListCustomKeyStoresResponse;
import eu.isygoit.dto.KmsDtos.UpdateCustomKeyStoreRequest;
import jakarta.validation.Valid;

import java.util.Map;

public interface ICustomKeyStoreService {

    DescribeCustomKeyStoreResponse.CustomKeyStore createCustomKeyStore(String tenant, @Valid CreateCustomKeyStoreRequest request);

    DescribeCustomKeyStoreResponse.CustomKeyStore describeCustomKeyStore(String tenant, Long keyStoreId);

    DescribeCustomKeyStoreResponse.CustomKeyStore updateCustomKeyStore(String tenant, Long keyStoreId, @Valid UpdateCustomKeyStoreRequest request);

    void deleteCustomKeyStore(String tenant, Long keyStoreId);

    ListCustomKeyStoresResponse listCustomKeyStores(String tenant, Integer limit, String nextToken);

    void connectCustomKeyStore(String tenant, Long keyStoreId);

    void disconnectCustomKeyStore(String tenant, Long keyStoreId);

    byte[] encrypt(String tenant, Long keyStoreId, String keyId, byte[] plaintext,
                   Map<String, String> encryptionContext) throws Exception;

    byte[] decrypt(String tenant, Long keyStoreId, String keyId, byte[] ciphertext,
                   Map<String, String> encryptionContext) throws Exception;

    byte[] sign(String tenant, Long keyStoreId, String keyId, byte[] message, String algorithm) throws Exception;

    boolean verify(String tenant, Long keyStoreId, String keyId, byte[] message, byte[] signature, String algorithm)
            throws Exception;
}
