package eu.isygoit.service.impl;

import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.enums.IEnumKeyOrigin;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.exception.CustomKeyStoreNotFoundException;
import eu.isygoit.exception.InvalidKeyStateException;
import eu.isygoit.exception.KeyNotFoundException;
import eu.isygoit.exception.KmsKeyNotFoundException;
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
    private KeyPolicyValidator policyValidator;

    @Override
    public CreateKeyResponse createKey(String tenant, CreateKeyRequest request) {
        log.info("Creating key for tenant: {} with spec: {}",
                tenant,
                request.getKeySpec());

        UUID keyId = UUID.randomUUID();

        String wrn = String.format(
                "wrn:kms:service:%s:key:%s",
                tenant,
                keyId
        );

        String versionId = "v-" + UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        try {
            // Generate key material
            byte[] keyMaterial = cryptoService.generateKeyMaterial(request.getKeySpec());

            // Create and save KMS Key
            KmsKey key = KmsKey.builder()
                    .tenant(tenant)
                    .keyId(keyId.toString())
                    .keyWrn(wrn)
                    .keySpec(request.getKeySpec())
                    .keyUsage(request.getKeyUsage())
                    .primaryKeyAlias(request.getKeyAlias())
                    .description(request.getDescription())
                    .keyStatus(IEnumKeyStatus.Types.ENABLED)
                    .origin(request.getOrigin())
                    .multiRegion(request.getMultiRegion())
                    .currentVersionId(versionId)
                    .rotationEnabled(request.getRotationEnabled())
                    .rotationPeriodInDays(request.getRotationPeriodInDays())
                    .keyMaterial(keyMaterial)
                    .build();

            if (Boolean.TRUE.equals(request.getMultiRegion())) {
                // This is a primary key
                key.setPrimaryKeyId(null);
                key.setPrimaryRegion("region:" + tenant); // e.g., from tenant or config
                key.setReplicaRegions(null); // or empty string
            }

            KmsKey savedKey = kmsKeyRepository.save(key);

            // Create initial version
            KmsKeyVersion keyVersion = KmsKeyVersion.builder()
                    .tenant(tenant)
                    .keyId(savedKey.getKeyId())
                    .versionId(versionId)
                    .keyStatus(IEnumKeyStatus.Types.ENABLED)
                    .keyMaterial(keyMaterial)
                    .build();

            kmsKeyVersionRepository.save(keyVersion);

            if (request.getPolicy() != null && !request.getPolicy().isEmpty()) {
                String policyString = KmsKeyPolicy.serializePolicy(request.getPolicy());
                policyValidator.validatePolicyLockout(policyString, request.getBypassPolicyLockoutSafetyCheck(), tenant);
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
                    wrn);

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
                            //.policy(...) // map if available
                            //.tags(savedKey.getTags())   // deserialize JSON if needed
                            .currentVersion(savedKey.getCurrentVersionId())
                            .origin(savedKey.getOrigin())
                            .keyStatus(savedKey.getKeyStatus())
                            .createdAt(savedKey.getCreateDate())
                            .updatedAt(savedKey.getUpdateDate())
                            .keyAlias(savedKey.getPrimaryKeyAlias())
                            .expirationModel(savedKey.getExpirationModel())
                            .customerMasterKeySpec(
                                    savedKey.getKeySpec() != null
                                            ? savedKey.getKeySpec().name()
                                            : null
                            )
                            //.encryptionAlgorithmSpecs(...)
                            //.signingAlgorithms(...)
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
            throw new RuntimeException(
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

        CreateKeyResponse.KeyMetadata metadata = CreateKeyResponse.KeyMetadata.builder()
                .keyId(key.getKeyId())
                .wrn(key.getKeyWrn())
                .keyStatus(key.getKeyStatus())
                .keySpec(key.getKeySpec())
                .keyUsage(key.getKeyUsage())
                .currentVersion(key.getCurrentVersionId())
                .createDate(key.getCreateDate())
                .createdAt(key.getCreateDate())
                .keyAlias(key.getPrimaryKeyAlias())
                .description(key.getDescription())
                .rotationEnabled(key.getRotationEnabled())
                .origin(key.getOrigin())
                .expirationModel(key.getExpirationModel())
                .multiRegion(key.getMultiRegion())
                .enabled(IEnumKeyStatus.Types.ENABLED.equals(key.getKeyStatus()))
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

        Page<KmsKey> keyPage = kmsKeyRepository.findByTenant(tenant, pageable);

        return ListKeysResponse.builder()
                .keys(keyPage.getContent().stream()
                        .map(key -> ListKeysResponse.KeyEntry.builder()
                                .keyId(key.getKeyId())
                                .keyWrn(key.getKeyWrn())
                                .build())
                        .toList())
                .nextToken(keyPage.hasNext()
                        ? String.valueOf(pageNum + 1)
                        : null)
                .truncated(keyPage.hasNext())
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

        key.setKeyStatus(IEnumKeyStatus.Types.DISABLED);
        kmsKeyRepository.save(key);

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
            throw new IllegalArgumentException(
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

        if (!IEnumKeyStatus.Types.ENABLED.equals(key.getKeyStatus())) {
            throw new InvalidKeyStateException(
                    "Cannot rotate key with status: " + key.getKeyStatus()
            );
        }

        String newVersionId = "v-" + UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        try {
            // Generate new key material
            byte[] newKeyMaterial =
                    cryptoService.generateKeyMaterial(key.getKeySpec());

            // Create new key version
            KmsKeyVersion newVersion = KmsKeyVersion.builder()
                    .tenant(tenant)
                    .keyId(keyId)
                    .versionId(newVersionId)
                    .keyStatus(IEnumKeyStatus.Types.ENABLED)
                    .keyMaterial(newKeyMaterial)
                    .build();

            kmsKeyVersionRepository.save(newVersion);

            // Update main key metadata
            key.setCurrentVersionId(newVersionId);
            key.setLastRotationDate(now);

            kmsKeyRepository.save(key);

            log.info("Key {} rotated successfully with new version {}",
                    keyId,
                    newVersionId);

            return RotateKeyResponse.builder()
                    .keyId(keyId)
                    .newVersionId(newVersionId)
                    .rotationDate(now)
                    .build();

        } catch (Exception e) {
            log.error("Error rotating key: {}", keyId, e);

            throw new RuntimeException(
                    "Failed to rotate key: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public UpdateKeyDescriptionResponse updateKeyDescription(
            String tenant,
            String keyId,
            UpdateKeyDescriptionRequest request) {

        log.info("Updating key description for tenant: {} keyId: {}", tenant, keyId);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));


        if (request.getDescription() != null) {
            key.setDescription(request.getDescription());
        }

        KmsKey updated = kmsKeyRepository.save(key);

        return UpdateKeyDescriptionResponse.builder().keyMetadata(CreateKeyResponse.KeyMetadata.builder()
                        .keyId(String.valueOf(updated.getKeyId()))
                        .keyAlias(updated.getPrimaryKeyAlias())
                        .description(updated.getDescription())
                        .keyStatus(updated.getKeyStatus())
                        .updatedAt(LocalDateTime.now())
                        .build())
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
            throw new InvalidKeyStateException(
                    "Key is not pending deletion: " + keyId
            );
        }

        key.setKeyStatus(IEnumKeyStatus.Types.DISABLED);
        key.setPendingDeletionWindowDays(null);
        key.setDeletionDate(null);

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

        key.setRotationEnabled(request.getEnableRotation());

        if (request.getRotationPeriodInDays() != null) {
            key.setRotationPeriodInDays(request.getRotationPeriodInDays());
        }

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
    public AliasResponseDto createAlias(String tenant, CreateAliasRequest request) {
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
                .targetKeyId(key.getKeyId())
                .build();

        KmsAlias savedAlias = kmsAliasRepository.save(alias);

        return AliasResponseDto.builder()
                .aliasName(savedAlias.getAliasName())
                .targetKeyId(savedAlias.getTargetKeyId())
                .build();
    }

    @Override
    public AliasResponseDto updateAlias(String tenant, String aliasName, UpdateAliasRequest request) {
        log.info("Updating alias: {} for tenant: {} to keyId: {}", aliasName, tenant, request.getTargetKeyId());

        KmsAlias alias = kmsAliasRepository.findByTenantAndAliasName(tenant, aliasName)
                .orElseThrow(() -> new IllegalArgumentException("Alias not found: " + aliasName));

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, request.getTargetKeyId())
                .orElseThrow(() -> new KeyNotFoundException(request.getTargetKeyId()));

        alias.setTargetKeyId(key.getKeyId());
        kmsAliasRepository.save(alias);

        return AliasResponseDto.builder()
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
                                .targetKeyId(alias.getTargetKeyId())
                                .build())
                        .collect(Collectors.toList()))
                .nextToken(aliasPage.hasNext() ? String.valueOf(pageNum + 1) : null)
                .build();
    }

    @Override
    public ListAliasesResponseDto listAliasesForKey(String tenant, String keyId, Integer limit, String nextToken) {
        log.info("Listing aliases for key: {} tenant: {}", keyId, tenant);

        Pageable pageable = RepoHelper.resolvePageable(limit, nextToken, "createDate");

        List<KmsAlias> aliases = kmsAliasRepository.findByTenantAndKeyId(tenant, keyId, pageable);

        return ListAliasesResponseDto.builder()
                .aliases(aliases.stream()
                        .map(alias -> AliasResponseDto.builder()
                                .aliasName(alias.getAliasName())
                                .targetKeyId(alias.getTargetKeyId())
                                .build())
                        .collect(Collectors.toList()))
                .nextToken(null)
                .build();
    }

    @Override
    public Object tagResource(String tenant, String keyId, TagResourceRequestDto request) {
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
    public Object untagResource(String tenant, String keyId, UntagResourceRequestDto request) {
        log.info("Untagging resource for tenant: {} keyId: {} tags: {}", tenant, keyId, request.getTagKeys());

        kmsTagRepository.deleteByTenantAndKeyIdAndTagKeyIn(tenant, keyId, request.getTagKeys());

        return new Object(); // Return empty response
    }

    @Override
    public ListTagsResponseDto listResourceTags(String tenant, String keyId) {
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
    public ImportParametersResponseDto getParametersForImport(String tenant, String keyId) {
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
    public KeyDescriptionResponseDto importKeyMaterial(String tenant, String keyId, ImportKeyMaterialRequest request) {
        log.info("Importing key material for tenant: {} keyId: {}", tenant, keyId);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        // Decrypt the imported key material
        byte[] decryptedMaterial = cryptoService.decryptKeyMaterial(
                tenant,
                request.getEncryptedKeyMaterial().getBytes(),
                request.getImportToken().getBytes()
        );

        key.setKeyMaterial(decryptedMaterial);
        key.setImportDate(LocalDateTime.now());
        key.setValidTo(request.getValidTo());
        kmsKeyRepository.save(key);

        return KeyDescriptionResponseDto.builder()
                .keyId(key.getKeyId())
                .status(key.getKeyStatus())
                .build();
    }

    @Override
    public KeyDescriptionResponseDto deleteImportedKeyMaterial(String tenant, String keyId) {
        log.info("Deleting imported key material for tenant: {} keyId: {}", tenant, keyId);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        if (!key.getImported()) {
            throw new InvalidKeyStateException("Key material was not imported: " + keyId);
        }

        key.setKeyMaterial(null);
        kmsKeyRepository.save(key);

        return KeyDescriptionResponseDto.builder()
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
                Sort.by("rotationDate").descending()
        );

        Page<KmsKeyVersion> versionPage =
                kmsKeyVersionRepository.findByTenantAndKeyIdAndCreateDateIsNotNull(
                        tenant,
                        keyId,
                        pageable
                );

        return ListKeyRotationsResponse.builder()
                .rotations(versionPage.getContent().stream()
                        .map(version -> ListKeyRotationsResponse.RotationDto.builder()
                                .versionId(version.getVersionId())
                                .rotationDate(version.getCreateDate())
                                .build())
                        .collect(Collectors.toList()))
                .nextToken(versionPage.hasNext()
                        ? String.valueOf(pageNum + 1)
                        : null)
                .build();
    }

    @Override
    public KeyUsageStatsResponseDto getKeyUsageStats(String tenant, String keyId) {
        log.info("Getting key usage stats for tenant: {} keyId: {}", tenant, keyId);

        KmsKey key = kmsKeyRepository.findByTenantAndKeyId(tenant, keyId)
                .orElseThrow(() -> new KeyNotFoundException(keyId));

        // Get usage statistics (these would typically come from a usage tracking service)
        long encryptCount = cryptoService.getEncryptCount(tenant, keyId);
        long decryptCount = cryptoService.getDecryptCount(tenant, keyId);
        LocalDateTime lastUsed = cryptoService.getLastUsedDate(tenant, keyId);

        return KeyUsageStatsResponseDto.builder()
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
        CustomKeyStore store = customKeyStoreRepository
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
        kmsKeyRepository.save(key);
        customKeyStoreRepository.save(store);

        log.info("Registered key {} in custom store {}/{}", keyId, tenant, keyStoreId);
    }

    @Override
    public void unregisterKeyFromCustomStore(String tenant, Long keyStoreId, String keyId) {
        log.debug("Unregistering key {} from custom store {}/{}", keyId, tenant, keyStoreId);

        CustomKeyStore store = customKeyStoreRepository
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

