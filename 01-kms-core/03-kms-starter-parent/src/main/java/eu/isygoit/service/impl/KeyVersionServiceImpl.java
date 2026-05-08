package eu.isygoit.service.impl;

import eu.isygoit.dto.response.ActiveVersionResponseDto;
import eu.isygoit.dto.response.KeyVersionListResponseDto;
import eu.isygoit.model.KmsKeyVersion;
import eu.isygoit.repository.KmsKeyVersionRepository;
import eu.isygoit.service.IKeyVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * The type Key version service.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class KeyVersionServiceImpl implements IKeyVersionService {

    private final KmsKeyVersionRepository kmsKeyVersionRepository;

    @Override
    public KeyVersionListResponseDto listKeyVersions(String tenant, Long keyId) {
        log.info("Listing key versions for tenant: {} keyId: {}", tenant, keyId);

        return KeyVersionListResponseDto.builder()
                .versions(kmsKeyVersionRepository.findVersionsByKeyId(tenant, keyId).stream()
                        .map(v -> KeyVersionListResponseDto.KeyVersionDto.builder()
                                .versionId(v.getVersionId())
                                .createdAt(v.getCreationDate())
                                .status(v.getStatus())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    public ActiveVersionResponseDto getActiveVersion(String tenant, Long keyId) {
        log.info("Getting active version for tenant: {} keyId: {}", tenant, keyId);

        KmsKeyVersion activeVersion = kmsKeyVersionRepository.findActiveVersionByKeyId(tenant, keyId)
                .orElseThrow(() -> new RuntimeException("No active version found for key: " + keyId));

        return ActiveVersionResponseDto.builder()
                .keyId(activeVersion.getKeyId())
                .versionId(activeVersion.getVersionId())
                .build();
    }
}

