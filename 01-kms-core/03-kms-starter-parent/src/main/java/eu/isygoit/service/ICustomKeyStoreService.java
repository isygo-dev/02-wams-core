package eu.isygoit.service;

import eu.isygoit.dto.request.CreateCustomKeyStoreRequestDto;
import eu.isygoit.dto.request.CustomKeyStoreResponseDto;
import eu.isygoit.dto.request.ListCustomKeyStoresResponseDto;
import eu.isygoit.dto.request.UpdateCustomKeyStoreRequestDto;
import jakarta.validation.Valid;

public interface ICustomKeyStoreService {

    CustomKeyStoreResponseDto createCustomKeyStore(String tenant, @Valid CreateCustomKeyStoreRequestDto request);

    CustomKeyStoreResponseDto describeCustomKeyStore(String tenant, String keyStoreId);

    CustomKeyStoreResponseDto updateCustomKeyStore(String tenant, String keyStoreId, @Valid UpdateCustomKeyStoreRequestDto request);

    void deleteCustomKeyStore(String tenant, String keyStoreId);

    ListCustomKeyStoresResponseDto listCustomKeyStores(String tenant, Integer limit, String nextToken);

    void connectCustomKeyStore(String tenant, String keyStoreId);

    void disconnectCustomKeyStore(String tenant, String keyStoreId);
}
