package eu.isygoit.service.impl;

import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.enums.IEnumCustomKeyStoreStatus;
import eu.isygoit.enums.IEnumCustomKeyStoreType;
import eu.isygoit.exception.*;
import eu.isygoit.model.CustomKeyStore;
import eu.isygoit.repository.CustomKeyStoreRepository;
import eu.isygoit.repository.RepoHelper;
import eu.isygoit.service.ICustomKeyStoreService;
import eu.isygoit.service.IKeyManagementService;
import eu.isygoit.simulation.CustomKeyStoreConnection;
import eu.isygoit.simulation.ExternalKeyProxyInstance;
import eu.isygoit.simulation.SoftwareHsmInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Complete internal Custom Key Store Service Implementation.
 * Supports CloudHSM (software simulation) and External Key Store (XKS proxy simulation).
 * Implements full key management, cryptographic operations, rotation, health checks, and audit.
 */
@Slf4j
@Service
@Transactional
public class CustomKeyStoreService implements ICustomKeyStoreService {

    // In-memory connection & store simulations
    private final ConcurrentHashMap<Long, CustomKeyStoreConnection> activeConnections = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, SoftwareHsmInstance> hsmInstances = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ExternalKeyProxyInstance> externalProxies = new ConcurrentHashMap<>();

    @Autowired
    private CustomKeyStoreRepository customKeyStoreRepository;

    @Autowired
    private IKeyManagementService keyManagementService;   // main KMS service that uses custom stores

    @Value("${kms.custom-key-store.max-stores-per-tenant:10}")
    private int maxStoresPerTenant;

    @Value("${kms.custom-key-store.connection-heartbeat-seconds:60}")
    private int connectionHeartbeatSeconds;

    @Value("${kms.custom-key-store.health-check-interval-seconds:300}")
    private int healthCheckIntervalSeconds;

    @Value("${kms.custom-key-store.max-keys-default:1000}")
    private int defaultMaxKeys;

    // =========================================================================
    // Custom Key Store Lifecycle Management (CRUD + Connect/Disconnect)
    // =========================================================================

    @Override
    public CustomKeyStoreResponseDto createCustomKeyStore(String tenant, CreateCustomKeyStoreRequestDto request) {
        log.info("Creating custom key store for tenant: {}, name: {}, type: {}",
                tenant, request.getKeyStoreName(), request.getType());

        validateTenantLimit(tenant);
        if (customKeyStoreRepository.existsByTenantAndName(tenant, request.getKeyStoreName())) {
            throw new DuplicateCustomKeyStoreNameException(
                    "Custom key store with name '" + request.getKeyStoreName() + "' already exists for tenant");
        }

        CustomKeyStore store = new CustomKeyStore();
        store.setTenant(tenant);
        store.setName(request.getKeyStoreName());
        store.setType(request.getType());
        store.setStatus(IEnumCustomKeyStoreStatus.Types.DISCONNECTED);
        store.setCreatedAt(LocalDateTime.now());
        store.setUpdatedAt(LocalDateTime.now());
        store.setMaxKeys(request.getMaxKeys() != null ? request.getMaxKeys() : defaultMaxKeys);
        store.setHealthStatus("UNKNOWN");
        store.setMetadata(request.getMetadata() != null ? convertMapToJson(request.getMetadata()) : null);
        store.setTags(request.getTags() != null ? convertMapToJson(request.getTags()) : null);

        // Type-specific configuration
        if (store.getType() == IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM) {
            validateCloudHsmRequest(request);
            configureCloudHsmStore(store, request);
        } else if (store.getType() == IEnumCustomKeyStoreType.Types.EXTERNAL_KEY_STORE) {
            validateExternalKeyStoreRequest(request);
            configureExternalKeyStore(store, request);
        } else {
            throw new UnsupportedCustomKeyStoreTypeException("Unsupported type: " + request.getType());
        }

        // Initialize underlying simulation
        CustomKeyStore saved = customKeyStoreRepository.save(store);
        log.info("Custom key store created with id: {}", saved.getId());

        initializeStoreSimulation(saved);
        log.info("Custom key store simulation initialized: {}", saved.getId());

        return convertToResponseDto(saved);
    }

    @Override
    public CustomKeyStoreResponseDto describeCustomKeyStore(String tenant, Long keyStoreId) {
        CustomKeyStore store = findCustomKeyStore(tenant, keyStoreId);
        updateConnectionStatus(store);   // refresh based on heartbeat
        return convertToResponseDto(store);
    }

    @Override
    public CustomKeyStoreResponseDto updateCustomKeyStore(String tenant, Long keyStoreId,
                                                          UpdateCustomKeyStoreRequestDto request) {
        CustomKeyStore store = findCustomKeyStore(tenant, keyStoreId);
        if (store.getStatus() == IEnumCustomKeyStoreStatus.Types.CONNECTED) {
            throw new CustomKeyStoreConnectedException("Cannot update while connected. Disconnect first.");
        }

        if (request.getNewCustomKeyStoreName() != null && !request.getNewCustomKeyStoreName().isEmpty()) {
            if (!store.getName().equals(request.getNewCustomKeyStoreName()) &&
                    customKeyStoreRepository.existsByTenantAndName(tenant, request.getNewCustomKeyStoreName())) {
                throw new DuplicateCustomKeyStoreNameException("Name already exists: " + request.getNewCustomKeyStoreName());
            }
            store.setName(request.getNewCustomKeyStoreName());
        }

        if (request.getMaxKeys() != null) {
            store.setMaxKeys(request.getMaxKeys());
        }
        if (request.getMetadata() != null) {
            store.setMetadata(convertMapToJson(request.getMetadata()));
        }
        if (request.getTags() != null) {
            store.setTags(convertMapToJson(request.getTags()));
        }

        // Update type-specific credentials (always hashed)
        if (store.getType() == IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM) {
            updateCloudHsmStore(store, request);
        } else if (store.getType() == IEnumCustomKeyStoreType.Types.EXTERNAL_KEY_STORE) {
            updateExternalKeyStore(store, request);
        }

        store.setUpdatedAt(LocalDateTime.now());
        CustomKeyStore updated = customKeyStoreRepository.save(store);
        log.info("Updated custom key store: {}", keyStoreId);
        return convertToResponseDto(updated);
    }

    @Override
    public void deleteCustomKeyStore(String tenant, Long keyStoreId) {
        CustomKeyStore store = findCustomKeyStore(tenant, keyStoreId);
        int keyCount = keyManagementService.countKeysInCustomKeyStore(tenant, keyStoreId);
        if (keyCount > 0) {
            throw new CustomKeyStoreHasKeysException("Cannot delete store containing " + keyCount + " keys.");
        }
        if (store.getStatus() != IEnumCustomKeyStoreStatus.Types.DISCONNECTED) {
            throw new CustomKeyStoreConnectedException("Store must be disconnected before deletion.");
        }

        cleanupStoreSimulation(store);
        customKeyStoreRepository.delete(store);
        log.info("Deleted custom key store: {}", keyStoreId);
    }

    @Override
    public ListCustomKeyStoresResponseDto listCustomKeyStores(String tenant, Integer limit, String nextToken) {

        Pageable pageable = RepoHelper.resolvePageable(limit, nextToken, "creationDate");

        List<CustomKeyStore> stores;
        String newNextToken = null;

        if (nextToken != null && !nextToken.isEmpty()) {
            Long lastId = decodeNextToken(nextToken);
            stores = customKeyStoreRepository.findByTenantAndIdGreaterThanOrderByIdAsc(tenant, lastId, pageable);
        } else {
            stores = customKeyStoreRepository.findByTenantOrderByIdAsc(tenant, pageable);
        }

        if (stores.size() == pageable.getPageSize()) {
            newNextToken = encodeNextToken(stores.get(stores.size() - 1).getId());
        }

        List<CustomKeyStoreResponseDto> dtos = stores.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
        return ListCustomKeyStoresResponseDto.builder()
                .customKeyStores(dtos)
                .nextToken(newNextToken)
                .truncated(newNextToken != null)
                .build();
    }

    @Override
    public void connectCustomKeyStore(String tenant, Long keyStoreId) {
        CustomKeyStore store = findCustomKeyStore(tenant, keyStoreId);
        if (store.getStatus() == IEnumCustomKeyStoreStatus.Types.CONNECTED) {
            throw new CustomKeyStoreAlreadyConnectedException("Already connected.");
        }
        if (store.getStatus() == IEnumCustomKeyStoreStatus.Types.CONNECTING) {
            throw new CustomKeyStoreConnectingException("Connection already in progress.");
        }

        store.setStatus(IEnumCustomKeyStoreStatus.Types.CONNECTING);
        store.setLastConnectionAttempt(LocalDateTime.now());
        customKeyStoreRepository.save(store);

        try {
            boolean success = establishInternalConnection(store);
            if (success) {
                store.setStatus(IEnumCustomKeyStoreStatus.Types.CONNECTED);
                store.setLastSuccessfulConnection(LocalDateTime.now());
                store.setConnectionError(null);
                activeConnections.put(store.getId(),
                        new CustomKeyStoreConnection(store.getId(), LocalDateTime.now(), connectionHeartbeatSeconds));
                log.info("Connected custom key store: {}", keyStoreId);
            } else {
                store.setStatus(IEnumCustomKeyStoreStatus.Types.FAILED);
                store.setConnectionError("Connection failed – invalid credentials or unreachable.");
                throw new CustomKeyStoreConnectionException("Connection failed.");
            }
        } catch (Exception e) {
            store.setStatus(IEnumCustomKeyStoreStatus.Types.FAILED);
            store.setConnectionError(e.getMessage());
            throw new CustomKeyStoreConnectionException("Connection error: " + e.getMessage());
        } finally {
            store.updateHealthStatus();
            store.setUpdatedAt(LocalDateTime.now());
            customKeyStoreRepository.save(store);
        }
    }

    @Override
    public void disconnectCustomKeyStore(String tenant, Long keyStoreId) {
        CustomKeyStore store = findCustomKeyStore(tenant, keyStoreId);
        if (store.getStatus() != IEnumCustomKeyStoreStatus.Types.CONNECTED) {
            throw new CustomKeyStoreNotConnectedException("Store is not connected.");
        }

        closeInternalConnection(store);
        activeConnections.remove(store.getId());
        store.setStatus(IEnumCustomKeyStoreStatus.Types.DISCONNECTED);
        store.updateHealthStatus();
        store.setUpdatedAt(LocalDateTime.now());
        customKeyStoreRepository.save(store);
        log.info("Disconnected custom key store: {}", keyStoreId);
    }

    // =========================================================================
    // Cryptographic Operations Delegated to the Underlying Store
    // =========================================================================

    @Override
    public byte[] encrypt(String tenant, Long keyStoreId, String keyId, byte[] plaintext,
                          Map<String, String> encryptionContext) throws Exception {
        CustomKeyStore store = findCustomKeyStore(tenant, keyStoreId);
        ensureConnected(store);
        if (store.getType() == IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM) {
            SoftwareHsmInstance hsm = hsmInstances.get(store.getId());
            return hsm.encrypt(keyId, plaintext, encryptionContext);
        } else {
            ExternalKeyProxyInstance proxy = externalProxies.get(store.getId());
            return proxy.encrypt(keyId, plaintext, encryptionContext);
        }
    }

    @Override
    public byte[] decrypt(String tenant, Long keyStoreId, String keyId, byte[] ciphertext,
                          Map<String, String> encryptionContext) throws Exception {
        CustomKeyStore store = findCustomKeyStore(tenant, keyStoreId);
        ensureConnected(store);
        if (store.getType() == IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM) {
            SoftwareHsmInstance hsm = hsmInstances.get(store.getId());
            return hsm.decrypt(keyId, ciphertext, encryptionContext);
        } else {
            ExternalKeyProxyInstance proxy = externalProxies.get(store.getId());
            return proxy.decrypt(keyId, ciphertext, encryptionContext);
        }
    }

    @Override
    public byte[] sign(String tenant, Long keyStoreId, String keyId, byte[] message, String algorithm) throws Exception {
        CustomKeyStore store = findCustomKeyStore(tenant, keyStoreId);
        ensureConnected(store);
        if (store.getType() == IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM) {
            return hsmInstances.get(store.getId()).sign(keyId, message, algorithm);
        } else {
            return externalProxies.get(store.getId()).sign(keyId, message, algorithm);
        }
    }

    @Override
    public boolean verify(String tenant, Long keyStoreId, String keyId, byte[] message, byte[] signature, String algorithm)
            throws Exception {
        CustomKeyStore store = findCustomKeyStore(tenant, keyStoreId);
        ensureConnected(store);
        if (store.getType() == IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM) {
            return hsmInstances.get(store.getId()).verify(keyId, message, signature, algorithm);
        } else {
            return externalProxies.get(store.getId()).verify(keyId, message, signature, algorithm);
        }
    }

    // =========================================================================
    // Background Health Check & Connection Heartbeat
    // =========================================================================

    @Scheduled(fixedDelayString = "${kms.custom-key-store.health-check-interval-ms:300000}")
    public void healthCheckAllStores() {
        List<CustomKeyStore> stores = customKeyStoreRepository.findAll();
        for (CustomKeyStore store : stores) {
            if (store.getStatus() == IEnumCustomKeyStoreStatus.Types.CONNECTED) {
                try {
                    boolean healthy = performHealthCheck(store);
                    if (!healthy) {
                        log.warn("Health check failed for store {}, marking as FAILED", store.getId());
                        store.setStatus(IEnumCustomKeyStoreStatus.Types.FAILED);
                        store.setConnectionError("Health check failed");
                        store.updateHealthStatus();
                        activeConnections.remove(store.getId());
                        customKeyStoreRepository.save(store);
                    } else {
                        // Refresh heartbeat
                        CustomKeyStoreConnection conn = activeConnections.get(store.getId());
                        if (conn != null) conn.refresh();
                        store.setLastHealthCheck(LocalDateTime.now());
                        customKeyStoreRepository.save(store);
                    }
                } catch (Exception e) {
                    log.error("Health check error for store {}: {}", store.getId(), e.getMessage());
                }
            }
        }
    }

    // Modified performHealthCheck:
    private boolean performHealthCheck(CustomKeyStore store) {
        if (store.getType() == IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM) {
            SoftwareHsmInstance hsm = hsmInstances.get(store.getId());
            return hsm != null && hsm.isConnected();
        } else {
            ExternalKeyProxyInstance proxy = externalProxies.get(store.getId());
            return proxy != null && proxy.isConnected();
        }
    }

    // =========================================================================
    // Private Helpers: Validation, Configuration, Simulation Management
    // =========================================================================

    private void validateTenantLimit(String tenant) {
        long count = customKeyStoreRepository.countByTenant(tenant);
        if (count >= maxStoresPerTenant) {
            throw new CustomKeyStoreLimitExceededException(
                    "Max stores per tenant reached: " + maxStoresPerTenant);
        }
    }

    private void validateCloudHsmRequest(CreateCustomKeyStoreRequestDto request) {
        if (request.getCloudHsmClusterId() == null || request.getCloudHsmClusterId().isEmpty())
            throw new MissingCloudHsmClusterIdException("cloudHsmClusterId required");
        if (request.getKeyStorePassword() == null || request.getKeyStorePassword().isEmpty())
            throw new MissingKeyStorePasswordException("keyStorePassword required");
        if (request.getTrustAnchorCertificate() == null || request.getTrustAnchorCertificate().isEmpty())
            throw new MissingTrustAnchorCertificateException("trustAnchorCertificate required");
    }

    private void validateExternalKeyStoreRequest(CreateCustomKeyStoreRequestDto request) {
        if (request.getXksProxyUriEndpoint() == null || request.getXksProxyUriEndpoint().isEmpty())
            throw new MissingXksProxyEndpointException("xksProxyUriEndpoint required");
        if (request.getXksProxyUriPath() == null || request.getXksProxyUriPath().isEmpty())
            throw new MissingXksProxyPathException("xksProxyUriPath required");
        if (request.getXksProxyAuthenticationCredential() == null || request.getXksProxyAuthenticationCredential().isEmpty())
            throw new MissingXksProxyAuthCredentialException("xksProxyAuthenticationCredential required");
    }

    private void configureCloudHsmStore(CustomKeyStore store, CreateCustomKeyStoreRequestDto request) {
        store.setCloudHsmClusterId(request.getCloudHsmClusterId());
        store.setKeyStorePassword(hashPassword(request.getKeyStorePassword()));
        store.setTrustAnchorCertificate(request.getTrustAnchorCertificate());
        store.setCustomKeyStoreTypeSpecificData(
                String.format("{\"clusterId\":\"%s\",\"hsmType\":\"SOFTWARE_SIMULATED\"}", request.getCloudHsmClusterId()));
    }

    private void configureExternalKeyStore(CustomKeyStore store, CreateCustomKeyStoreRequestDto request) {
        store.setXksProxyUriEndpoint(request.getXksProxyUriEndpoint());
        store.setXksProxyUriPath(request.getXksProxyUriPath());
        store.setXksProxyAuthenticationCredential(hashPassword(request.getXksProxyAuthenticationCredential()));
        store.setCustomKeyStoreTypeSpecificData(
                String.format("{\"endpoint\":\"%s\",\"path\":\"%s\",\"proxyType\":\"INTERNAL_SIMULATED\"}",
                        request.getXksProxyUriEndpoint(), request.getXksProxyUriPath()));
    }

    private void updateCloudHsmStore(CustomKeyStore store, UpdateCustomKeyStoreRequestDto request) {
        if (request.getKeyStorePassword() != null && !request.getKeyStorePassword().isEmpty()) {
            store.setKeyStorePassword(hashPassword(request.getKeyStorePassword()));
        }
        if (request.getTrustAnchorCertificate() != null && !request.getTrustAnchorCertificate().isEmpty()) {
            store.setTrustAnchorCertificate(request.getTrustAnchorCertificate());
        }
    }

    private void updateExternalKeyStore(CustomKeyStore store, UpdateCustomKeyStoreRequestDto request) {
        if (request.getXksProxyUriEndpoint() != null && !request.getXksProxyUriEndpoint().isEmpty()) {
            store.setXksProxyUriEndpoint(request.getXksProxyUriEndpoint());
        }
        if (request.getXksProxyUriPath() != null && !request.getXksProxyUriPath().isEmpty()) {
            store.setXksProxyUriPath(request.getXksProxyUriPath());
        }
        if (request.getXksProxyAuthenticationCredential() != null && !request.getXksProxyAuthenticationCredential().isEmpty()) {
            store.setXksProxyAuthenticationCredential(hashPassword(request.getXksProxyAuthenticationCredential()));
        }
    }

    private void initializeStoreSimulation(CustomKeyStore store) {
        if (store.getType() == IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM) {
            hsmInstances.put(store.getId(), new SoftwareHsmInstance(store.getId(), store.getName()));
        } else {
            externalProxies.put(store.getId(),
                    new ExternalKeyProxyInstance(store.getId(), store.getXksProxyUriEndpoint(), store.getXksProxyUriPath()));
        }
    }

    private void cleanupStoreSimulation(CustomKeyStore store) {
        if (store.getType() == IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM) {
            hsmInstances.remove(store.getId());
        } else {
            externalProxies.remove(store.getId());
        }
        activeConnections.remove(store.getId());
    }

    // Modified establishInternalConnection:
    private boolean establishInternalConnection(CustomKeyStore store) {
        if (store.getType() == IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM) {
            SoftwareHsmInstance hsm = hsmInstances.computeIfAbsent(store.getId(),
                    id -> new SoftwareHsmInstance(store.getId(), store.getName()));
            return hsm.connect(store.getKeyStorePassword());
        } else {
            ExternalKeyProxyInstance proxy = externalProxies.computeIfAbsent(store.getId(),
                    id -> new ExternalKeyProxyInstance(store.getId(), store.getXksProxyUriEndpoint(), store.getXksProxyUriPath()));
            return proxy.connect(store.getXksProxyAuthenticationCredential());
        }
    }

    // Modified closeInternalConnection:
    private void closeInternalConnection(CustomKeyStore store) {
        if (store.getType() == IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM) {
            SoftwareHsmInstance hsm = hsmInstances.get(store.getId());
            if (hsm != null) hsm.disconnect();
        } else {
            ExternalKeyProxyInstance proxy = externalProxies.get(store.getId());
            if (proxy != null) proxy.disconnect();
        }
    }

    private void updateConnectionStatus(CustomKeyStore store) {
        if (store.getStatus() == IEnumCustomKeyStoreStatus.Types.CONNECTED) {
            CustomKeyStoreConnection conn = activeConnections.get(store.getId());
            if (conn == null || conn.isExpired()) {
                store.setStatus(IEnumCustomKeyStoreStatus.Types.FAILED);
                store.setConnectionError("Connection heartbeat expired");
                store.updateHealthStatus();
                customKeyStoreRepository.save(store);
                activeConnections.remove(store.getId());
                log.warn("Store {} connection expired", store.getId());
            } else {
                conn.refresh();
            }
        }
    }

    private void ensureConnected(CustomKeyStore store) {
        if (store.getStatus() != IEnumCustomKeyStoreStatus.Types.CONNECTED) {
            throw new CustomKeyStoreNotConnectedException("Custom key store is not connected.");
        }
        CustomKeyStoreConnection conn = activeConnections.get(store.getId());
        if (conn == null || conn.isExpired()) {
            throw new CustomKeyStoreNotConnectedException("Connection is stale or missing. Reconnect required.");
        }
    }

    private CustomKeyStore findCustomKeyStore(String tenant, Long keyStoreId) {
        return customKeyStoreRepository.findByTenantAndId(tenant, keyStoreId)
                .orElseThrow(() -> new CustomKeyStoreNotFoundException("Store not found: " + keyStoreId));
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not available", e);
        }
    }

    private String convertMapToJson(Map<String, String> map) {
        if (map == null) return null;
        return map.entrySet().stream()
                .map(e -> "\"" + e.getKey() + "\":\"" + e.getValue() + "\"")
                .collect(Collectors.joining(",", "{", "}"));
    }

    private String encodeNextToken(Long id) {
        return Base64.getEncoder().encodeToString(id.toString().getBytes());
    }

    private Long decodeNextToken(String token) {
        try {
            return Long.parseLong(new String(Base64.getDecoder().decode(token)));
        } catch (IllegalArgumentException e) {
            throw new InvalidPaginationTokenException("Invalid pagination token");
        }
    }

    private CustomKeyStoreResponseDto convertToResponseDto(CustomKeyStore store) {
        return CustomKeyStoreResponseDto.builder()
                .keyStoreId(store.getId())
                .keyStoreName(store.getName())
                .type(store.getType())
                .status(store.getStatus())
                .connectionState(store.getConnectionError())
                .createdAt(store.getCreatedAt())
                .updatedAt(store.getUpdatedAt())
                .lastSuccessfulConnection(store.getLastSuccessfulConnection())
                .cloudHsmClusterId(maskSensitive(store.getCloudHsmClusterId()))
                .xksProxyUriEndpoint(maskSensitive(store.getXksProxyUriEndpoint()))
                .xksProxyUriPath(store.getXksProxyUriPath())
                .configuration(store.getMetadata() != null ? parseJsonToMap(store.getMetadata()) : null)
                .build();
    }

    private String maskSensitive(String data) {
        if (data == null || data.length() <= 8) return "***MASKED***";
        return data.substring(0, 4) + "***" + data.substring(data.length() - 4);
    }

    private Map<String, String> parseJsonToMap(String json) {
        if (json == null || json.isEmpty()) return null;
        Map<String, String> map = new HashMap<>();
        String stripped = json.substring(1, json.length() - 1);
        for (String pair : stripped.split(",")) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].replace("\"", "");
                String value = kv[1].replace("\"", "");
                map.put(key, value);
            }
        }
        return map;
    }
}