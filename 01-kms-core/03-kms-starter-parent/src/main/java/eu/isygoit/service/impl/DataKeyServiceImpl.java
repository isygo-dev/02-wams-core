package eu.isygoit.service.impl;

import eu.isygoit.dto.request.GenerateDataKeyRequestDto;
import eu.isygoit.dto.response.DataKeyResponseDto;
import eu.isygoit.service.IDataKeyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.UUID;

/**
 * The type Data key service.
 */
@Slf4j
@Service
@Transactional
public class DataKeyServiceImpl implements IDataKeyService {

    @Override
    public DataKeyResponseDto generateDataKey(String tenant, GenerateDataKeyRequestDto request) {
        log.info("Generating data key for tenant: {} keyId: {} keySize: {}",
                tenant, request.getKeyId(), request.getKeySize());

        // Mock data key generation
        String plaintextKey = Base64.getEncoder().encodeToString(
                UUID.randomUUID().toString().getBytes()
        );

        String encryptedKey = Base64.getEncoder().encodeToString(
                ("encrypted-" + UUID.randomUUID().toString()).getBytes()
        );

        return DataKeyResponseDto.builder()
                .plaintextKey(plaintextKey)
                .encryptedKey(encryptedKey)
                .keyId(request.getKeyId())
                .build();
    }
}
