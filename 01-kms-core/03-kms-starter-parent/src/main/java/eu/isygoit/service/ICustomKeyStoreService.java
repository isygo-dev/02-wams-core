package eu.isygoit.service;

import eu.isygoit.dto.KmsDtos.CreateCustomKeyStoreRequestDto;
import eu.isygoit.dto.KmsDtos.CustomKeyStoreResponseDto;
import eu.isygoit.dto.KmsDtos.ListCustomKeyStoresResponseDto;
import eu.isygoit.dto.KmsDtos.UpdateCustomKeyStoreRequestDto;
import jakarta.validation.Valid;

public interface ICustomKeyStoreService {

    CustomKeyStoreResponseDto createCustomKeyStore(String tenant, @Valid CreateCustomKeyStoreRequestDto request);

    CustomKeyStoreResponseDto describeCustomKeyStore(String tenant, Long keyStoreId);

    CustomKeyStoreResponseDto updateCustomKeyStore(String tenant, Long keyStoreId, @Valid UpdateCustomKeyStoreRequestDto request);

    void deleteCustomKeyStore(String tenant, Long keyStoreId);

    ListCustomKeyStoresResponseDto listCustomKeyStores(String tenant, Integer limit, String nextToken);

    void connectCustomKeyStore(String tenant, Long keyStoreId);

    void disconnectCustomKeyStore(String tenant, Long keyStoreId);
}
