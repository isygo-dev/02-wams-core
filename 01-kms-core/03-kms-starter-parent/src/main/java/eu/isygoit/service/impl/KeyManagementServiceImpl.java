package eu.isygoit.service.impl;

import eu.isygoit.dto.request.CreateKeyRequestDto;
import eu.isygoit.dto.response.CreateKeyResponseDto;
import eu.isygoit.dto.response.KeyMetadataResponseDto;
import eu.isygoit.dto.response.ListKeysResponseDto;
import eu.isygoit.dto.response.RotateKeyResponseDto;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.exception.InvalidKeyStateException;
import eu.isygoit.exception.KeyNotFoundException;
import eu.isygoit.model.KmsKey;
import eu.isygoit.model.KmsKeyVersion;
import eu.isygoit.repository.KmsKeyRepository;
import eu.isygoit.repository.KmsKeyVersionRepository;
import eu.isygoit.service.ICryptoService;
import eu.isygoit.service.IKeyManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The type Key management service.
 * Implements AWS KMS-compliant key management operations
 */
@Slf4j
@Service
@Transactional
public class KeyManagementServiceImpl implements IKeyManagementService {

    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final int MIN_DELETION_WINDOW_DAYS = 7;
    private static final int MAX_DELETION_WINDOW_DAYS = 30;
    @Autowired
    private KmsKeyRepository kmsKeyRepository;
    @Autowired
    private KmsKeyVersionRepository kmsKeyVersionRepository;
    @Autowired
    private ICryptoService cryptoService;

    @Override
    public CreateKeyResponseDto createKey(String tenant, CreateKeyRequestDto request) {
        log.info("Creating key for tenant: {} with spec: {}", tenant, request.getKeySpec());

        Long keyId = "key-" + UUID.randomUUID().toString();
        String arn = String.format("arn:kms:service:%s:key/%s", tenant, keyId);
        String versionId = "v-" + UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            // Generate key material
            byte[] keyMaterial = cryptoService.generateKeyMaterial(request.getKeySpec());

            // Create and save KMS Key
            KmsKey key = KmsKey.builder()
                    .tenant(tenant)
                    .keyId(keyId)
                    .keyArn(arn)
                    .keySpec(request.getKeySpec())
                    .keyPurpose(request.getPurpose())
                    .keyAlias(request.getAlias())
                    .description(request.getDescription())
                    .status(IEnumKeyStatus.Types.ENABLED)
                    .currentVersionId(versionId)
                    .rotationEnabled(false)
                    .keyMaterial(keyMaterial)
                    .keyMaterialEncrypted(true)
                    .creationDate(now)
                    .build();

            KmsKey savedKey = kmsKeyRepository.save(key);

            // Create initial version
            KmsKeyVersion keyVersion = KmsKeyVersion.builder()
                    .tenant(tenant)
                    .keyId(keyId)
                    .versionId(versionId)
                    .status("ACTIVE")
                    .keyMaterial(keyMaterial)
                    .creationDate(now)
                    .activationDate(now)
                    .build();

            kmsKeyVersionRepository.save(keyVersion);

            log.info("Key created successfully: keyId={}, arn={}", keyId, arn);

            return CreateKeyResponseDto.builder()
                    .keyId(keyId)
                    .arn(arn)
                    .status(IEnumKeyStatus.Types.ENABLED)
                    .createdAt(now)
                    .build();
        } catch (Exception e) {
            log.error("Error creating key for tenant: {}", tenant, e);
            throw new RuntimeException("Failed to create key: " + e.getMessage(), e);
        }
    }

    @Override
    public KeyMetadataResponseDto getKeyMetadata(String tenant, Long keyId) {
        log.info("Getting key metadata for tenant: {} keyId: {}", tenant, keyId);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException("Key not found: " + keyId));

        return KeyMetadataResponseDto.builder()
                .keyId(key.getKeyId())
                .status(key.getStatus())
                .keySpec(key.getKeySpec())
                .keyPurpose(key.getKeyPurpose())
                .currentVersion(key.getCurrentVersionId())
                .createdAt(key.getCreationDate())
                .build();
    }

    @Override
    public ListKeysResponseDto listKeys(String tenant, Integer limit, String nextToken) {
        log.info("Listing keys for tenant: {} with limit: {}", tenant, limit);

        int pageSize = (limit != null && limit > 0) ? Math.min(limit, DEFAULT_PAGE_SIZE) : DEFAULT_PAGE_SIZE;
        int pageNum = (nextToken != null) ? Integer.parseInt(nextToken) : 0;

        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by("creationDate").descending());
        Page<KmsKey> keyPage = kmsKeyRepository.findByTenant(tenant, pageable);

        return ListKeysResponseDto.builder()
                .keys((List<ListKeysResponseDto.KeySummaryDto>) keyPage.getContent().stream()
                        .map(key -> ListKeysResponseDto.KeySummaryDto.builder()
                                .keyId(key.getKeyId())
                                .alias(key.getKeyAlias())
                                .status(key.getStatus())
                                .build())
                        .toList())
                .nextToken(keyPage.hasNext() ? String.valueOf(pageNum + 1) : null)
                .build();
    }

    @Override
    public KeyMetadataResponseDto enableKey(String tenant, Long keyId) {
        log.info("Enabling key: {} for tenant: {}", keyId, tenant);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException("Key not found: " + keyId));

        if (IEnumKeyStatus.Types.ENABLED.equals(key.getStatus())) {
            log.warn("Key {} is already enabled", keyId);
        } else {
            key.setStatus(IEnumKeyStatus.Types.ENABLED);
            kmsKeyRepository.save(key);
        }

        return KeyMetadataResponseDto.builder()
                .keyId(key.getKeyId())
                .status(key.getStatus())
                .build();
    }

    @Override
    public KeyMetadataResponseDto disableKey(String tenant, Long keyId) {
        log.info("Disabling key: {} for tenant: {}", keyId, tenant);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException("Key not found: " + keyId));

        if (IEnumKeyStatus.Types.PENDING_DELETION.equals(key.getStatus())) {
            throw new InvalidKeyStateException("Cannot disable key scheduled for deletion: " + keyId);
        }

        key.setStatus(IEnumKeyStatus.Types.DISABLED);
        kmsKeyRepository.save(key);

        return KeyMetadataResponseDto.builder()
                .keyId(key.getKeyId())
                .status(key.getStatus())
                .build();
    }

    @Override
    public KeyMetadataResponseDto scheduleKeyDeletion(String tenant, Long keyId, Integer pendingWindowInDays) {
        log.info("Scheduling deletion for key: {} for tenant: {} with pending window: {} days",
                keyId, tenant, pendingWindowInDays);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException("Key not found: " + keyId));

        int windowDays = (pendingWindowInDays != null) ? pendingWindowInDays : MIN_DELETION_WINDOW_DAYS;
        if (windowDays < MIN_DELETION_WINDOW_DAYS || windowDays > MAX_DELETION_WINDOW_DAYS) {
            throw new IllegalArgumentException(String.format("Pending window must be between %d and %d days",
                    MIN_DELETION_WINDOW_DAYS, MAX_DELETION_WINDOW_DAYS));
        }

        key.setStatus(IEnumKeyStatus.Types.PENDING_DELETION);
        key.setPendingDeletionWindowDays(windowDays);
        key.setDeletionDate(LocalDateTime.now().plusDays(windowDays));
        kmsKeyRepository.save(key);

        return KeyMetadataResponseDto.builder()
                .keyId(key.getKeyId())
                .status(key.getStatus())
                .build();
    }

    @Override
    public RotateKeyResponseDto rotateKey(String tenant, Long keyId) {
        log.info("Rotating key: {} for tenant: {}", keyId, tenant);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException("Key not found: " + keyId));

        if (!IEnumKeyStatus.Types.ENABLED.equals(key.getStatus())) {
            throw new InvalidKeyStateException("Cannot rotate key with status: " + key.getStatus());
        }

        String newVersionId = "v-" + UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            // Generate new key material for rotation
            byte[] newKeyMaterial = cryptoService.generateKeyMaterial(key.getKeySpec());

            // Create new version
            KmsKeyVersion newVersion = KmsKeyVersion.builder()
                    .tenant(tenant)
                    .keyId(keyId)
                    .versionId(newVersionId)
                    .status("ACTIVE")
                    .keyMaterial(newKeyMaterial)
                    .creationDate(now)
                    .activationDate(now)
                    .rotationDate(now)
                    .build();

            kmsKeyVersionRepository.save(newVersion);

            // Update key with new version
            key.setCurrentVersionId(newVersionId);
            key.setLastRotationDate(now);
            kmsKeyRepository.save(key);

            log.info("Key {} rotated successfully with new version {}", keyId, newVersionId);

            return RotateKeyResponseDto.builder()
                    .keyId(keyId)
                    .newVersion(newVersionId)
                    .rotationDate(now)
                    .build();
        } catch (Exception e) {
            log.error("Error rotating key: {}", keyId, e);
            throw new RuntimeException("Failed to rotate key: " + e.getMessage(), e);
        }
    }
}

