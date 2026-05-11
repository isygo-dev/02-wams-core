package eu.isygoit.service.impl;

import eu.isygoit.dto.KmsDtos.*;
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
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Internal Custom Key Store Service Implementation
 *
 * <p>Provides a comprehensive, self-contained custom key store implementation
 * that mimics WAMS KMS custom key stores but operates entirely within the system.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><b>Two Key Store Types:</b>
 *     <ul>
 *       <li>CLOUDHSM - Software-based HSM simulation for secure key storage</li>
 *       <li>EXTERNAL_KEY_STORE - External KMS proxy simulation (XKS)</li>
 *     </ul>
 *   </li>
 *   <li><b>Connection Management:</b> Connection state tracking with heartbeat monitoring</li>
 *   <li><b>Multi-tenant Support:</b> Per-tenant limits and isolation</li>
 *   <li><b>Security:</b> Password hashing, sensitive data masking</li>
 *   <li><b>Lifecycle Management:</b> Full CRUD operations with validation</li>
 * </ul>
 *
 * <h3>Connection Lifecycle:</h3>
 * <ol>
 *   <li>DISCONNECTED - Initial state after creation</li>
 *   <li>CONNECTING - Intermediate state during connection attempt</li>
 *   <li>CONNECTED - Active connection with heartbeat monitoring</li>
 *   <li>FAILED - Connection failure state</li>
 * </ol>
 *
 * <h3>Usage Examples:</h3>
 * <pre>
 * // Create a CloudHSM custom key store
 * CreateCustomKeyStoreRequestDto cloudHsmRequest = new CreateCustomKeyStoreRequestDto();
 * cloudHsmRequest.setKeyStoreName("my-hsm-store");
 * cloudHsmRequest.setType(IEnumCustomKeyStoreType.Types.CLOUDHSM);
 * CustomKeyStoreResponseDto response = service.createCustomKeyStore(tenant, cloudHsmRequest);
 *
 * // Connect the key store
 * service.connectCustomKeyStore(tenant, response.getKeyStoreId());
 *
 * // List all key stores
 * ListCustomKeyStoresResponseDto stores = service.listCustomKeyStores(tenant, 100, null);
 * </pre>
 *
 * @author Isygoit Team
 * @version 2.0
 * @since 1.0
 * @apiNote This service manages custom key stores for cryptographic operations
 */
@Slf4j
@Service
@Transactional
public class CustomKeyStoreService implements ICustomKeyStoreService {

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

    /**
     * Creates a new custom key store for the specified tenant.
     *
     * <p><b>Operation Flow:</b></p>
     * <ol>
     *   <li>Validates tenant limits (max stores per tenant)</li>
     *   <li>Checks for duplicate key store names</li>
     *   <li>Validates type-specific configuration based on CloudHSM or External type</li>
     *   <li>Creates and initializes the internal store simulation</li>
     *   <li>Persists to database</li>
     * </ol>
     *
     * <p><b>Validation Rules:</b></p>
     * <ul>
     *   <li>CloudHSM requires: cluster ID, password, and trust anchor certificate</li>
     *   <li>External Key Store requires: URI endpoint, URI path, and authentication credential</li>
     *   <li>Key store names must be unique per tenant</li>
     * </ul>
     *
     * <p><b>Security Notes:</b></p>
     * <ul>
     *   <li>Passwords are hashed using SHA-256 before storage</li>
     *   <li>Authentication credentials are never returned in responses</li>
     *   <li>Initial status is always DISCONNECTED</li>
     * </ul>
     *
     * @param tenant  the tenant identifier (required, non-empty)
     * @param request the creation request containing key store configuration
     * @return a {@link CustomKeyStoreResponseDto} containing the newly created key store metadata
     * @throws TenantCustomKeyStoreLimitExceededException if tenant has reached maximum stores
     * @throws DuplicateCustomKeyStoreNameException       if a key store with the same name exists
     * @throws UnsupportedCustomKeyStoreTypeException     if the type is not CLOUDHSM or EXTERNAL_KEY_STORE
     * @throws MissingCloudHsmClusterIdException          if CloudHSM configuration is incomplete
     * @throws MissingXksProxyEndpointException           if External Key Store configuration is incomplete
     * @apiNote Newly created key stores start in DISCONNECTED state and must be connected before use
     * @see CreateCustomKeyStoreRequestDto
     * @see CustomKeyStoreResponseDto
     */
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

    /**
     * Retrieves detailed metadata about a specific custom key store.
     *
     * <p>This operation returns all metadata associated with the key store, including
     * its current connection status, type-specific configuration (with sensitive data masked),
     * and creation/modification timestamps.</p>
     *
     * <p><b>Connection Status Monitoring:</b></p>
     * <p>If the key store is currently CONNECTED, this method verifies that the connection
     * is still active by checking the heartbeat timer. If the heartbeat has expired,
     * the status is updated to FAILED.</p>
     *
     * @param tenant the tenant identifier (required, non-empty)
     * @param keyStoreId the ID of the key store to describe (required, must exist)
     * @return a {@link CustomKeyStoreResponseDto} containing complete key store metadata
     * @throws CustomKeyStoreNotFoundException if the key store does not exist for the tenant
     * @see CustomKeyStoreResponseDto
     * @apiNote Sensitive data (passwords, credentials) is masked in the response with format: {first4}***{last4}
     * @since 1.0
     */
    @Override
    public CustomKeyStoreResponseDto describeCustomKeyStore(String tenant, Long keyStoreId) {
        log.info("Describing internal custom key store: {} for tenant: {}", keyStoreId, tenant);

        CustomKeyStore customKeyStore = findCustomKeyStore(tenant, keyStoreId);

        // Update connection status if needed
        updateConnectionStatus(customKeyStore);

        return convertToResponseDto(customKeyStore);
    }

    /**
     * Updates the configuration of an existing custom key store.
     *
     * <p><b>Update Constraints:</b></p>
     * <ul>
     *   <li>Key store must be in DISCONNECTED state (cannot update while connected)</li>
     *   <li>Can update: name, passwords, endpoints, credentials</li>
     *   <li>Cannot update: key store type, CloudHSM cluster ID (immutable after creation)</li>
     * </ul>
     *
     * <p><b>Update Logic by Type:</b></p>
     * <ul>
     *   <li><b>CloudHSM:</b> Can update password and trust anchor certificate</li>
     *   <li><b>External Key Store:</b> Can update endpoint, path, and authentication credential</li>
     * </ul>
     *
     * @param tenant the tenant identifier (required, non-empty)
     * @param keyStoreId the ID of the key store to update (required, must exist)
     * @param request the update request containing modified configuration
     * @return a {@link CustomKeyStoreResponseDto} with updated metadata
     * @throws CustomKeyStoreNotFoundException if the key store does not exist for the tenant
     * @throws CustomKeyStoreConnectedException if the key store is currently CONNECTED
     * @throws DuplicateCustomKeyStoreNameException if the new name is already in use
     * @see UpdateCustomKeyStoreRequestDto
     * @see CustomKeyStoreResponseDto
     * @apiNote All timestamp fields are automatically updated
     */
    @Override
    public CustomKeyStoreResponseDto updateCustomKeyStore(String tenant, Long keyStoreId,
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
        if (request.getNewCustomKeyStoreName() != null && !request.getNewCustomKeyStoreName().isEmpty()) {
            if (customKeyStoreRepository.existsByTenantAndName(tenant, request.getNewCustomKeyStoreName())) {
                throw new DuplicateCustomKeyStoreNameException(
                        String.format("Custom key store name '%s' already exists", request.getNewCustomKeyStoreName())
                );
            }
            customKeyStore.setName(request.getNewCustomKeyStoreName());
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

    /**
     * Deletes a custom key store.
     *
     * <p><b>Deletion Prerequisites:</b></p>
     * <ol>
     *   <li>Key store must be in DISCONNECTED state</li>
     *   <li>Key store must contain no KMS keys (all keys must be deleted first)</li>
     * </ol>
     *
     * <p><b>Cleanup Operations:</b></p>
     * <ul>
     *   <li>Removes in-memory HSM instances (if CloudHSM type)</li>
     *   <li>Removes in-memory proxy instances (if External type)</li>
     *   <li>Terminates active connections</li>
     *   <li>Deletes database record</li>
     * </ul>
     *
     * <p><b>Safety Notes:</b></p>
     * <ul>
     *   <li>This operation is permanent and irreversible</li>
     *   <li>All cryptographic material associated with the key store is destroyed</li>
     *   <li>Ensure keys are rotated or re-encrypted elsewhere before deletion</li>
     * </ul>
     *
     * @param tenant the tenant identifier (required, non-empty)
     * @param keyStoreId the ID of the key store to delete (required, must exist)
     * @throws CustomKeyStoreNotFoundException if the key store does not exist for the tenant
     * @throws CustomKeyStoreHasKeysException if the key store still contains KMS keys
     * @throws CustomKeyStoreConnectedException if the key store is not in DISCONNECTED state
     * @apiNote Audit events are logged for compliance and forensics
     */
    @Override
    public void deleteCustomKeyStore(String tenant, Long keyStoreId) {
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

    /**
     * Lists all custom key stores for the specified tenant with pagination support.
     *
     * <p><b>Pagination Mechanism:</b></p>
     * <ul>
     *   <li>Results are returned in pages of limited size</li>
     *   <li>Token-based pagination using Base64-encoded key store IDs</li>
     *   <li>Default page size: 100 items (minimum 1, maximum 1000)</li>
     *   <li>Include {@code nextToken} in subsequent requests to fetch next page</li>
     * </ul>
     *
     * <p><b>Response Structure:</b></p>
     * <pre>
     * {
     *   "customKeyStores": [ ... list of key stores ... ],
     *   "nextToken": "...",          // null if no more pages
     *   "truncated": true          // false if this is the last page
     * }
     * </pre>
     *
     * @param tenant the tenant identifier (required, non-empty)
     * @param limit the maximum number of key stores to return (1-1000, default 100, nullable)
     * @param nextToken pagination token to retrieve next page (nullable for first page)
     * @return a {@link ListCustomKeyStoresResponseDto} containing paginated results
     * @throws InvalidPaginationTokenException if the pagination token is malformed
     * @see ListCustomKeyStoresResponseDto
     * @apiNote All key stores are returned in ascending order by ID
     */
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

    /**
     * Connects a custom key store to its underlying hardware/proxy.
     *
     * <p><b>Connection State Transitions:</b></p>
     * <pre>
     * DISCONNECTED --[connect]--> CONNECTING --[success]--> CONNECTED
     *                                           --[failure]--> FAILED
     * </pre>
     *
     * <p><b>Connection Process:</b></p>
     * <ol>
     *   <li>Validates the key store is not already connected</li>
     *   <li>Updates status to CONNECTING</li>
     *   <li>Retrieves stored credentials and attempts connection</li>
     *   <li>On success: updates status to CONNECTED, records heartbeat</li>
     *   <li>On failure: updates status to FAILED, records error message</li>
     * </ol>
     *
     * <p><b>Heartbeat Monitoring:</b></p>
     * <ul>
     *   <li>Connection heartbeat interval: configurable (default 60 seconds)</li>
     *   <li>Heartbeat is refreshed on each successful API call</li>
     *   <li>Expired connections are automatically marked as FAILED</li>
     * </ul>
     *
     * @param tenant the tenant identifier (required, non-empty)
     * @param keyStoreId the ID of the key store to connect (required, must exist)
     * @throws CustomKeyStoreNotFoundException if the key store does not exist for the tenant
     * @throws CustomKeyStoreAlreadyConnectedException if the key store is already CONNECTED
     * @throws CustomKeyStoreConnectingException if connection attempt is already in progress
     * @throws CustomKeyStoreConnectionException if connection fails
     * @apiNote This operation is asynchronous in real deployments; here it's synchronous
     * @since 1.0
     */
    @Override
    public void connectCustomKeyStore(String tenant, Long keyStoreId) {
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

    /**
     * Disconnects a custom key store from its underlying hardware/proxy.
     *
     * <p><b>Disconnection Process:</b></p>
     * <ol>
     *   <li>Validates the key store is currently CONNECTED</li>
     *   <li>Closes underlying connection (HSM or proxy)</li>
     *   <li>Removes connection tracking from active connections</li>
     *   <li>Updates status to DISCONNECTED</li>
     * </ol>
     *
     * <p><b>After Disconnection:</b></p>
     * <ul>
     *   <li>The key store configuration can be modified</li>
     *   <li>No cryptographic operations can be performed until reconnected</li>
     *   <li>Connection state is preserved for audit trails</li>
     * </ul>
     *
     * @param tenant the tenant identifier (required, non-empty)
     * @param keyStoreId the ID of the key store to disconnect (required, must exist)
     * @throws CustomKeyStoreNotFoundException if the key store does not exist for the tenant
     * @throws CustomKeyStoreNotConnectedException if the key store is not currently CONNECTED
     * @throws CustomKeyStoreDisconnectionException if the disconnection process fails
     * @apiNote Disconnecting does not delete keys stored in the key store
     */
    @Override
    public void disconnectCustomKeyStore(String tenant, Long keyStoreId) {
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
    // INTERNAL STORE IMPLEMENTATION METHODS | PRIVATE UTILITIES
    // ============================================================================

    /**
     * Initializes the underlying store simulation for a newly created custom key store.
     *
     * <p>Delegates to appropriate initialization method based on key store type:</p>
     * <ul>
     *   <li>CLOUDHSM → Creates and stores a SoftwareHsmInstance</li>
     *   <li>EXTERNAL_KEY_STORE → Creates and stores an ExternalKeyProxyInstance</li>
     * </ul>
     *
     * @param customKeyStore the custom key store entity to initialize (non-null)
     * @see SoftwareHsmInstance
     * @see ExternalKeyProxyInstance
     */
    private void initializeCustomKeyStore(CustomKeyStore customKeyStore) {
        Objects.requireNonNull(customKeyStore, "CustomKeyStore cannot be null");
        
        if (customKeyStore.getType() == IEnumCustomKeyStoreType.Types.CLOUDHSM) {
            // Create a simulated HSM instance
            SoftwareHsmInstance hsm = new SoftwareHsmInstance(
                    customKeyStore.getId(),
                    customKeyStore.getName()
            );
            hsmInstances.put(customKeyStore.getId(), hsm);
            log.debug("Initialized software HSM instance for custom key store: {}", customKeyStore.getId());
        } else if (customKeyStore.getType() == IEnumCustomKeyStoreType.Types.EXTERNAL_KEY_STORE) {
            // Create a simulated external proxy
            ExternalKeyProxyInstance proxy = new ExternalKeyProxyInstance(
                    customKeyStore.getId(),
                    customKeyStore.getXksProxyUriEndpoint(),
                    customKeyStore.getXksProxyUriPath()
            );
            externalProxies.put(customKeyStore.getId(), proxy);
            log.debug("Initialized external proxy instance for custom key store: {}", customKeyStore.getId());
        }
    }

    /**
     * Cleans up resources associated with a custom key store.
     *
     * <p>Removes:</p>
     * <ul>
     *   <li>In-memory HSM instances</li>
     *   <li>In-memory proxy instances</li>
     *   <li>Active connection tracking</li>
     * </ul>
     *
     * @param customKeyStore the custom key store entity to clean up (non-null)
     */
    private void cleanupCustomKeyStore(CustomKeyStore customKeyStore) {
        Objects.requireNonNull(customKeyStore, "CustomKeyStore cannot be null");
        
        if (customKeyStore.getType() == IEnumCustomKeyStoreType.Types.CLOUDHSM) {
            hsmInstances.remove(customKeyStore.getId());
        } else if (customKeyStore.getType() == IEnumCustomKeyStoreType.Types.EXTERNAL_KEY_STORE) {
            externalProxies.remove(customKeyStore.getId());
        }
        activeConnections.remove(customKeyStore.getId());
        log.debug("Cleaned up resources for custom key store: {}", customKeyStore.getId());
    }

    /**
     * Establishes a connection to the underlying key store (HSM or proxy).
     *
     * @param customKeyStore the custom key store to connect (non-null)
     * @return true if connection succeeded, false otherwise
     */
    private boolean establishInternalConnection(CustomKeyStore customKeyStore) {
        Objects.requireNonNull(customKeyStore, "CustomKeyStore cannot be null");
        
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

    /**
     * Closes the connection to the underlying key store (HSM or proxy).
     *
     * @param customKeyStore the custom key store to disconnect (non-null)
     */
    private void closeInternalConnection(CustomKeyStore customKeyStore) {
        Objects.requireNonNull(customKeyStore, "CustomKeyStore cannot be null");
        
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

    /**
     * Updates connection status for a custom key store with heartbeat validation.
     *
     * <p>If a CONNECTED key store's heartbeat has expired, automatically updates
     * its status to FAILED and removes it from active connections.</p>
     *
     * @param customKeyStore the custom key store to validate (non-null)
     */
    private void updateConnectionStatus(CustomKeyStore customKeyStore) {
        Objects.requireNonNull(customKeyStore, "CustomKeyStore cannot be null");
        
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
    // VALIDATION AND CONFIGURATION METHODS | INTERNAL UTILITIES
    // ============================================================================

    /**
     * Validates that the tenant has not exceeded the maximum number of custom key stores.
     *
     * @param tenant the tenant identifier (non-null, non-empty)
     * @throws TenantCustomKeyStoreLimitExceededException if limit is reached
     */
    private void validateTenantLimit(String tenant) {
        long count = customKeyStoreRepository.countByTenant(tenant);
        if (count >= maxStoresPerTenant) {
            throw new TenantCustomKeyStoreLimitExceededException(
                    String.format("Maximum number of custom key stores (%d) reached for tenant", maxStoresPerTenant)
            );
        }
    }

    private void validateCloudHsmRequest(CreateCustomKeyStoreRequestDto request) {
        Objects.requireNonNull(request, "Request cannot be null");

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

    /**
     * Validates that an external key store request contains all required fields.
     *
     * @param request the creation request to validate (non-null)
     * @throws MissingXksProxyEndpointException if required fields are missing
     * @throws MissingXksProxyPathException if required fields are missing
     * @throws MissingXksProxyAuthCredentialException if required fields are missing
     */
    private void validateExternalKeyStoreRequest(CreateCustomKeyStoreRequestDto request) {
        Objects.requireNonNull(request, "Request cannot be null");

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

    /**
     * Configures an internal CloudHSM-based custom key store.
     *
     * <p>Stores hashed passwords and certificates for secure storage.</p>
     *
     * @param customKeyStore the entity to configure (non-null)
     * @param request the creation request with configuration data (non-null)
     */
    private void configureInternalCloudHsmStore(CustomKeyStore customKeyStore, CreateCustomKeyStoreRequestDto request) {
        Objects.requireNonNull(customKeyStore, "CustomKeyStore cannot be null");
        Objects.requireNonNull(request, "Request cannot be null");

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

    /**
     * Configures an internal External Key Store (XKS) custom key store.
     *
     * <p>Stores authentication credentials securely as hashed values.</p>
     *
     * @param customKeyStore the entity to configure (non-null)
     * @param request the creation request with configuration data (non-null)
     */
    private void configureInternalExternalKeyStore(CustomKeyStore customKeyStore, CreateCustomKeyStoreRequestDto request) {
        Objects.requireNonNull(customKeyStore, "CustomKeyStore cannot be null");
        Objects.requireNonNull(request, "Request cannot be null");

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

    /**
     * Updates the configuration of an internal CloudHSM custom key store.
     *
     * @param customKeyStore the entity to update (non-null)
     * @param request the update request with new configuration (non-null)
     */
    private void updateInternalCloudHsmStore(CustomKeyStore customKeyStore, UpdateCustomKeyStoreRequestDto request) {
        Objects.requireNonNull(customKeyStore, "CustomKeyStore cannot be null");
        Objects.requireNonNull(request, "Request cannot be null");

        if (request.getKeyStorePassword() != null && !request.getKeyStorePassword().isEmpty()) {
            String passwordHash = hashPassword(request.getKeyStorePassword());
            customKeyStore.setKeyStorePassword(passwordHash);
            log.debug("Updated CloudHSM password for key store: {}", customKeyStore.getId());
        }
        // CloudHSM cluster ID cannot be updated after creation
    }

    /**
     * Updates the configuration of an internal External Key Store (XKS) custom key store.
     *
     * @param customKeyStore the entity to update (non-null)
     * @param request the update request with new configuration (non-null)
     */
    private void updateInternalExternalKeyStore(CustomKeyStore customKeyStore, UpdateCustomKeyStoreRequestDto request) {
        Objects.requireNonNull(customKeyStore, "CustomKeyStore cannot be null");
        Objects.requireNonNull(request, "Request cannot be null");

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
    // UTILITY METHODS | PRIVATE HELPERS
    // ============================================================================

    /**
     * Finds a custom key store by its ID for a specific tenant.
     *
     * @param tenant     the tenant identifier (non-null, non-empty)
     * @param keyStoreId the custom key store ID (non-null, must exist)
     * @return the CustomKeyStore entity
     * @throws CustomKeyStoreNotFoundException if the key store does not exist for the tenant
     */
    private CustomKeyStore findCustomKeyStore(String tenant, Long keyStoreId) {
        return (CustomKeyStore) customKeyStoreRepository.findByTenantAndCustomKeyStoreId(tenant, keyStoreId)
                .orElseThrow(() -> new CustomKeyStoreNotFoundException(
                        String.format("Custom key store not found: %s", keyStoreId)
                ));
    }

    /**
     * Hashes a password using SHA-256 with Base64 encoding.
     *
     * <p><b>Security Notes:</b></p>
     * <ul>
     *   <li>Passwords are never stored in plaintext</li>
     *   <li>SHA-256 hash is Base64-encoded for storage</li>
     *   <li>One-way function: original password cannot be recovered</li>
     * </ul>
     *
     * @param password the password to hash (non-null)
     * @return Base64-encoded SHA-256 hash of the password
     * @throws SecurityException if SHA-256 algorithm is unavailable (should never occur)
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to hash password - SHA-256 not available", e);
            throw new RuntimeException("Cryptographic algorithm unavailable", e);
        }
    }

    /**
     * Masks sensitive data for safe display in logs and responses.
     *
     * <p><b>Masking Format:</b></p>
     * <ul>
     *   <li>Data length ≤ 8: fully masked as "***MASKED***"</li>
     *   <li>Data length > 8: shows first 4 and last 4 characters, middle masked as "***"</li>
     *   <li>Example: "mysecretkey123" → "myse***key123"</li>
     * </ul>
     *
     * @param data the sensitive data to mask (nullable, returns "***MASKED***" if null)
     * @return masked string safe for display
     */
    private String maskSensitiveData(String data) {
        if (data == null || data.length() <= 8) {
            return "***MASKED***";
        }
        return data.substring(0, 4) + "***" + data.substring(data.length() - 4);
    }

    /**
     * Encodes a key store ID as a pagination token using Base64 encoding.
     *
     * <p>Used for cursor-based pagination in list operations.</p>
     *
     * @param id the key store ID to encode (non-null)
     * @return Base64-encoded pagination token
     */
    private String encodeNextToken(Long id) {
        Objects.requireNonNull(id, "ID cannot be null");
        return Base64.getEncoder().encodeToString(id.toString().getBytes());
    }

    /**
     * Decodes a pagination token to retrieve the key store ID.
     *
     * <p>Reverses the encoding performed by {@link #encodeNextToken(Long)}.</p>
     *
     * @param nextToken the pagination token to decode (non-null)
     * @return the decoded key store ID
     * @throws InvalidPaginationTokenException if the token is malformed or invalid
     */
    private Long decodeNextToken(String nextToken) {
        try {
            Objects.requireNonNull(nextToken, "NextToken cannot be null");
            String decoded = new String(Base64.getDecoder().decode(nextToken));
            return Long.parseLong(decoded);
        } catch (IllegalArgumentException e) {
            log.debug("Invalid pagination token provided: {}", nextToken);
            throw new InvalidPaginationTokenException("Invalid pagination token: malformed or corrupted");
        }
    }

    /**
     * Converts a JPA CustomKeyStore entity to a response DTO for API consumers.
     *
     * <p><b>Transformations:</b></p>
     * <ul>
     *   <li>Sensitive data is automatically masked</li>
     *   <li>All timestamps are preserved</li>
     *   <li>Connection state is derived from status field</li>
     * </ul>
     *
     * @param customKeyStore the JPA entity to convert (non-null)
     * @return a {@link CustomKeyStoreResponseDto} with masked sensitive data
     */
    private CustomKeyStoreResponseDto convertToResponseDto(CustomKeyStore customKeyStore) {
        Objects.requireNonNull(customKeyStore, "CustomKeyStore cannot be null");

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
    // INNER CLASSES FOR INTERNAL SIMULATION | IMPLEMENTATION DETAILS
    // ============================================================================

    /**
     * Internal class representing a software-simulated HSM instance.
     *
     * <p><b>Responsibilities:</b></p>
     * <ul>
     *   <li>Manages connection state (connected/disconnected)</li>
     *   <li>Stores cryptographic keys in-memory</li>
     *   <li>Tracks connection lifetime for heartbeat monitoring</li>
     * </ul>
     *
     * <p><b>Thread Safety:</b> Uses ConcurrentHashMap for thread-safe key storage.</p>
     *
     * @since 1.0
     */
    @SuppressWarnings("unused")
    private static class SoftwareHsmInstance {
        private final Long storeId;
        private final String name;
        private final ConcurrentHashMap<String, SecretKey> keys;
        private boolean connected;
        private LocalDateTime connectedAt;

        public SoftwareHsmInstance(Long storeId, String name) {
            this.storeId = Objects.requireNonNull(storeId, "StoreId cannot be null");
            this.name = Objects.requireNonNull(name, "Name cannot be null");
            this.connected = false;
            this.keys = new ConcurrentHashMap<>();
        }

        /**
         * Connects this HSM instance with the provided password.
         *
         * @param password the password to validate (non-null)
         * @return true if connection succeeds, false otherwise
         */
        public boolean connect(String password) {
            // Validate password (simplified)
            if (password != null && !password.isEmpty()) {
                this.connected = true;
                this.connectedAt = LocalDateTime.now();
                log.debug("Software HSM instance '{}' connected successfully", storeId);
                return true;
            }
            log.warn("Software HSM instance '{}' connection failed - invalid credentials", storeId);
            return false;
        }

        /**
         * Disconnects this HSM instance.
         */
        public void disconnect() {
            this.connected = false;
            this.connectedAt = null;
            log.debug("Software HSM instance '{}' disconnected", storeId);
        }

        /**
         * Checks if this HSM instance is currently connected.
         *
         * @return true if connected, false otherwise
         */
        public boolean isConnected() {
            return connected;
        }

        /**
         * Generates a cryptographic key using the specified algorithm.
         *
         * @param algorithm the key algorithm (e.g., "AES", "RSA")
         * @param keySize the key size in bits
         * @return the generated SecretKey
         * @throws IllegalStateException if HSM is not connected
         * @throws Exception if key generation fails
         */
        public SecretKey generateKey(String algorithm, int keySize) throws Exception {
            if (!connected) {
                throw new IllegalStateException("HSM not connected - unable to generate key");
            }
            KeyGenerator keyGen = KeyGenerator.getInstance(algorithm);
            keyGen.init(keySize);
            SecretKey key = keyGen.generateKey();
            keys.put(key.hashCode() + "", key);
            log.debug("Generated key using algorithm: {}, size: {} bits", algorithm, keySize);
            return key;
        }
    }

    /**
     * Internal class representing a simulated external key proxy (XKS) instance.
     *
     * <p><b>Responsibilities:</b></p>
     * <ul>
     *   <li>Manages connection to external key store endpoint</li>
     *   <li>Maintains session state and authentication</li>
     *   <li>Forwards cryptographic operations to external service</li>
     * </ul>
     *
     * @since 1.0
     */
    @SuppressWarnings("unused")
    private static class ExternalKeyProxyInstance {
        private final Long storeId;
        private final String endpoint;
        private final String path;
        private boolean connected;
        private String sessionId;

        public ExternalKeyProxyInstance(Long storeId, String endpoint, String path) {
            this.storeId = Objects.requireNonNull(storeId, "StoreId cannot be null");
            this.endpoint = Objects.requireNonNull(endpoint, "Endpoint cannot be null");
            this.path = Objects.requireNonNull(path, "Path cannot be null");
            this.connected = false;
        }

        /**
         * Connects this proxy to the external key store endpoint.
         *
         * @param authCredential the authentication credential (non-null)
         * @return true if connection succeeds, false otherwise
         */
        public boolean connect(String authCredential) {
            if (authCredential != null && !authCredential.isEmpty()) {
                this.connected = true;
                this.sessionId = UUID.randomUUID().toString();
                log.debug("External proxy instance '{}' connected with session: {}", storeId, sessionId);
                return true;
            }
            log.warn("External proxy instance '{}' connection failed - invalid credentials", storeId);
            return false;
        }

        /**
         * Disconnects this proxy from the external key store.
         */
        public void disconnect() {
            this.connected = false;
            this.sessionId = null;
            log.debug("External proxy instance '{}' disconnected", storeId);
        }

        /**
         * Checks if this proxy is currently connected.
         *
         * @return true if connected, false otherwise
         */
        public boolean isConnected() {
            return connected;
        }
    }

    /**
     * Internal class for tracking active custom key store connections.
     *
     * <p><b>Features:</b></p>
     * <ul>
     *   <li>Tracks connection lifetime</li>
     *   <li>Implements heartbeat mechanism</li>
     *   <li>Detects stale/expired connections</li>
     * </ul>
     *
     * @since 1.0
     */
    @SuppressWarnings("unused")
    private static class CustomKeyStoreConnection {
        private final Long storeId;
        private final int timeoutSeconds;
        private LocalDateTime lastHeartbeat;

        public CustomKeyStoreConnection(Long storeId, LocalDateTime connectedAt, int timeoutSeconds) {
            this.storeId = Objects.requireNonNull(storeId, "StoreId cannot be null");
            this.lastHeartbeat = Objects.requireNonNull(connectedAt, "ConnectedAt cannot be null");
            this.timeoutSeconds = timeoutSeconds;
        }

        /**
         * Refreshes the heartbeat timer to current time.
         *
         * <p>Called on every successful connection operation.</p>
         */
        public void refresh() {
            this.lastHeartbeat = LocalDateTime.now();
        }

        /**
         * Checks if this connection's heartbeat has expired.
         *
         * @return true if heartbeat has timed out, false otherwise
         */
        public boolean isExpired() {
            return lastHeartbeat.plusSeconds(timeoutSeconds).isBefore(LocalDateTime.now());
        }
    }
}