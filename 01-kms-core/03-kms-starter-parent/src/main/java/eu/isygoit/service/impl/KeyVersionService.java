package eu.isygoit.service.impl;

import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.exception.InvalidKeyStateException;
import eu.isygoit.exception.KeyNotFoundException;
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
    public DisableKeyVersionResponse disableKeyVersion(String tenant, String keyId, String keyVersionId) {
        log.info("Disabling key: {} for tenant: {}", keyId, tenant);

        KmsKeyVersion keyVersion = kmsKeyVersionRepository.findByTenantAndKeyIdAndVersionId(tenant, keyId, keyVersionId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        if (IEnumKeyStatus.Types.DISABLED.equals(keyVersion.getKeyStatus())) {
            throw new InvalidKeyStateException(
                    "Key already disabled " + keyId + " v: " + keyVersionId
            );
        }

        keyVersion.setKeyStatus(IEnumKeyStatus.Types.DISABLED);
        kmsKeyVersionRepository.save(keyVersion);

        return DisableKeyVersionResponse.builder()
                .keyId(keyVersion.getKeyId())
                .keyVersionId(keyVersion.getVersionId())
                .status(keyVersion.getKeyStatus())
                .build();
    }

    @Override
    public EnableKeyVersionResponse enableKeyVersion(String tenant, String keyId, String keyVersionId) {
        log.info("Enabling key: {} for tenant: {}", keyId, tenant);

        KmsKeyVersion keyVersion = kmsKeyVersionRepository.findByTenantAndKeyIdAndVersionId(tenant, keyId, keyVersionId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        if (IEnumKeyStatus.Types.ENABLED.equals(keyVersion.getKeyStatus())) {
            throw new InvalidKeyStateException(
                    "Key already enabled " + keyId + " v: " + keyVersionId
            );
        }

        keyVersion.setKeyStatus(IEnumKeyStatus.Types.ENABLED);
        kmsKeyVersionRepository.save(keyVersion);

        return EnableKeyVersionResponse.builder()
                .keyId(keyVersion.getKeyId())
                .keyVersionId(keyVersion.getVersionId())
                .status(keyVersion.getKeyStatus())
                .build();
    }

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
                                .deactivationDate(v.getDeactivationDate())
                                .validTo(v.getValidTo())
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

