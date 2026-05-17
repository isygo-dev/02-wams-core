package eu.isygoit.service;

import eu.isygoit.dto.KmsDtos.CreateCustomKeyStoreRequest;
import eu.isygoit.dto.KmsDtos.CustomKeyStoreResponseDto;
import eu.isygoit.dto.KmsDtos.ListCustomKeyStoresResponseDto;
import eu.isygoit.dto.KmsDtos.UpdateCustomKeyStoreRequest;
import jakarta.validation.Valid;

import java.util.Map;

public interface ICustomKeyStoreService {

    CustomKeyStoreResponseDto createCustomKeyStore(String tenant, @Valid CreateCustomKeyStoreRequest request);

    CustomKeyStoreResponseDto describeCustomKeyStore(String tenant, Long keyStoreId);

    CustomKeyStoreResponseDto updateCustomKeyStore(String tenant, Long keyStoreId, @Valid UpdateCustomKeyStoreRequest request);

    void deleteCustomKeyStore(String tenant, Long keyStoreId);

    ListCustomKeyStoresResponseDto listCustomKeyStores(String tenant, Integer limit, String nextToken);

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
