package eu.isygoit.service.impl;

import eu.isygoit.builder.WrnBuilder;
import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.dto.data.KeyPairMaterial;
import eu.isygoit.enums.IEnumKeyOrigin;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.exception.*;
import eu.isygoit.mapper.AlgorithmMapper;
import eu.isygoit.model.*;
import eu.isygoit.repository.*;
import eu.isygoit.service.ICryptoService;
import eu.isygoit.service.IKeyManagementService;
import eu.isygoit.validator.KeyPolicyValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The type Key management service.
 * Implements WAMS KMS-compliant key management operations
 */
@Slf4j
@Service
@Transactional
public class KeyManagementService implements IKeyManagementService {

    private static final int DEFAULT_PAGE_SIZE = 100;
    private static final int MIN_DELETION_WINDOW_DAYS = 7;
    private static final int MAX_DELETION_WINDOW_DAYS = 30;

    @Autowired
    private CustomKeyStoreRepository customKeyStoreRepository;
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

    @Autowired
    private KmsKeyPolicyRepository kmsKeyPolicyRepository;

    @Autowired
    private KeyPolicyValidator keyPolicyValidator;

    @Override
    public CreateKeyResponse createKey(String tenant, CreateKeyRequest request) {
        log.info("Creating key for tenant: {} with spec: {}",
                tenant,
                request.getKeySpec());

        UUID keyId = UUID.randomUUID();

        String keyWrn = WrnBuilder.builder()
                .region(WrnBuilder.REGION_NORTH)
                .accountId(tenant)
                .resource("key", keyId.toString())
                .build();

        String versionId = "v-" + UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        try {
            // Generate key material
            KeyPairMaterial keyMaterial = cryptoService.generateKeyMaterial(request.getKeySpec());

            // Compute signing algorithm for the version (if applicable)
            String signingAlgorithm = AlgorithmMapper.getDefaultAlgorithm(request.getKeySpec(), request.getKeyUsage());

            // Calculate validTo for this version
            LocalDateTime versionValidTo = null;
            if (request.getOrigin() == IEnumKeyOrigin.Types.EXTERNAL && request.getValidTo() != null) {
                // For BYOK keys, use the explicit expiration date from the request
                versionValidTo = request.getValidTo();
            } else if (Boolean.TRUE.equals(request.getRotationEnabled()) && request.getRotationPeriodInDays() != null) {
                // For keys with rotation enabled, the version "expires" at the next rotation date
                versionValidTo = LocalDateTime.now().plusDays(request.getRotationPeriodInDays());
            } else {
                versionValidTo = null; // No expiration
            }

            // Create and save KMS Key
            KmsKey key = KmsKey.builder()
                    .tenant(tenant)
                    .keyId(keyId.toString())
                    .keyWrn(keyWrn)
                    .keySpec(request.getKeySpec())
                    .keyUsage(request.getKeyUsage())
                    .primaryKeyAlias(request.getKeyAlias())
                    .description(request.getDescription())
                    .keyStatus(request.getOrigin() == IEnumKeyOrigin.Types.EXTERNAL
                            ? IEnumKeyStatus.Types.PENDING_IMPORT
                            : IEnumKeyStatus.Types.ENABLED) // BYOK keys start as PENDING_IMPORT by default
                    .origin(request.getOrigin())
                    .multiRegion(request.getMultiRegion())
                    .currentVersionId(versionId)
                    .rotationEnabled(request.getRotationEnabled())
                    .rotationPeriodInDays(request.getRotationPeriodInDays())
                    .keyMaterial(keyMaterial.privateKey())
                    .publicKey(keyMaterial.publicKey())
                    .expirationModel(request.getExpirationModel())
                    .validTo(versionValidTo)
                    .build();

            if (Boolean.TRUE.equals(request.getMultiRegion())) {
                // This is a primary key
                key.setPrimaryKeyId(null);
                key.setPrimaryRegion("region:" + tenant); // e.g., from tenant or config
                key.setReplicaRegions(null); // or empty string
            }

            key.validateBeforeSave();
            KmsKey savedKey = kmsKeyRepository.save(key);

            // Create initial version with extended fields
            KmsKeyVersion keyVersion = KmsKeyVersion.builder()
                    .tenant(tenant)
                    .keyId(savedKey.getKeyId())
                    .versionId(versionId)
                    .keyStatus(IEnumKeyStatus.Types.ENABLED)
                    .keyMaterial(keyMaterial.privateKey())
                    .publicKey(keyMaterial.publicKey())
                    .origin(request.getOrigin())
                    .expirationModel(request.getExpirationModel())
                    .validTo(versionValidTo)          // calculated as above
                    .signingAlgorithm(signingAlgorithm)
                    .build();

            kmsKeyVersionRepository.save(keyVersion);

            if (request.getPolicy() != null && !request.getPolicy().isEmpty()) {
                String policyString = KmsKeyPolicy.serializePolicy(request.getPolicy());
                keyPolicyValidator.validatePolicyLockout(policyString, request.getBypassPolicyLockoutSafetyCheck(), tenant);
                KmsKeyPolicy keyPolicy = KmsKeyPolicy.builder()
                        .tenant(tenant)
                        .keyId(savedKey.getKeyId())
                        .policyDocument(policyString)
                        .description("Default key policy")
                        .build();
                kmsKeyPolicyRepository.save(keyPolicy);
            }

            // Create Alias
            if (StringUtils.hasText(request.getKeyAlias())) {
                KmsAlias kmsAlias = KmsAlias.builder()
                        .tenant(tenant)
                        .targetKeyId(savedKey.getKeyId())
                        .primaryKey(true)
                        .aliasName(request.getKeyAlias())
                        .build();
                kmsAliasRepository.save(kmsAlias);
            }

            // Create Tags
            if (request.getTags() != null && !request.getTags().isEmpty()) {
                List<KmsTag> tags = request.getTags().stream()
                        .map(tag -> KmsTag.builder()
                                .tenant(tenant)
                                .keyId(savedKey.getKeyId())
                                .tagKey(tag.getTagKey())
                                .tagValue(tag.getTagValue())
                                .build())
                        .collect(Collectors.toList());
                kmsTagRepository.saveAll(tags);
            }

            log.info("Key created successfully: keyId={}, wrn={}",
                    keyVersion.getKeyId(),
                    keyWrn);

            Map<String, Object> multiRegionConfig = null;
            if (Boolean.TRUE.equals(savedKey.getMultiRegion())) {
                multiRegionConfig = new HashMap<>();
                if (savedKey.getPrimaryKeyId() != null) {
                    multiRegionConfig.put("primaryKeyId", savedKey.getPrimaryKeyId());
                }
                if (savedKey.getPrimaryRegion() != null) {
                    multiRegionConfig.put("primaryRegion", savedKey.getPrimaryRegion());
                }
                if (savedKey.getReplicaRegions() != null) {
                    // Assuming replicaRegions is a comma‑separated string; split into list if desired
                    multiRegionConfig.put("replicaRegions", savedKey.getReplicaRegions());
                }
            }

            return CreateKeyResponse.builder()
                    .keyMetadata(CreateKeyResponse.KeyMetadata.builder()
                            .tenant(savedKey.getTenant())
                            .keyId(savedKey.getKeyId())
                            .wrn(savedKey.getKeyWrn())
                            .createDate(savedKey.getCreateDate())
                            .enabled(savedKey.isEnabled())
                            .description(savedKey.getDescription())
                            .rotationEnabled(savedKey.getRotationEnabled())
                            .keySpec(savedKey.getKeySpec())
                            .keyUsage(savedKey.getKeyUsage())
                            .currentVersion(savedKey.getCurrentVersionId())
                            .origin(savedKey.getOrigin())
                            .rotationEnabled(savedKey.getRotationEnabled())
                            .rotationPeriodInDays(savedKey.getRotationPeriodInDays())
                            .keyStatus(savedKey.getKeyStatus())
                            .createDate(savedKey.getCreateDate())
                            .updateDate(savedKey.getUpdateDate())
                            .keyAlias(savedKey.getPrimaryKeyAlias())
                            .expirationModel(savedKey.getExpirationModel())
                            .customerMasterKeySpec(
                                    savedKey.getKeySpec() != null
                                            ? savedKey.getKeySpec().name()
                                            : null
                            )
                            .keyManager(
                                    IEnumKeyOrigin.Types.EXTERNAL.equals(savedKey.getOrigin())
                                            ? "CUSTOMER"
                                            : "WAMS"
                            )
                            .multiRegion(savedKey.getMultiRegion())
                            .multiRegionConfiguration(multiRegionConfig)
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("Error creating key for tenant: {}", tenant, e);
            throw new CreateKeyException(
                    "Failed to create key: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public DescribeKeyResponse describeKey(String tenant, String keyId, List<String> grantTokens) {
        log.info("Getting key metadata for tenant: {} keyId: {}", tenant, keyId);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        // Fetch tags associated with this key
        List<CreateKeyRequest.Tag> tags = kmsTagRepository.findByTenantAndKeyId(tenant, key.getKeyId())
                .stream()
                .map(t -> CreateKeyRequest.Tag.builder()
                        .tagKey(t.getTagKey())
                        .tagValue(t.getTagValue())
                        .build())
                .collect(Collectors.toList());

        // Fetch key policy (if any)
        Optional<KmsKeyPolicy> policy = kmsKeyPolicyRepository.findByTenantAndKeyId(tenant, key.getKeyId());

        DescribeKeyResponse.KeyMetadata metadata = DescribeKeyResponse.KeyMetadata.builder()
                .keyId(key.getKeyId())
                .wrn(key.getKeyWrn())
                .keyStatus(key.getKeyStatus())
                .keySpec(key.getKeySpec())
                .keyUsage(key.getKeyUsage())
                .currentVersion(key.getCurrentVersionId())
                .createDate(key.getCreateDate())
                .updateDate(key.getUpdateDate())
                .keyAlias(key.getPrimaryKeyAlias())
                .description(key.getDescription())
                .rotationEnabled(key.getRotationEnabled())
                .rotationPeriodInDays(key.getRotationPeriodInDays())
                .origin(key.getOrigin())
                .expirationModel(key.getExpirationModel())
                .multiRegion(key.getMultiRegion())
                .enabled(IEnumKeyStatus.Types.ENABLED.equals(key.getKeyStatus()))
                .pendingDeletionWindowDays(key.getPendingDeletionWindowDays())
                .build();

        return DescribeKeyResponse.builder().keyMetadata(metadata).build();
    }

    @Override
    public ListKeysResponse listKeys(
            String tenant,
            Integer limit,
            String nextToken) {

        log.info("Listing keys for tenant: {} with limit: {}",
                tenant,
                limit);

        int pageSize = (limit != null && limit > 0)
                ? Math.min(limit, DEFAULT_PAGE_SIZE)
                : DEFAULT_PAGE_SIZE;

        int pageNum = (nextToken != null)
                ? Integer.parseInt(nextToken)
                : 0;

        Pageable pageable = PageRequest.of(
                pageNum,
                pageSize,
                Sort.by("createDate").descending()
        );

        Page<KmsKey> page = kmsKeyRepository.findByTenant(tenant, pageable);

        return ListKeysResponse.builder()
                .keys(page.getContent().stream()
                        .map(key -> ListKeysResponse.KeyEntry.builder()
                                .keyId(key.getKeyId())
                                .keyWrn(key.getKeyWrn())
                                .build())
                        .toList())
                .nextToken(page.hasNext() ? String.valueOf(pageable.getPageNumber() + 1) : null)
                .numberOfElements(page.getNumberOfElements())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .truncated(page.hasNext())
                .build();
    }

    @Override
    public EnableKeyResponse enableKey(String tenant, String keyId) {

        log.info("Enabling key: {} for tenant: {}", keyId, tenant);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        if (IEnumKeyStatus.Types.ENABLED.equals(key.getKeyStatus())) {
            log.warn("Key {} is already enabled", keyId);
        } else {
            key.validateBeforeSave();
            key.setKeyStatus(IEnumKeyStatus.Types.ENABLED);
            kmsKeyRepository.save(key);
        }

        return EnableKeyResponse.builder()
                .keyId(key.getKeyId())
                .keyStatus(key.getKeyStatus())
                .build();
    }

    @Override
    public DisableKeyResponse disableKey(String tenant, String keyId) {

        log.info("Disabling key: {} for tenant: {}", keyId, tenant);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        if (IEnumKeyStatus.Types.PENDING_DELETION.equals(key.getKeyStatus())) {
            throw new InvalidKeyStateException(
                    "Cannot disable key scheduled for deletion: " + keyId
            );
        }

        if (IEnumKeyStatus.Types.DISABLED.equals(key.getKeyStatus())) {
            log.warn("Key {} is already disabled", keyId);
        } else {
            key.validateBeforeSave();
            key.setKeyStatus(IEnumKeyStatus.Types.DISABLED);
            kmsKeyRepository.save(key);
        }

        return DisableKeyResponse.builder()
                .keyId(key.getKeyId())
                .status(key.getKeyStatus())
                .build();
    }

    @Override
    public ScheduleKeyDeletionResponse scheduleKeyDeletion(
            String tenant,
            String keyId,
            Integer pendingWindowInDays) {

        log.info("Scheduling deletion for key: {} for tenant: {} with window: {} days",
                keyId,
                tenant,
                pendingWindowInDays);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        int windowDays = (pendingWindowInDays != null)
                ? pendingWindowInDays
                : MIN_DELETION_WINDOW_DAYS;

        if (windowDays < MIN_DELETION_WINDOW_DAYS || windowDays > MAX_DELETION_WINDOW_DAYS) {
            throw new KeyDeletionException(
                    String.format(
                            "Pending window must be between %d and %d days",
                            MIN_DELETION_WINDOW_DAYS,
                            MAX_DELETION_WINDOW_DAYS
                    )
            );
        }

        key.setKeyStatus(IEnumKeyStatus.Types.PENDING_DELETION);
        key.setPendingDeletionWindowDays(windowDays);
        key.setDeletionDate(LocalDateTime.now().plusDays(windowDays));

        if (key.getOrigin() != IEnumKeyOrigin.Types.EXTERNAL) {
            // Non‑external keys must have material generated
            if (key.getKeyMaterial() == null) {
                throw new KeyDeletionException("Key material cannot be null for non‑external keys");
            }
        }

        key.validateBeforeSave();
        kmsKeyRepository.save(key);

        return ScheduleKeyDeletionResponse.builder()
                .keyId(key.getKeyId())
                .keyStatus(key.getKeyStatus())
                .pendingWindowInDays(windowDays)
                .deletionDate(key.getDeletionDate())
                .build();
    }

    @Override
    public RotateKeyResponse rotateKey(
            String tenant,
            String keyId) {

        log.info("Rotating key: {} for tenant: {}", keyId, tenant);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        if (IEnumKeyOrigin.Types.EXTERNAL.equals(key.getOrigin())) {
            throw new OperationNotAllowedException(
                    "Cannot rotate key with origin: " + key.getOrigin()
            );
        }

        if (!IEnumKeyStatus.Types.ENABLED.equals(key.getKeyStatus())) {
            throw new KeyRotationException(
                    "Cannot rotate key with status: " + key.getKeyStatus()
            );
        }

        String versionId = "v-" + UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        try {
            // Compute signing algorithm for the version (if applicable)
            String signingAlgorithm = AlgorithmMapper.getDefaultAlgorithm(key.getKeySpec(), key.getKeyUsage());

            // Calculate validTo for this version
            LocalDateTime versionValidTo = null;
            if (key.getOrigin() == IEnumKeyOrigin.Types.EXTERNAL && key.getValidTo() != null) {
                // For BYOK keys, use the explicit expiration date from the request
                versionValidTo = key.getValidTo();
            } else if (Boolean.TRUE.equals(key.getRotationEnabled()) && key.getRotationPeriodInDays() != null) {
                // For keys with rotation enabled, the version "expires" at the next rotation date
                versionValidTo = LocalDateTime.now().plusDays(key.getRotationPeriodInDays());
            } else {
                versionValidTo = null; // No expiration
            }

            // Generate new key material
            KeyPairMaterial keyMaterial =
                    cryptoService.generateKeyMaterial(key.getKeySpec());

            // Create new key version
            KmsKeyVersion keyVersion = KmsKeyVersion.builder()
                    .tenant(tenant)
                    .keyId(key.getKeyId())
                    .versionId(versionId)
                    .keyStatus(IEnumKeyStatus.Types.ENABLED)
                    .keyMaterial(keyMaterial.privateKey())
                    .publicKey(keyMaterial.publicKey())
                    .origin(key.getOrigin())
                    .expirationModel(key.getExpirationModel())
                    .validTo(versionValidTo)          // calculated as above
                    .signingAlgorithm(signingAlgorithm)
                    .build();

            kmsKeyVersionRepository.save(keyVersion);

            // Update main key metadata
            key.setCurrentVersionId(versionId);
            key.setLastRotationDate(now);

            key.validateBeforeSave();
            kmsKeyRepository.save(key);

            log.info("Key {} rotated successfully with new version {}",
                    keyId,
                    versionId);

            return RotateKeyResponse.builder()
                    .keyId(keyId)
                    .newVersionId(versionId)
                    .rotationDate(now)
                    .build();

        } catch (Exception e) {
            log.error("Error rotating key: {}", keyId, e);

            throw new KeyRotationException(
                    "Failed to rotate key: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    @Transactional
    public UpdateKeyDescriptionResponse updateKeyDescription(String tenant, String keyId, UpdateKeyDescriptionRequest request) {
        log.info("Updating key description, alias, and tags for tenant: {} keyId: {}", tenant, keyId);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        // 1. Update description if provided
        if (request.getDescription() != null) {
            key.setDescription(request.getDescription());
        }

        key.setRotationEnabled(request.getRotationEnabled());
        key.setRotationPeriodInDays(request.getRotationPeriodInDays());

        // 2. Update alias if provided
        String newAlias = request.getKeyAlias();
        String currentAlias = key.getPrimaryKeyAlias();
        if (newAlias != null && !newAlias.equals(currentAlias)) {

            // Check if alias already exists for another key
            Optional<KmsAlias> existingAlias = kmsAliasRepository.findByTenantAndAliasName(tenant, newAlias);
            if (existingAlias.isPresent()) {
                // Reassign existing alias to this key
                KmsAlias alias = existingAlias.get();
                alias.setTargetKeyId(key.getKeyId());
                kmsAliasRepository.save(alias);
            } else {
                // Create new alias
                KmsAlias alias = KmsAlias.builder()
                        .tenant(tenant)
                        .aliasName(newAlias)
                        .targetKeyId(key.getKeyId())
                        .primaryKey(true)  // mark as primary if this is the main alias
                        .build();
                kmsAliasRepository.save(alias);
            }
            // Update denormalized field on KmsKey
            key.setPrimaryKeyAlias(newAlias);
        }

        // 3. Update tags if provided (replace all existing tags)
        List<CreateKeyRequest.Tag> newTags = request.getTags();
        if (newTags != null) {
            // Remove all existing tags for this key
            if (kmsTagRepository.existsByTenantAndKeyId(tenant, keyId)) {
                kmsTagRepository.deleteByTenantAndKeyId(tenant, keyId);
                kmsKeyRepository.flush();
            }
            // Add new tags
            if (!newTags.isEmpty()) {
                List<KmsTag> tagsToAdd = newTags.stream()
                        .map(tag -> KmsTag.builder()
                                .tenant(tenant)
                                .keyId(key.getKeyId())
                                .tagKey(tag.getTagKey())
                                .tagValue(tag.getTagValue())
                                .build())
                        .collect(Collectors.toList());
                kmsTagRepository.saveAll(tagsToAdd);
            }
        }

        key.validateBeforeSave();
        KmsKey updated = kmsKeyRepository.save(key);

        // Build response metadata
        CreateKeyResponse.KeyMetadata keyMetadata = CreateKeyResponse.KeyMetadata.builder()
                .keyId(updated.getKeyId())
                .wrn(updated.getKeyWrn())
                .keyAlias(updated.getPrimaryKeyAlias())
                .description(updated.getDescription())
                .keyStatus(updated.getKeyStatus())
                .rotationEnabled(updated.getRotationEnabled())
                .rotationPeriodInDays(updated.getRotationPeriodInDays())
                .keySpec(updated.getKeySpec())
                .keyUsage(updated.getKeyUsage())
                .origin(updated.getOrigin())
                .createDate(updated.getCreateDate())
                .updateDate(updated.getUpdateDate())
                .currentVersion(updated.getCurrentVersionId())
                .enabled(updated.isEnabled())
                .multiRegion(updated.getMultiRegion())
                .build();

        // Optionally include tags in response (if needed)
        // keyMetadata.setTags(convertTagsToDto(newTags));

        return UpdateKeyDescriptionResponse.builder()
                .keyMetadata(keyMetadata)
                .build();
    }

    @Override
    public CancelKeyDeletionResponse cancelKeyDeletion(
            String tenant,
            String keyId) {

        log.info("Cancelling deletion for key: {} for tenant: {}",
                keyId,
                tenant);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        if (!IEnumKeyStatus.Types.PENDING_DELETION.equals(key.getKeyStatus())) {
            throw new KeyDeletionException(
                    "Key is not pending deletion: " + keyId
            );
        }

        key.setKeyStatus(IEnumKeyStatus.Types.DISABLED);
        key.setPendingDeletionWindowDays(null);
        key.setDeletionDate(null);

        key.validateBeforeSave();
        kmsKeyRepository.save(key);

        return CancelKeyDeletionResponse.builder()
                .keyId(key.getKeyId())
                .keyStatus(key.getKeyStatus())
                .build();
    }

    @Override
    public UpdateKeyRotationResponse updateKeyRotation(
            String tenant,
            String keyId,
            UpdateKeyRotationRequest request) {

        log.info("Updating key rotation for tenant: {} keyId: {} autoRotate: {}",
                tenant,
                keyId,
                request.getEnableRotation());

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        if (key.getRotationEnabled() == request.getEnableRotation()) {
            throw new KeyRotationException(
                    "Key rotation is already " + (request.getEnableRotation() ? "enabled" : "disabled") + " for key: " + keyId
            );
        }

        key.setRotationEnabled(request.getEnableRotation());

        if (request.getEnableRotation() != null && request.getEnableRotation() && request.getRotationPeriodInDays() != null) {
            key.setRotationPeriodInDays(request.getRotationPeriodInDays());
        } else if (request.getEnableRotation() != null && !request.getEnableRotation() && key.getRotationPeriodInDays() == null) {
            throw new KeyRotationException(
                    "Key rotation period is required for key: " + keyId
            );
        }

        key.validateBeforeSave();
        kmsKeyRepository.save(key);

        if (request.getEnableRotation() != null && request.getEnableRotation()) {
            this.rotateKey(tenant, keyId);
        }

        return UpdateKeyRotationResponse.builder()
                .keyId(key.getKeyId())
                .rotationEnabled(key.getRotationEnabled())
                .rotationPeriodInDays(key.getRotationPeriodInDays())
                .lastRotationDate(key.getLastRotationDate())
                .build();
    }

    @Override
    public UpdateKeyRotationResponse enableKeyRotation(String tenant, String keyId) {
        log.info("Enable key rotation for tenant: {} keyId: {}",
                tenant,
                keyId);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        key.setRotationEnabled(true);

        key.validateBeforeSave();
        kmsKeyRepository.save(key);

        return UpdateKeyRotationResponse.builder()
                .keyId(key.getKeyId())
                .rotationEnabled(key.getRotationEnabled())
                .rotationPeriodInDays(key.getRotationPeriodInDays())
                .lastRotationDate(key.getLastRotationDate())
                .build();
    }

    @Override
    public UpdateKeyRotationResponse disableKeyRotation(String tenant, String keyId) {
        log.info("Disable key rotation for tenant: {} keyId: {}",
                tenant,
                keyId);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        key.setRotationEnabled(false);

        key.validateBeforeSave();
        kmsKeyRepository.save(key);

        return UpdateKeyRotationResponse.builder()
                .keyId(key.getKeyId())
                .rotationEnabled(key.getRotationEnabled())
                .rotationPeriodInDays(key.getRotationPeriodInDays())
                .lastRotationDate(key.getLastRotationDate())
                .build();
    }

    @Override
    public GetKeyRotationStatusResponse getKeyRotationStatus(
            String tenant,
            String keyId) {

        log.info("Getting key rotation status for tenant: {} keyId: {}",
                tenant,
                keyId);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        return GetKeyRotationStatusResponse.builder()
                .keyId(key.getKeyId())
                .rotationEnabled(key.getRotationEnabled())
                .rotationPeriodInDays(key.getRotationPeriodInDays())
                .lastRotationDate(key.getLastRotationDate())
                .build();
    }

    @Override
    public GetPublicKeyResponse getPublicKey(String tenant, String keyId) {
        log.info("Getting public key for tenant: {} keyId: {}", tenant, keyId);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        // Extract public key from key material (implementation depends on key spec)
        byte[] publicKey = cryptoService.extractPublicKey(key.getKeyMaterial(), key.getKeySpec());

        return GetPublicKeyResponse.builder()
                .keyId(keyId)
                .publicKey(Arrays.toString(publicKey))
                .customerMasterKeySpec(key.getKeySpec())
                .build();
    }

    @Override
    public AliasResponse createAlias(String tenant, CreateAliasRequest request) {
        log.info("Creating alias: {} for tenant: {} keyId: {}",
                request.getAliasName(), tenant, request.getTargetKeyId());

        // Check if alias already exists
        if (kmsAliasRepository.findByTenantAndAliasName(tenant, request.getAliasName()).isPresent()) {
            throw new KeyAliasNotFoundException("Alias already exists: " + request.getAliasName());
        }

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, request.getTargetKeyId())
                .orElseThrow(() -> new KeyNotFoundException(request.getTargetKeyId()));

        KmsAlias alias = KmsAlias.builder()
                .tenant(tenant)
                .aliasName(request.getAliasName())
                .targetKeyId(key.getKeyId())
                .primaryKey(request.getPrimary() != null ? request.getPrimary() : false)
                .build();

        // If primary alias, mark existing primary alias as non-primary
        if (request.getPrimary() != null && request.getPrimary()) {
            kmsAliasRepository.findByTenantAndAliasName(key.getTenant(), key.getPrimaryKeyAlias()).ifPresent(existingAlias -> {
                existingAlias.setPrimaryKey(false);
                kmsAliasRepository.save(existingAlias);
            });
            key.setPrimaryKeyAlias(request.getAliasName());
            kmsKeyRepository.save(key);
        }

        KmsAlias savedAlias = kmsAliasRepository.save(alias);

        return AliasResponse.builder()
                .aliasName(savedAlias.getAliasName())
                .targetKeyId(savedAlias.getTargetKeyId())
                .build();
    }

    @Override
    public AliasResponse updateAlias(String tenant, String aliasName, UpdateAliasRequest request) {
        log.info("Updating alias: {} for tenant: {} to keyId: {}", aliasName, tenant, request.getTargetKeyId());

        KmsAlias alias = kmsAliasRepository.findByTenantAndAliasName(tenant, aliasName)
                .orElseThrow(() -> new IllegalArgumentException("Alias not found: " + aliasName));

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, request.getTargetKeyId())
                .orElseThrow(() -> new KeyNotFoundException(request.getTargetKeyId()));

        alias.setTargetKeyId(key.getKeyId());
        kmsAliasRepository.save(alias);

        return AliasResponse.builder()
                .aliasName(alias.getAliasName())
                .targetKeyId(alias.getTargetKeyId())
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
    public ListAliasesResponse listAliases(String tenant, Integer limit, String nextToken) {
        log.info("Listing aliases for tenant: {} with limit: {}", tenant, limit);

        int pageSize = (limit != null && limit > 0) ? Math.min(limit, DEFAULT_PAGE_SIZE) : DEFAULT_PAGE_SIZE;
        int pageNum = (nextToken != null) ? Integer.parseInt(nextToken) : 0;

        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by("createDate"));
        Page<KmsAlias> page = kmsAliasRepository.findByTenant(tenant, pageable);

        return ListAliasesResponse.builder()
                .aliases(page.getContent().stream()
                        .map(alias -> ListAliasesResponse.AliasEntry.builder()
                                .aliasName(alias.getAliasName())
                                .primaryKey(alias.getPrimaryKey())
                                .targetKeyId(alias.getTargetKeyId())
                                .build())
                        .collect(Collectors.toList()))
                .nextToken(page.hasNext() ? String.valueOf(pageable.getPageNumber() + 1) : null)
                .numberOfElements(page.getNumberOfElements())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .truncated(page.hasNext())
                .build();
    }

    @Override
    public ListAliasesResponse listAliasesForKey(String tenant, String keyId, Integer limit, String nextToken) {
        log.info("Listing aliases for key: {} tenant: {}", keyId, tenant);

        Pageable pageable = RepoHelper.resolvePageable(limit, nextToken, "createDate");

        Page<KmsAlias> page = kmsAliasRepository.findByTenantAndKeyId(tenant, keyId, pageable);

        return ListAliasesResponse.builder()
                .aliases(page.stream()
                        .map(alias -> ListAliasesResponse.AliasEntry.builder()
                                .aliasName(alias.getAliasName())
                                .targetKeyId(alias.getTargetKeyId())
                                .primaryKey(alias.getPrimaryKey())
                                .build())
                        .collect(Collectors.toList()))
                .nextToken(page.hasNext() ? String.valueOf(pageable.getPageNumber() + 1) : null)
                .numberOfElements(page.getNumberOfElements())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .truncated(page.hasNext())
                .build();
    }

    @Override
    public Object tagResource(String tenant, String keyId, TagResourceRequest request) {
        log.info("Tagging resource for tenant: {} keyId: {}", tenant, keyId);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        for (ListResourceTagsResponse.Tag tag : request.getTags()) {
            KmsTag kmsTag = KmsTag.builder()
                    .tenant(tenant)
                    .keyId(keyId)
                    .tagKey(tag.getTagKey())
                    .tagValue(tag.getTagValue())
                    .build();
            kmsTagRepository.save(kmsTag);
        }

        return new Object(); // Return empty response
    }

    @Override
    public Object untagResource(String tenant, String keyId, UntagResourceRequest request) {
        log.info("Untagging resource for tenant: {} keyId: {} tags: {}", tenant, keyId, request.getTagKeys());

        kmsTagRepository.deleteByTenantAndKeyIdAndTagKeyIn(tenant, keyId, request.getTagKeys());

        return new Object(); // Return empty response
    }

    @Override
    public ListResourceTagsResponse listResourceTags(String tenant,
                                                     String keyId,
                                                     Integer limit,
                                                     String nextToken) {
        log.info("Listing resource tags for tenant: {} keyId: {}", tenant, keyId);

        int pageSize = (limit != null && limit > 0)
                ? Math.min(limit, 1000)
                : 100;

        int pageNum = (nextToken != null)
                ? Integer.parseInt(nextToken)
                : 0;

        Pageable pageable = PageRequest.of(
                pageNum,
                pageSize,
                Sort.by("createDate").descending()
        );

        Page<KmsTag> page = kmsTagRepository.findByTenantAndKeyId(tenant, keyId, pageable);

        return ListResourceTagsResponse.builder()
                .tags(page.stream()
                        .map(tag -> ListResourceTagsResponse.Tag.builder()
                                .tagKey(tag.getTagKey())
                                .tagValue(tag.getTagValue())
                                .build())
                        .collect(Collectors.toList()))
                .nextToken(page.hasNext() ? String.valueOf(page.getNumber() + 1) : null)
                .numberOfElements(page.getNumberOfElements())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .truncated(page.hasNext())
                .build();
    }

    @Override
    public ImportParametersResponse getParametersForImport(String tenant, String keyId) {
        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        // 1. Verify key is suitable for BYOK
        if (key.getOrigin() != IEnumKeyOrigin.Types.EXTERNAL) {
            throw new IllegalKeyOriginException("Key origin must be EXTERNAL for BYOK");
        }
        if (key.getKeyStatus() == IEnumKeyStatus.Types.PENDING_DELETION) {
            throw new DisabledKeyException("Key is pending deletion");
        }
        if (key.hasImportedMaterial()) {
            throw new ImportedMaterialExistsException("Key already has imported material. Delete it first.");
        }

        // 2. Generate parameters
        KeyPairMaterial wrappingKey = cryptoService.generateWrappingKey(); // RSA 2048 key pair
        byte[] importToken = cryptoService.generateImportToken();         // 32 random bytes
        LocalDateTime validTo = LocalDateTime.now().plusHours(24);

        // 3. Store token, its expiration, and the private wrapping key
        key.setImportToken(importToken);
        key.setImportTokenValidTo(validTo);
        key.setPrivateWrappingKey(wrappingKey.privateKey());   // store the private part

        key.validateBeforeSave();
        kmsKeyRepository.save(key);

        // 4. Return the public wrapping key and token (private key stays in DB)
        return ImportParametersResponse.builder()
                .keyId(keyId)
                .wrappingKey(wrappingKey)   // includes public key only
                .importToken(importToken)
                .validTo(validTo)
                .build();
    }

    @Override
    public KeyDescriptionResponse importKeyMaterial(String tenant, String keyId, ImportKeyMaterialRequest request) {
        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        // 1. Validate key state
        if (key.getOrigin() != IEnumKeyOrigin.Types.EXTERNAL) {
            throw new IllegalKeyOriginException("Key origin must be EXTERNAL for BYOK");
        }
        if (key.getKeyStatus() == IEnumKeyStatus.Types.PENDING_DELETION) {
            throw new DisabledKeyException("Key is pending deletion");
        }
        if (key.hasImportedMaterial()) {
            throw new ImportedMaterialExistsException("Key already has imported material. Delete it first.");
        }

        // 2. Validate import token
        byte[] requestToken = Base64.getDecoder().decode(request.getImportToken());
        if (key.getImportToken() == null || !Arrays.equals(key.getImportToken(), requestToken)) {
            throw new InvalidImportTokenException("Invalid or missing import token");
        }
        if (key.getImportTokenValidTo().isBefore(LocalDateTime.now())) {
            throw new ExpiredImportTokenException("Import token has expired. Generate new parameters.");
        }

        // 3. Decrypt the key material using the stored private wrapping key
        byte[] encryptedMaterial = Base64.getDecoder().decode(request.getEncryptedKeyMaterial());
        byte[] privateWrappingKey = key.getPrivateWrappingKey();
        if (privateWrappingKey == null) {
            throw new InvalidImportTokenException("No private wrapping key found for this key.");
        }
        byte[] decryptedMaterial;
        try {
            decryptedMaterial = cryptoService.decryptWithPrivateKey(privateWrappingKey, encryptedMaterial);
        } catch (Exception e) {
            log.error("Decryption of key material failed", e);
            throw new ImportKeyMaterialException("Failed to decrypt key material: " + e.getMessage(), e);
        }

        // 4. Set the key material and clear import parameters
        key.setKeyMaterial(decryptedMaterial);
        key.setImportDate(LocalDateTime.now());
        key.setValidTo(request.getValidTo());
        key.setKeyStatus(IEnumKeyStatus.Types.ENABLED);
        // Invalidate the import token and private wrapping key
        key.setImportToken(null);
        key.setImportTokenValidTo(null);
        key.setPrivateWrappingKey(null);

        key.validateBeforeSave();
        kmsKeyRepository.save(key);

        return KeyDescriptionResponse.builder()
                .keyId(key.getKeyId())
                .status(key.getKeyStatus())
                .build();
    }

    @Override
    public KeyDescriptionResponse deleteImportedKeyMaterial(String tenant, String keyId) {
        log.info("Deleting imported key material for tenant: {} keyId: {}", tenant, keyId);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        if (!key.getImported()) {
            throw new InvalidKeyStateException("Key material was not imported: " + keyId);
        }

        // Remove key material and reset to PENDING_IMPORT state
        key.setKeyMaterial(null);
        key.setValidTo(null);
        key.setExpirationModel(null);
        key.setImportDate(null);
        key.setKeyStatus(IEnumKeyStatus.Types.PENDING_IMPORT);
        // Clean up any leftover BYOK temporary fields
        key.setImportToken(null);
        key.setImportTokenValidTo(null);
        key.setPrivateWrappingKey(null);

        key.validateBeforeSave();
        kmsKeyRepository.save(key);

        return KeyDescriptionResponse.builder()
                .keyId(key.getKeyId())
                .status(key.getKeyStatus())
                .build();
    }

    @Override
    public boolean isValidKey(String tenant, String keyId) {
        log.info("Validating key for tenant: {} keyId: {}", tenant, keyId);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        if (IEnumKeyStatus.Types.PENDING_DELETION.equals(key.getKeyStatus())) {
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

        return true;
    }

    @Override
    public void deleteKey(String tenant, String keyId) {

        log.info("Deleting key for tenant: {} keyId: {}", tenant, keyId);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        // Optional safety guard (recommended in KMS-like systems)
        if (!IEnumKeyStatus.Types.PENDING_DELETION.equals(key.getKeyStatus())) {
            throw new InvalidKeyStateException(
                    "Key must be in PENDING_DELETION state before permanent deletion: " + keyId
            );
        }

        // Delete associated resources
        kmsKeyVersionRepository.deleteByTenantAndKeyId(tenant, keyId);
        kmsAliasRepository.deleteByTenantAndTargetKeyId(tenant, keyId);
        kmsTagRepository.deleteByTenantAndKeyId(tenant, keyId);

        // Delete key
        kmsKeyRepository.delete(key);

        log.info("Key {} deleted successfully", keyId);
    }

    @Override
    public ListKeyRotationsResponse listKeyRotations(
            String tenant,
            String keyId,
            Integer limit,
            String nextToken) {

        log.info("Listing key rotations for tenant: {} keyId: {} with limit: {}",
                tenant,
                keyId,
                limit);

        int pageSize = (limit != null && limit > 0)
                ? Math.min(limit, DEFAULT_PAGE_SIZE)
                : DEFAULT_PAGE_SIZE;

        int pageNum = (nextToken != null)
                ? Integer.parseInt(nextToken)
                : 0;

        Pageable pageable = PageRequest.of(
                pageNum,
                pageSize,
                Sort.by("createDate")
        );

        Page<KmsKeyVersion> page =
                kmsKeyVersionRepository.findByTenantAndKeyIdAndCreateDateIsNotNull(
                        tenant,
                        keyId,
                        pageable
                );

        return ListKeyRotationsResponse.builder()
                .rotations(page.getContent().stream()
                        .map(version -> ListKeyRotationsResponse.Rotation.builder()
                                .versionId(version.getVersionId())
                                .rotationDate(version.getCreateDate())
                                .build())
                        .collect(Collectors.toList()))
                .nextToken(page.hasNext() ? String.valueOf(pageable.getPageNumber() + 1) : null)
                .numberOfElements(page.getNumberOfElements())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .truncated(page.hasNext())
                .build();
    }

    @Override
    public KeyUsageStatsResponse getKeyUsageStats(String tenant, String keyId) {
        log.info("Getting key usage stats for tenant: {} keyId: {}", tenant, keyId);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        // Get usage statistics (these would typically come from a usage tracking service)
        long encryptCount = cryptoService.getEncryptCount(tenant, keyId);
        long decryptCount = cryptoService.getDecryptCount(tenant, keyId);
        LocalDateTime lastUsed = cryptoService.getLastUsedDate(tenant, keyId);

        return KeyUsageStatsResponse.builder()
                .keyId(keyId)
                .encryptCount(encryptCount)
                .decryptCount(decryptCount)
                .lastUsedDate(lastUsed)
                .build();
    }


    @Override
    public void registerKeyInCustomStore(String tenant, Long keyStoreId, String keyId) {
        log.debug("Registering key {} in custom store {}/{}", keyId, tenant, keyStoreId);

        // Fetch the CustomKeyStore (ensure tenant isolation)
        KmsCustomKeyStore store = customKeyStoreRepository
                .findByTenantAndId(tenant, keyStoreId)
                .orElseThrow(() -> new CustomKeyStoreNotFoundException(
                        "Custom key store not found: " + keyStoreId));

        // Fetch the KmsKey (ensure tenant isolation)
        KmsKey key = kmsKeyRepository
                .findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KmsKeyNotFoundException(
                        "KMS key not found: " + keyId));

        // Set the bidirectional relationship
        store.addKey(key);  // updates keyCount and the in‑memory list

        // Persist both sides (cascade is not configured, so save both)
        key.validateBeforeSave();
        kmsKeyRepository.save(key);

        customKeyStoreRepository.save(store);

        log.info("Registered key {} in custom store {}/{}", keyId, tenant, keyStoreId);
    }

    @Override
    public void unregisterKeyFromCustomStore(String tenant, Long keyStoreId, String keyId) {
        log.debug("Unregistering key {} from custom store {}/{}", keyId, tenant, keyStoreId);

        KmsCustomKeyStore store = customKeyStoreRepository
                .findByTenantAndId(tenant, keyStoreId)
                .orElse(null);
        if (store == null) {
            log.warn("Custom store {} not found – cannot unregister key {}", keyStoreId, keyId);
            return;
        }

        KmsKey key = kmsKeyRepository
                .findByTenantAndKeyId(tenant, keyId)
                .orElse(null);
        if (key == null) {
            log.warn("KMS key {} not found – cannot unregister from store {}", keyId, keyStoreId);
            return;
        }

        store.removeKey(key);

        // Save changes
        key.validateBeforeSave();
        kmsKeyRepository.save(key);

        customKeyStoreRepository.save(store);

        log.info("Unregistered key {} from custom store {}/{}", keyId, tenant, keyStoreId);
    }

    @Override
    public int countKeysInCustomKeyStore(String tenant, Long keyStoreId) {
        return customKeyStoreRepository
                .findByTenantAndId(tenant, keyStoreId)
                .map(store -> store.getKeys().size())
                .orElse(0);
    }
}

