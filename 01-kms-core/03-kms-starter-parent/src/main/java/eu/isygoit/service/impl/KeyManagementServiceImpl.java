package eu.isygoit.service.impl;

import eu.isygoit.dto.data.TagDto;
import eu.isygoit.dto.request.*;
import eu.isygoit.dto.response.*;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.exception.InvalidKeyStateException;
import eu.isygoit.exception.KeyNotFoundException;
import eu.isygoit.model.KmsAlias;
import eu.isygoit.model.KmsKey;
import eu.isygoit.model.KmsKeyVersion;
import eu.isygoit.model.KmsTag;
import eu.isygoit.repository.KmsAliasRepository;
import eu.isygoit.repository.KmsKeyRepository;
import eu.isygoit.repository.KmsKeyVersionRepository;
import eu.isygoit.repository.KmsTagRepository;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Autowired
    private KmsAliasRepository kmsAliasRepository;

    @Autowired
    private KmsTagRepository kmsTagRepository;

    @Override
    public CreateKeyResponseDto createKey(String tenant, CreateKeyRequestDto request) {
        log.info("Creating key for tenant: {} with spec: {}", tenant, request.getKeySpec());

        String arn = String.format("arn:kms:service:%s:key/%s", tenant, UUID.randomUUID().toString());
        String versionId = "v-" + UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            // Generate key material
            byte[] keyMaterial = cryptoService.generateKeyMaterial(request.getKeySpec());

            // Create and save KMS Key
            KmsKey key = KmsKey.builder()
                    .tenant(tenant)
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
                    .keyId(savedKey.getKeyId())
                    .versionId(versionId)
                    .status("ACTIVE")
                    .keyMaterial(keyMaterial)
                    .creationDate(now)
                    .activationDate(now)
                    .build();

            kmsKeyVersionRepository.save(keyVersion);

            log.info("Key created successfully: keyId={}, arn={}", keyVersion.getKeyId(), arn);

            return CreateKeyResponseDto.builder()
                    .arn(arn)
                    .keyId(keyVersion.getKeyId())
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
                .orElseThrow(() -> new KeyNotFoundException(keyId));

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
                .orElseThrow(() -> new KeyNotFoundException(keyId));

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
                .orElseThrow(() -> new KeyNotFoundException(keyId));

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
                .orElseThrow(() -> new KeyNotFoundException(keyId));

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
                .orElseThrow(() -> new KeyNotFoundException(keyId));

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

    @Override
    public KeyMetadataResponseDto updateKeyMetadata(String tenant, Long keyId, UpdateKeyMetadataRequestDto request) {
        log.info("Updating key metadata for tenant: {} keyId: {}", tenant, keyId);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        if (request.getAlias() != null) {
            key.setKeyAlias(request.getAlias());
        }
        if (request.getDescription() != null) {
            key.setDescription(request.getDescription());
        }

        kmsKeyRepository.save(key);

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
    public KeyMetadataResponseDto cancelKeyDeletion(String tenant, Long keyId) {
        log.info("Cancelling deletion for key: {} for tenant: {}", keyId, tenant);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        if (!IEnumKeyStatus.Types.PENDING_DELETION.equals(key.getStatus())) {
            throw new InvalidKeyStateException("Key is not pending deletion: " + keyId);
        }

        key.setStatus(IEnumKeyStatus.Types.DISABLED);
        key.setPendingDeletionWindowDays(null);
        key.setDeletionDate(null);
        kmsKeyRepository.save(key);

        return KeyMetadataResponseDto.builder()
                .keyId(key.getKeyId())
                .status(key.getStatus())
                .build();
    }

    @Override
    public KeyRotationStatusResponseDto updateKeyRotation(String tenant, Long keyId, UpdateKeyRotationRequestDto request) {
        log.info("Updating key rotation for tenant: {} keyId: {} autoRotate: {}",
                tenant, keyId, request.getEnableRotation());

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        key.setRotationEnabled(request.getEnableRotation());
        if (request.getRotationPeriodDays() != null) {
            key.setRotationPeriodDays(request.getRotationPeriodDays());
        }

        kmsKeyRepository.save(key);

        return KeyRotationStatusResponseDto.builder()
                .keyId(keyId)
                .rotationEnabled(key.getRotationEnabled())
                .rotationPeriodDays(key.getRotationPeriodDays())
                .lastRotationDate(key.getLastRotationDate())
                .build();
    }

    @Override
    public KeyRotationStatusResponseDto getKeyRotationStatus(String tenant, Long keyId) {
        log.info("Getting key rotation status for tenant: {} keyId: {}", tenant, keyId);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        return KeyRotationStatusResponseDto.builder()
                .keyId(keyId)
                .rotationEnabled(key.getRotationEnabled())
                .rotationPeriodDays(key.getRotationPeriodDays())
                .lastRotationDate(key.getLastRotationDate())
                .build();
    }

    @Override
    public PublicKeyResponseDto getPublicKey(String tenant, Long keyId) {
        log.info("Getting public key for tenant: {} keyId: {}", tenant, keyId);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        // Extract public key from key material (implementation depends on key spec)
        byte[] publicKey = cryptoService.extractPublicKey(key.getKeyMaterial(), key.getKeySpec());

        return PublicKeyResponseDto.builder()
                .keyId(keyId)
                .publicKey(Arrays.toString(publicKey))
                .keySpec(key.getKeySpec())
                .build();
    }

    @Override
    public AliasResponseDto createAlias(String tenant, CreateAliasRequestDto request) {
        log.info("Creating alias: {} for tenant: {} keyId: {}",
                request.getAliasName(), tenant, request.getTargetKeyId());

        // Check if alias already exists
        if (kmsAliasRepository.findByTenantAndAliasName(tenant, request.getAliasName()).isPresent()) {
            throw new IllegalArgumentException("Alias already exists: " + request.getAliasName());
        }

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, request.getTargetKeyId())
                .orElseThrow(() -> new KeyNotFoundException(request.getTargetKeyId()));

        KmsAlias alias = KmsAlias.builder()
                .tenant(tenant)
                .aliasName(request.getAliasName())
                .keyId(key.getKeyId())
                .build();

        KmsAlias savedAlias = kmsAliasRepository.save(alias);

        return AliasResponseDto.builder()
                .aliasName(savedAlias.getAliasName())
                .targetKeyId(savedAlias.getId())
                .build();
    }

    @Override
    public AliasResponseDto updateAlias(String tenant, String aliasName, UpdateAliasRequestDto request) {
        log.info("Updating alias: {} for tenant: {} to keyId: {}", aliasName, tenant, request.getTargetKeyId());

        KmsAlias alias = kmsAliasRepository.findByTenantAndAliasName(tenant, aliasName)
                .orElseThrow(() -> new IllegalArgumentException("Alias not found: " + aliasName));

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, request.getTargetKeyId())
                .orElseThrow(() -> new KeyNotFoundException(request.getTargetKeyId()));

        alias.setKeyId(key.getKeyId());
        kmsAliasRepository.save(alias);

        return AliasResponseDto.builder()
                .aliasName(alias.getAliasName())
                .targetKeyId(alias.getKeyId())
                .build();
    }

    @Override
    public void deleteAlias(String tenant, String aliasName) {
        log.info("Deleting alias: {} for tenant: {}", aliasName, tenant);

        KmsAlias alias = kmsAliasRepository.findByTenantAndAliasName(tenant, aliasName)
                .orElseThrow(() -> new IllegalArgumentException("Alias not found: " + aliasName));

        kmsAliasRepository.delete(alias);
    }

    @Override
    public ListAliasesResponseDto listAliases(String tenant, Integer limit, String nextToken) {
        log.info("Listing aliases for tenant: {} with limit: {}", tenant, limit);

        int pageSize = (limit != null && limit > 0) ? Math.min(limit, DEFAULT_PAGE_SIZE) : DEFAULT_PAGE_SIZE;
        int pageNum = (nextToken != null) ? Integer.parseInt(nextToken) : 0;

        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Page<KmsAlias> aliasPage = kmsAliasRepository.findByTenant(tenant, pageable);

        return ListAliasesResponseDto.builder()
                .aliases(aliasPage.getContent().stream()
                        .map(alias -> AliasResponseDto.builder()
                                .aliasName(alias.getAliasName())
                                .targetKeyId(alias.getKeyId())
                                .build())
                        .collect(Collectors.toList()))
                .nextToken(aliasPage.hasNext() ? String.valueOf(pageNum + 1) : null)
                .build();
    }

    @Override
    public ListAliasesResponseDto listAliasesForKey(String tenant, Long keyId) {
        log.info("Listing aliases for key: {} tenant: {}", keyId, tenant);

        List<KmsAlias> aliases = kmsAliasRepository.findByTenantAndKeyId(tenant, keyId);

        return ListAliasesResponseDto.builder()
                .aliases(aliases.stream()
                        .map(alias -> AliasResponseDto.builder()
                                .aliasName(alias.getAliasName())
                                .targetKeyId(alias.getKeyId())
                                .build())
                        .collect(Collectors.toList()))
                .nextToken(null)
                .build();
    }

    @Override
    public Object tagResource(String tenant, Long keyId, TagResourceRequestDto request) {
        log.info("Tagging resource for tenant: {} keyId: {}", tenant, keyId);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        for (String tagKey : request.getTags().keySet()) {
            KmsTag kmsTag = KmsTag.builder()
                    .tenant(tenant)
                    .keyId(keyId)
                    .tagKey(tagKey)
                    .tagValue(request.getTags().get(tagKey))
                    .build();
            kmsTagRepository.save(kmsTag);
        }

        return new Object(); // Return empty response
    }

    @Override
    public Object untagResource(String tenant, Long keyId, UntagResourceRequestDto request) {
        log.info("Untagging resource for tenant: {} keyId: {} tags: {}", tenant, keyId, request.getTagKeys());

        kmsTagRepository.deleteByTenantAndKeyIdAndTagKeyIn(tenant, keyId, request.getTagKeys());

        return new Object(); // Return empty response
    }

    @Override
    public ListTagsResponseDto listResourceTags(String tenant, Long keyId) {
        log.info("Listing resource tags for tenant: {} keyId: {}", tenant, keyId);

        List<KmsTag> tags = kmsTagRepository.findByTenantAndKeyId(tenant, keyId);

        return ListTagsResponseDto.builder()
                .tags(tags.stream()
                        .map(tag -> TagDto.builder()
                                .tagKey(tag.getTagKey())
                                .tagValue(tag.getTagValue())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    public ImportParametersResponseDto getParametersForImport(String tenant, Long keyId) {
        log.info("Getting import parameters for tenant: {} keyId: {}", tenant, keyId);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        // Generate import parameters (wrapping key and token)
        byte[] wrappingKey = cryptoService.generateWrappingKey();
        byte[] importToken = cryptoService.generateImportToken();

        return ImportParametersResponseDto.builder()
                .keyId(keyId)
                .wrappingKey(wrappingKey)
                .importToken(importToken)
                .validityPeriodHours(24)
                .build();
    }

    @Override
    public KeyMetadataResponseDto importKeyMaterial(String tenant, Long keyId, ImportKeyMaterialRequestDto request) {
        log.info("Importing key material for tenant: {} keyId: {}", tenant, keyId);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        // Decrypt the imported key material
        byte[] decryptedMaterial = cryptoService.decryptKeyMaterial(
                request.getEncryptedKeyMaterial(),
                request.getImportToken()
        );

        key.setKeyMaterial(decryptedMaterial);
        key.setKeyMaterialEncrypted(true);
        key.setImported(true);
        key.setImportDate(LocalDateTime.now());
        key.setExpirationDate(request.getExpirationDate());
        kmsKeyRepository.save(key);

        return KeyMetadataResponseDto.builder()
                .keyId(key.getKeyId())
                .status(key.getStatus())
                .build();
    }

    @Override
    public KeyMetadataResponseDto deleteImportedKeyMaterial(String tenant, Long keyId) {
        log.info("Deleting imported key material for tenant: {} keyId: {}", tenant, keyId);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        if (!key.getImported()) {
            throw new InvalidKeyStateException("Key material was not imported: " + keyId);
        }

        key.setKeyMaterial(null);
        key.setKeyMaterialEncrypted(false);
        kmsKeyRepository.save(key);

        return KeyMetadataResponseDto.builder()
                .keyId(key.getKeyId())
                .status(key.getStatus())
                .build();
    }

    @Override
    public void validateKey(String tenant, Long keyId) {
        log.info("Validating key for tenant: {} keyId: {}", tenant, keyId);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        if (IEnumKeyStatus.Types.PENDING_DELETION.equals(key.getStatus())) {
            throw new InvalidKeyStateException("Key is pending deletion: " + keyId);
        }

        if (key.getKeyMaterial() == null) {
            throw new InvalidKeyStateException("Key has no key material: " + keyId);
        }

        // Validate key integrity
        boolean isValid = cryptoService.validateKeyIntegrity(key.getKeyMaterial(), key.getKeySpec());
        if (!isValid) {
            throw new InvalidKeyStateException("Key integrity validation failed: " + keyId);
        }
    }

    @Override
    public void deleteKey(String tenant, Long keyId) {
        log.info("Deleting key for tenant: {} keyId: {}", tenant, keyId);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        // Delete associated versions, aliases, and tags
        kmsKeyVersionRepository.deleteByTenantAndKeyId(tenant, keyId);
        kmsAliasRepository.deleteByTenantAndKeyId(tenant, keyId);
        kmsTagRepository.deleteByTenantAndKeyId(tenant, keyId);

        // Delete the key itself
        kmsKeyRepository.delete(key);

        log.info("Key {} deleted successfully", keyId);
    }

    @Override
    public ListKeyRotationsResponseDto listKeyRotations(String tenant, Long keyId, Integer limit, String nextToken) {
        log.info("Listing key rotations for tenant: {} keyId: {} with limit: {}", tenant, keyId, limit);

        int pageSize = (limit != null && limit > 0) ? Math.min(limit, DEFAULT_PAGE_SIZE) : DEFAULT_PAGE_SIZE;
        int pageNum = (nextToken != null) ? Integer.parseInt(nextToken) : 0;

        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by("rotationDate").descending());
        Page<KmsKeyVersion> versionPage = kmsKeyVersionRepository.findByTenantAndKeyIdAndRotationDateIsNotNull(tenant, keyId, pageable);

        return ListKeyRotationsResponseDto.builder()
                .rotations(versionPage.getContent().stream()
                        .map(version -> ListKeyRotationsResponseDto.RotationDto.builder()
                                .versionId(version.getVersionId())
                                .rotationDate(version.getRotationDate())
                                .build())
                        .collect(Collectors.toList()))
                .nextToken(versionPage.hasNext() ? String.valueOf(pageNum + 1) : null)
                .build();
    }

    @Override
    public KeyUsageStatsResponseDto getKeyUsageStats(String tenant, Long keyId) {
        log.info("Getting key usage stats for tenant: {} keyId: {}", tenant, keyId);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        // Get usage statistics (these would typically come from a usage tracking service)
        long encryptCount = cryptoService.getEncryptCount(keyId);
        long decryptCount = cryptoService.getDecryptCount(keyId);
        LocalDateTime lastUsed = cryptoService.getLastUsedDate(keyId);

        return KeyUsageStatsResponseDto.builder()
                .keyId(keyId)
                .encryptCount(encryptCount)
                .decryptCount(decryptCount)
                .lastUsedDate(lastUsed)
                .build();
    }

    @Override
    public int countKeysInCustomKeyStore(String tenant, String keyStoreId) {
        log.info("Counting keys in custom key store: {} for tenant: {}", keyStoreId, tenant);

        return kmsKeyRepository.countByTenantAndKeyStoreId(tenant, keyStoreId);
    }
}

