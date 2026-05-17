package eu.isygoit.service.impl;

import eu.isygoit.dto.KmsDtos.ActiveVersionResponse;
import eu.isygoit.dto.KmsDtos.ListKeyVersionsResponse;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.model.KmsKeyVersion;
import eu.isygoit.repository.KmsKeyVersionRepository;
import eu.isygoit.service.IKeyVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The type Key version service.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class KeyVersionService implements IKeyVersionService {

    private final KmsKeyVersionRepository kmsKeyVersionRepository;

    @Override
    public ListKeyVersionsResponse listKeyVersions(
            String tenant,
            String keyId,
            Integer limit,
            String nextToken) {

        log.info("Listing key versions for tenant: {} keyId: {}", tenant, keyId);

        int pageSize = (limit != null && limit > 0)
                ? Math.min(limit, 1000)
                : 100;

        int page = (nextToken != null) ? Integer.parseInt(nextToken) : 0;

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("createDate").descending());

        Page<KmsKeyVersion> versionPage =
                kmsKeyVersionRepository.findByTenantAndKeyId(
                        tenant,
                        keyId,
                        pageable
                );

        List<ListKeyVersionsResponse.KeyVersion> versions =
                versionPage.getContent().stream()
                        .map(v -> ListKeyVersionsResponse.KeyVersion.builder()
                                .keyId(String.valueOf(v.getKeyId()))
                                .versionId(v.getVersionId())
                                .createDate(v.getCreateDate())
                                .status(v.getKeyStatus())
                                .signingAlgorithm(v.getSigningAlgorithm())
                                .expirationModel(v.getExpirationModel())
                                .origin(v.getOrigin())
                                .build()
                        )
                        .toList();

        return ListKeyVersionsResponse.builder()
                .versions(versions)
                .nextToken(versionPage.hasNext() ? String.valueOf(page + 1) : null)
                .truncated(versionPage.hasNext())
                .build();
    }

    @Override
    public ActiveVersionResponse getActiveVersion(String tenant, String keyId) {
        log.info("Getting active version for tenant: {} keyId: {}", tenant, keyId);

        KmsKeyVersion activeVersion = kmsKeyVersionRepository
                .findFirstByTenantAndKeyIdAndKeyStatusOrderByCreateDateDesc(tenant, keyId, IEnumKeyStatus.Types.ENABLED)
                .orElseThrow(() -> new RuntimeException("No active version found for key: " + keyId));

        return ActiveVersionResponse.builder()
                .keyId(activeVersion.getKeyId())
                .versionId(activeVersion.getVersionId())
                .build();
    }
}

