package eu.isygoit.service.impl;

import eu.isygoit.dto.response.KeyVersionListResponseDto;
import eu.isygoit.dto.response.ActiveVersionResponseDto;
import eu.isygoit.service.IKeyVersionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * The type Key version service.
 */
@Slf4j
@Service
@Transactional
public class KeyVersionServiceImpl implements IKeyVersionService {

    @Override
    public KeyVersionListResponseDto listKeyVersions(String tenant, String keyId) {
        log.info("Listing key versions for tenant: {} keyId: {}", tenant, keyId);

        return KeyVersionListResponseDto.builder()
                .versions(new ArrayList<>())
                .build();
    }

    @Override
    public ActiveVersionResponseDto getActiveVersion(String tenant, String keyId) {
        log.info("Getting active version for tenant: {} keyId: {}", tenant, keyId);

        return ActiveVersionResponseDto.builder()
                .versionId("v-1")
                .build();
    }
}

