package eu.isygoit.service.impl;

import eu.isygoit.dto.request.CreateCustomKeyStoreRequestDto;
import eu.isygoit.dto.request.CustomKeyStoreResponseDto;
import eu.isygoit.dto.request.ListCustomKeyStoresResponseDto;
import eu.isygoit.dto.request.UpdateCustomKeyStoreRequestDto;
import eu.isygoit.enums.IEnumCustomKeyStoreStatus;
import eu.isygoit.enums.IEnumCustomKeyStoreType;
import eu.isygoit.exception.*;
import eu.isygoit.model.CustomKeyStore;
import eu.isygoit.repository.CustomKeyStoreRepository;
import eu.isygoit.service.ICustomKeyStoreService;
import eu.isygoit.service.IKeyManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Internal Custom Key Store Service Implementation
 *
 * <p>This service provides a complete, self-contained custom key store implementation
 * that mimics AWS KMS custom key stores but operates entirely within the system.
 * No external dependencies or third-party connections.</p>
 *
 * <p>Supports two types of custom key stores:</p>
 * <ul>
 *   <li><b>CLOUDHSM:</b> Software-based HSM simulation for key storage</li>
 *   <li><b>EXTERNAL_KEY_STORE:</b> External KMS proxy simulation</li>
 * </ul>
 *
 * @author Isygoit Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@Transactional
public class CustomKeyStoreService implements ICustomKeyStoreService {

    private static final String CUSTOM_KEY_STORE_ID_PREFIX = "cks-" ;
    private static final int ID_RANDOM_PART_LENGTH = 16;
    // In-memory connection management for active custom key stores
    private final ConcurrentHashMap<Long, CustomKeyStoreConnection> activeConnections = new ConcurrentHashMap<>();
    // Simulated HSM instances (software-based)
    private final ConcurrentHashMap<Long, SoftwareHsmInstance> hsmInstances = new ConcurrentHashMap<>();
    // Simulated external key proxies
    private final ConcurrentHashMap<Long, ExternalKeyProxyInstance> externalProxies = new ConcurrentHashMap<>();
    @Autowired
    private CustomKeyStoreRepository customKeyStoreRepository;
    @Autowired
    private IKeyManagementService keyManagementService;
    @Value("${kms.custom-key-store.max-stores-per-tenant:10}")
    private int maxStoresPerTenant;
    @Value("${kms.custom-key-store.connection-heartbeat-seconds:60}")
    private int connectionHeartbeatSeconds;

    @Override
    public CustomKeyStoreResponseDto createCustomKeyStore(String tenant, CreateCustomKeyStoreRequestDto request) {
        log.info("Creating internal custom key store for tenant: {}, name: {}, type: {}",
                tenant, request.getKeyStoreName(), request.getType());

        // Validate tenant limits
        validateTenantLimit(tenant);

        // Check for duplicate name
        if (customKeyStoreRepository.existsByTenantAndName(tenant, request.getKeyStoreName())) {
            throw new DuplicateCustomKeyStoreNameException(
                    String.format("Custom key store with name '%s' already exists for tenant", request.getKeyStoreName())
            );
        }

        // Create new custom key store entity
        CustomKeyStore customKeyStore = new CustomKeyStore();
        customKeyStore.setTenant(tenant);
        customKeyStore.setName(request.getKeyStoreName());
        customKeyStore.setType(request.getType());
        customKeyStore.setStatus(IEnumCustomKeyStoreStatus.Types.DISCONNECTED);
        customKeyStore.setCreatedAt(LocalDateTime.now());
        customKeyStore.setUpdatedAt(LocalDateTime.now());

        // Configure based on type
        if (request.getType() == IEnumCustomKeyStoreType.Types.CLOUDHSM) {
            validateCloudHsmRequest(request);
            configureInternalCloudHsmStore(customKeyStore, request);
        } else if (request.getType() == IEnumCustomKeyStoreType.Types.EXTERNAL_KEY_STORE) {
            validateExternalKeyStoreRequest(request);
            configureInternalExternalKeyStore(customKeyStore, request);
        } else {
            throw new UnsupportedCustomKeyStoreTypeException(
                    String.format("Unsupported custom key store type: %s", request.getType())
            );
        }

        // Initialize the underlying store (simulate HSM or external proxy)
        initializeCustomKeyStore(customKeyStore);

        // Save to database
        CustomKeyStore savedStore = customKeyStoreRepository.save(customKeyStore);
        log.info("Internal custom key store created successfully: {}", savedStore.getId());

        return convertToResponseDto(savedStore);
    }

    @Override
    public CustomKeyStoreResponseDto describeCustomKeyStore(String tenant, String keyStoreId) {
        log.info("Describing internal custom key store: {} for tenant: {}", keyStoreId, tenant);

        CustomKeyStore customKeyStore = findCustomKeyStore(tenant, keyStoreId);

        // Update connection status if needed
        updateConnectionStatus(customKeyStore);

        return convertToResponseDto(customKeyStore);
    }

    @Override
    public CustomKeyStoreResponseDto updateCustomKeyStore(String tenant, String keyStoreId,
                                                          UpdateCustomKeyStoreRequestDto request) {
        log.info("Updating internal custom key store: {} for tenant: {}", keyStoreId, tenant);

        CustomKeyStore customKeyStore = findCustomKeyStore(tenant, keyStoreId);

        // Prevent updates while connected
        if (customKeyStore.getStatus() == IEnumCustomKeyStoreStatus.Types.CONNECTED) {
            throw new CustomKeyStoreConnectedException(
                    "Cannot update custom key store while connected. Disconnect it first."
            );
        }

        // Update name if provided
        if (request.getNewKeyStoreName() != null && !request.getNewKeyStoreName().isEmpty()) {
            if (customKeyStoreRepository.existsByTenantAndName(tenant, request.getNewKeyStoreName())) {
                throw new DuplicateCustomKeyStoreNameException(
                        String.format("Custom key store name '%s' already exists", request.getNewKeyStoreName())
                );
            }
            customKeyStore.setName(request.getNewKeyStoreName());
        }

        // Update type-specific configuration
        if (customKeyStore.getType() == IEnumCustomKeyStoreType.Types.CLOUDHSM) {
            updateInternalCloudHsmStore(customKeyStore, request);
        } else if (customKeyStore.getType() == IEnumCustomKeyStoreType.Types.EXTERNAL_KEY_STORE) {
            updateInternalExternalKeyStore(customKeyStore, request);
        }

        customKeyStore.setUpdatedAt(LocalDateTime.now());
        CustomKeyStore updatedStore = customKeyStoreRepository.save(customKeyStore);

        log.info("Internal custom key store updated successfully: {}", keyStoreId);
        return convertToResponseDto(updatedStore);
    }

    @Override
    public void deleteCustomKeyStore(String tenant, String keyStoreId) {
        log.info("Deleting internal custom key store: {} for tenant: {}", keyStoreId, tenant);

        CustomKeyStore customKeyStore = findCustomKeyStore(tenant, keyStoreId);

        // Validate that store has no KMS keys
        int keyCount = keyManagementService.countKeysInCustomKeyStore(tenant, keyStoreId);
        if (keyCount > 0) {
            throw new CustomKeyStoreHasKeysException(
                    String.format("Cannot delete custom key store that contains %d KMS keys. Delete all keys first.", keyCount)
            );
        }

        // Must be disconnected
        if (customKeyStore.getStatus() != IEnumCustomKeyStoreStatus.Types.DISCONNECTED) {
            throw new CustomKeyStoreConnectedException(
                    "Custom key store must be disconnected before deletion"
            );
        }

        // Clean up resources
        cleanupCustomKeyStore(customKeyStore);

        customKeyStoreRepository.delete(customKeyStore);
        log.info("Internal custom key store deleted successfully: {}", keyStoreId);
    }

    @Override
    public ListCustomKeyStoresResponseDto listCustomKeyStores(String tenant, Integer limit, String nextToken) {
        log.info("Listing internal custom key stores for tenant: {}, limit: {}", tenant, limit);

        int pageSize = (limit != null && limit > 0 && limit <= 1000) ? limit : 100;

        List<CustomKeyStore> stores;
        String newNextToken = null;

        if (nextToken != null && !nextToken.isEmpty()) {
            Long lastId = decodeNextToken(nextToken);
            stores = customKeyStoreRepository.findByTenantAndIdGreaterThanOrderByIdAsc(tenant, lastId, pageSize);
        } else {
            stores = customKeyStoreRepository.findByTenantOrderByIdAsc(tenant, pageSize);
        }

        if (stores.size() == pageSize) {
            Long lastId = stores.get(stores.size() - 1).getId();
            newNextToken = encodeNextToken(lastId);
        }

        List<CustomKeyStoreResponseDto> storeDtos = stores.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());

        return ListCustomKeyStoresResponseDto.builder()
                .customKeyStores(storeDtos)
                .nextToken(newNextToken)        // Token to fetch next page
                .truncated(newNextToken != null) // true if there's a nextToken
                .build();
    }

    @Override
    public void connectCustomKeyStore(String tenant, String keyStoreId) {
        log.info("Connecting internal custom key store: {} for tenant: {}", keyStoreId, tenant);

        CustomKeyStore customKeyStore = findCustomKeyStore(tenant, keyStoreId);

        if (customKeyStore.getStatus() == IEnumCustomKeyStoreStatus.Types.CONNECTED) {
            throw new CustomKeyStoreAlreadyConnectedException("Custom key store is already connected");
        }

        if (customKeyStore.getStatus() == IEnumCustomKeyStoreStatus.Types.CONNECTING) {
            throw new CustomKeyStoreConnectingException("Custom key store is already in connecting state");
        }

        // Update status to CONNECTING
        customKeyStore.setStatus(IEnumCustomKeyStoreStatus.Types.CONNECTING);
        customKeyStore.setLastConnectionAttempt(LocalDateTime.now());
        customKeyStoreRepository.save(customKeyStore);

        try {
            // Establish connection to internal store
            boolean connected = establishInternalConnection(customKeyStore);

            if (connected) {
                customKeyStore.setStatus(IEnumCustomKeyStoreStatus.Types.CONNECTED);
                customKeyStore.setLastSuccessfulConnection(LocalDateTime.now());
                customKeyStore.setConnectionError(null);

                // Create and store connection object
                CustomKeyStoreConnection connection = new CustomKeyStoreConnection(
                        customKeyStore.getId(),
                        LocalDateTime.now(),
                        connectionHeartbeatSeconds
                );
                activeConnections.put(customKeyStore.getId(), connection);

                log.info("Successfully connected internal custom key store: {}", keyStoreId);
            } else {
                customKeyStore.setStatus(IEnumCustomKeyStoreStatus.Types.FAILED);
                customKeyStore.setConnectionError("Internal connection failed");
                throw new CustomKeyStoreConnectionException("Failed to connect custom key store");
            }
        } catch (Exception e) {
            customKeyStore.setStatus(IEnumCustomKeyStoreStatus.Types.FAILED);
            customKeyStore.setConnectionError(e.getMessage());
            throw new CustomKeyStoreConnectionException(
                    String.format("Connection failed: %s", e.getMessage()));
        } finally {
            customKeyStore.setUpdatedAt(LocalDateTime.now());
            customKeyStoreRepository.save(customKeyStore);
        }
    }

    @Override
    public void disconnectCustomKeyStore(String tenant, String keyStoreId) {
        log.info("Disconnecting internal custom key store: {} for tenant: {}", keyStoreId, tenant);

        CustomKeyStore customKeyStore = findCustomKeyStore(tenant, keyStoreId);

        if (customKeyStore.getStatus() != IEnumCustomKeyStoreStatus.Types.CONNECTED) {
            throw new CustomKeyStoreNotConnectedException("Custom key store is not connected");
        }

        try {
            // Close internal connection
            closeInternalConnection(customKeyStore);

            // Remove from active connections
            activeConnections.remove(customKeyStore.getId());

            customKeyStore.setStatus(IEnumCustomKeyStoreStatus.Types.DISCONNECTED);
            customKeyStore.setUpdatedAt(LocalDateTime.now());
            customKeyStoreRepository.save(customKeyStore);

            log.info("Successfully disconnected internal custom key store: {}", keyStoreId);
        } catch (Exception e) {
            log.error("Failed to disconnect custom key store: {}", keyStoreId, e);
            throw new CustomKeyStoreDisconnectionException(String.format("Disconnection failed: %s", e.getMessage()));
        }
    }

    // ============================================================================
    // INTERNAL STORE IMPLEMENTATION METHODS
    // ============================================================================

    private void initializeCustomKeyStore(CustomKeyStore customKeyStore) {
        if (customKeyStore.getType() == IEnumCustomKeyStoreType.Types.CLOUDHSM) {
            // Create a simulated HSM instance
            SoftwareHsmInstance hsm = new SoftwareHsmInstance(
                    customKeyStore.getId(),
                    customKeyStore.getName()
            );
            hsmInstances.put(customKeyStore.getId(), hsm);
            log.info("Initialized software HSM instance for custom key store: {}", customKeyStore.getId());
        } else if (customKeyStore.getType() == IEnumCustomKeyStoreType.Types.EXTERNAL_KEY_STORE) {
            // Create a simulated external proxy
            ExternalKeyProxyInstance proxy = new ExternalKeyProxyInstance(
                    customKeyStore.getId(),
                    customKeyStore.getXksProxyUriEndpoint(),
                    customKeyStore.getXksProxyUriPath()
            );
            externalProxies.put(customKeyStore.getId(), proxy);
            log.info("Initialized external proxy instance for custom key store: {}", customKeyStore.getId());
        }
    }

    private void cleanupCustomKeyStore(CustomKeyStore customKeyStore) {
        if (customKeyStore.getType() == IEnumCustomKeyStoreType.Types.CLOUDHSM) {
            hsmInstances.remove(customKeyStore.getId());
        } else if (customKeyStore.getType() == IEnumCustomKeyStoreType.Types.EXTERNAL_KEY_STORE) {
            externalProxies.remove(customKeyStore.getId());
        }
        activeConnections.remove(customKeyStore.getId());
    }

    private boolean establishInternalConnection(CustomKeyStore customKeyStore) {
        if (customKeyStore.getType() == IEnumCustomKeyStoreType.Types.CLOUDHSM) {
            SoftwareHsmInstance hsm = hsmInstances.get(customKeyStore.getId());
            if (hsm != null) {
                return hsm.connect(customKeyStore.getKeyStorePassword());
            }
        } else if (customKeyStore.getType() == IEnumCustomKeyStoreType.Types.EXTERNAL_KEY_STORE) {
            ExternalKeyProxyInstance proxy = externalProxies.get(customKeyStore.getId());
            if (proxy != null) {
                return proxy.connect(customKeyStore.getXksProxyAuthenticationCredential());
            }
        }
        return false;
    }

    private void closeInternalConnection(CustomKeyStore customKeyStore) {
        if (customKeyStore.getType() == IEnumCustomKeyStoreType.Types.CLOUDHSM) {
            SoftwareHsmInstance hsm = hsmInstances.get(customKeyStore.getId());
            if (hsm != null) {
                hsm.disconnect();
            }
        } else if (customKeyStore.getType() == IEnumCustomKeyStoreType.Types.EXTERNAL_KEY_STORE) {
            ExternalKeyProxyInstance proxy = externalProxies.get(customKeyStore.getId());
            if (proxy != null) {
                proxy.disconnect();
            }
        }
    }

    private void updateConnectionStatus(CustomKeyStore customKeyStore) {
        if (customKeyStore.getStatus() == IEnumCustomKeyStoreStatus.Types.CONNECTED) {
            CustomKeyStoreConnection connection = activeConnections.get(customKeyStore.getId());
            if (connection != null && connection.isExpired()) {
                customKeyStore.setStatus(IEnumCustomKeyStoreStatus.Types.FAILED);
                customKeyStore.setConnectionError("Connection heartbeat expired");
                customKeyStoreRepository.save(customKeyStore);
                activeConnections.remove(customKeyStore.getId());
                log.warn("Custom key store connection expired: {}", customKeyStore.getId());
            } else if (connection != null) {
                connection.refresh();
            }
        }
    }

    // ============================================================================
    // VALIDATION AND CONFIGURATION METHODS
    // ============================================================================

    private void validateTenantLimit(String tenant) {
        long count = customKeyStoreRepository.countByTenant(tenant);
        if (count >= maxStoresPerTenant) {
            throw new TenantCustomKeyStoreLimitExceededException(
                    String.format("Maximum number of custom key stores (%d) reached for tenant", maxStoresPerTenant)
            );
        }
    }

    private void validateCloudHsmRequest(CreateCustomKeyStoreRequestDto request) {
        if (request.getCloudHsmClusterId() == null || request.getCloudHsmClusterId().isEmpty()) {
            throw new MissingCloudHsmClusterIdException("CloudHSM cluster ID is required for CLOUDHSM type");
        }
        if (request.getKeyStorePassword() == null || request.getKeyStorePassword().isEmpty()) {
            throw new MissingKeyStorePasswordException("Key store password is required for CLOUDHSM type");
        }
        if (request.getTrustAnchorCertificate() == null || request.getTrustAnchorCertificate().isEmpty()) {
            throw new MissingTrustAnchorCertificateException("Trust anchor certificate is required for CLOUDHSM type");
        }
    }

    private void validateExternalKeyStoreRequest(CreateCustomKeyStoreRequestDto request) {
        if (request.getXksProxyUriEndpoint() == null || request.getXksProxyUriEndpoint().isEmpty()) {
            throw new MissingXksProxyEndpointException("XKS proxy URI endpoint is required for EXTERNAL_KEY_STORE type");
        }
        if (request.getXksProxyUriPath() == null || request.getXksProxyUriPath().isEmpty()) {
            throw new MissingXksProxyPathException("XKS proxy URI path is required for EXTERNAL_KEY_STORE type");
        }
        if (request.getXksProxyAuthenticationCredential() == null ||
                request.getXksProxyAuthenticationCredential().isEmpty()) {
            throw new MissingXksProxyAuthCredentialException(
                    "XKS proxy authentication credential is required for EXTERNAL_KEY_STORE type"
            );
        }
    }

    private void configureInternalCloudHsmStore(CustomKeyStore customKeyStore, CreateCustomKeyStoreRequestDto request) {
        customKeyStore.setCloudHsmClusterId(request.getCloudHsmClusterId());

        // Store password hash (not the actual password)
        String passwordHash = hashPassword(request.getKeyStorePassword());
        customKeyStore.setKeyStorePassword(passwordHash);

        // Store trust anchor certificate
        customKeyStore.setTrustAnchorCertificate(request.getTrustAnchorCertificate());

        // Store additional configuration
        customKeyStore.setCustomKeyStoreTypeSpecificData(
                String.format("{\"cloudHsmClusterId\":\"%s\",\"hsmType\":\"SOFTWARE_SIMULATED\"}",
                        request.getCloudHsmClusterId())
        );
    }

    private void configureInternalExternalKeyStore(CustomKeyStore customKeyStore, CreateCustomKeyStoreRequestDto request) {
        customKeyStore.setXksProxyUriEndpoint(request.getXksProxyUriEndpoint());
        customKeyStore.setXksProxyUriPath(request.getXksProxyUriPath());

        // Store auth credential hash
        String authHash = hashPassword(request.getXksProxyAuthenticationCredential());
        customKeyStore.setXksProxyAuthenticationCredential(authHash);

        // Store XKS specific configuration
        String xksConfig = String.format(
                "{\"uriEndpoint\":\"%s\",\"uriPath\":\"%s\",\"proxyType\":\"INTERNAL_SIMULATED\"}",
                request.getXksProxyUriEndpoint(),
                request.getXksProxyUriPath()
        );
        customKeyStore.setCustomKeyStoreTypeSpecificData(xksConfig);
    }

    private void updateInternalCloudHsmStore(CustomKeyStore customKeyStore, UpdateCustomKeyStoreRequestDto request) {
        if (request.getKeyStorePassword() != null && !request.getKeyStorePassword().isEmpty()) {
            String passwordHash = hashPassword(request.getKeyStorePassword());
            customKeyStore.setKeyStorePassword(passwordHash);
        }
        // CloudHSM cluster ID cannot be updated after creation
    }

    private void updateInternalExternalKeyStore(CustomKeyStore customKeyStore, UpdateCustomKeyStoreRequestDto request) {
        if (request.getXksProxyUriEndpoint() != null && !request.getXksProxyUriEndpoint().isEmpty()) {
            customKeyStore.setXksProxyUriEndpoint(request.getXksProxyUriEndpoint());
        }
        if (request.getXksProxyUriPath() != null && !request.getXksProxyUriPath().isEmpty()) {
            customKeyStore.setXksProxyUriPath(request.getXksProxyUriPath());
        }
        if (request.getXksProxyAuthenticationCredential() != null &&
                !request.getXksProxyAuthenticationCredential().isEmpty()) {
            String authHash = hashPassword(request.getXksProxyAuthenticationCredential());
            customKeyStore.setXksProxyAuthenticationCredential(authHash);
        }
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    private CustomKeyStore findCustomKeyStore(String tenant, String keyStoreId) {
        return customKeyStoreRepository.findByTenantAndCustomKeyStoreId(tenant, keyStoreId)
                .orElseThrow(() -> new CustomKeyStoreNotFoundException(
                        String.format("Custom key store not found: %s", keyStoreId)
                ));
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to hash password", e);
            return password;
        }
    }

    private String maskSensitiveData(String data) {
        if (data == null || data.length() <= 8) {
            return "***MASKED***" ;
        }
        return data.substring(0, 4) + "***" + data.substring(data.length() - 4);
    }

    private String encodeNextToken(Long id) {
        return Base64.getEncoder().encodeToString(id.toString().getBytes());
    }

    private Long decodeNextToken(String nextToken) {
        try {
            String decoded = new String(Base64.getDecoder().decode(nextToken));
            return Long.parseLong(decoded);
        } catch (Exception e) {
            throw new InvalidPaginationTokenException("Invalid pagination token");
        }
    }

    private CustomKeyStoreResponseDto convertToResponseDto(CustomKeyStore customKeyStore) {
        return CustomKeyStoreResponseDto.builder()
                .keyStoreId(customKeyStore.getId())
                .keyStoreName(customKeyStore.getName())
                .type(customKeyStore.getType())
                .status(customKeyStore.getStatus())
                .connectionState(customKeyStore.getConnectionError())
                .createdAt(customKeyStore.getCreatedAt())
                .updatedAt(customKeyStore.getUpdatedAt())
                .lastSuccessfulConnection(customKeyStore.getLastSuccessfulConnection())
                .cloudHsmClusterId(maskSensitiveData(customKeyStore.getCloudHsmClusterId()))
                .xksProxyUriEndpoint(maskSensitiveData(customKeyStore.getXksProxyUriEndpoint()))
                .xksProxyUriPath(customKeyStore.getXksProxyUriPath())
                .build();
    }

    // ============================================================================
    // INNER CLASSES FOR INTERNAL SIMULATION
    // ============================================================================

    /**
     * Internal class representing a software-simulated HSM instance
     */
    private static class SoftwareHsmInstance {
        private final Long storeId;
        private final String name;
        private final ConcurrentHashMap<String, SecretKey> keys;
        private boolean connected;
        private LocalDateTime connectedAt;

        public SoftwareHsmInstance(Long storeId, String name) {
            this.storeId = storeId;
            this.name = name;
            this.connected = false;
            this.keys = new ConcurrentHashMap<>();
        }

        public boolean connect(String password) {
            // Validate password (simplified)
            if (password != null && !password.isEmpty()) {
                this.connected = true;
                this.connectedAt = LocalDateTime.now();
                log.info("Software HSM instance '{}' connected", storeId);
                return true;
            }
            return false;
        }

        public void disconnect() {
            this.connected = false;
            this.connectedAt = null;
            log.info("Software HSM instance '{}' disconnected", storeId);
        }

        public boolean isConnected() {
            return connected;
        }

        public SecretKey generateKey(String algorithm, int keySize) throws Exception {
            if (!connected) {
                throw new IllegalStateException("HSM not connected");
            }
            KeyGenerator keyGen = KeyGenerator.getInstance(algorithm);
            keyGen.init(keySize);
            SecretKey key = keyGen.generateKey();
            keys.put(key.hashCode() + "", key);
            return key;
        }
    }

    /**
     * Internal class representing a simulated external key proxy
     */
    private static class ExternalKeyProxyInstance {
        private final Long storeId;
        private final String endpoint;
        private final String path;
        private boolean connected;
        private String sessionId;

        public ExternalKeyProxyInstance(Long storeId, String endpoint, String path) {
            this.storeId = storeId;
            this.endpoint = endpoint;
            this.path = path;
            this.connected = false;
        }

        public boolean connect(String authCredential) {
            if (authCredential != null && !authCredential.isEmpty()) {
                this.connected = true;
                this.sessionId = UUID.randomUUID().toString();
                log.info("External proxy instance '{}' connected with session: {}", storeId, sessionId);
                return true;
            }
            return false;
        }

        public void disconnect() {
            this.connected = false;
            this.sessionId = null;
            log.info("External proxy instance '{}' disconnected", storeId);
        }

        public boolean isConnected() {
            return connected;
        }
    }

    /**
     * Internal class for tracking active connections
     */
    private static class CustomKeyStoreConnection {
        private final Long storeId;
        private final int timeoutSeconds;
        private LocalDateTime lastHeartbeat;

        public CustomKeyStoreConnection(Long storeId, LocalDateTime connectedAt, int timeoutSeconds) {
            this.storeId = storeId;
            this.lastHeartbeat = connectedAt;
            this.timeoutSeconds = timeoutSeconds;
        }

        public void refresh() {
            this.lastHeartbeat = LocalDateTime.now();
        }

        public boolean isExpired() {
            return lastHeartbeat.plusSeconds(timeoutSeconds).isBefore(LocalDateTime.now());
        }
    }
}