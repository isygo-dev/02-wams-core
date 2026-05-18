package eu.isygoit.model;

import eu.isygoit.constants.TenantConstants;
import eu.isygoit.enums.IEnumCustomKeyStoreStatus;
import eu.isygoit.enums.IEnumCustomKeyStoreType;
import eu.isygoit.model.jakarta.AuditableEntity;
import eu.isygoit.model.schema.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom Key Store Entity
 *
 * <p>Represents a custom key store that can host KMS keys.
 * Supports two types of custom key stores:</p>
 * <ul>
 *   <li><b>WAMS_CLOUDHSM:</b> Software-based HSM simulation for key storage</li>
 *   <li><b>EXTERNAL_KEY_STORE:</b> External KMS proxy simulation</li>
 * </ul>
 *
 * <p>This entity manages the lifecycle and configuration of custom key stores,
 * including connection state, health monitoring, store-specific properties,
 * and the list of keys hosted in the store.</p>
 *
 * @author Isygoit Team
 * @version 2.0
 * @since 1.0
 */
@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = SchemaTableConstantName.T_KMS_CUSTOM_KEY_STORE,
        uniqueConstraints = {
                @UniqueConstraint(name = SchemaUcConstantName.UC_UK_CUSTOM_KEY_STORE_NAME_TENANT,
                        columnNames = {SchemaColumnConstantName.C_STORE_NAME, SchemaColumnConstantName.C_TENANT})
        },
        indexes = {
                @Index(name = SchemaIndexConstantName.IDX_CUSTOM_KEY_STORE_TENANT, columnList = SchemaColumnConstantName.C_TENANT),
                @Index(name = SchemaIndexConstantName.IDX_CUSTOM_KEY_STORE_STATUS, columnList = SchemaColumnConstantName.C_STATUS),
                @Index(name = SchemaIndexConstantName.IDX_CUSTOM_KEY_STORE_TYPE, columnList = SchemaColumnConstantName.C_STORE_TYPE)
        })
public class KmsCustomKeyStore extends AuditableEntity<Long> implements ITenantAssignable {

    /**
     * Unique identifier for the custom key store
     */
    @Id
    @SequenceGenerator(name = "custkeystore_sequence_generator", sequenceName = "custkeystore_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "custkeystore_sequence_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    /**
     * Tenant that owns this custom key store
     */
    @ColumnDefault("'" + TenantConstants.DEFAULT_TENANT_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_TENANT, length = SchemaConstantSize.TENANT, updatable = false, nullable = false)
    private String tenant;

    /**
     * Display name of the custom key store
     */
    @Column(name = SchemaColumnConstantName.C_STORE_NAME, nullable = false, length = 255)
    private String name;

    /**
     * Type of custom key store (WAMS_CLOUDHSM or EXTERNAL_KEY_STORE)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_STORE_TYPE, nullable = false, length = 50)
    private IEnumCustomKeyStoreType.Types type;

    /**
     * Current connection status of the custom key store
     */
    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_STATUS, nullable = false, length = 50)
    private IEnumCustomKeyStoreStatus.Types status;

    /**
     * Timestamp of the last successful connection to the underlying store
     */
    @Column(name = SchemaColumnConstantName.C_LAST_SUCCESSFUL_CONNECTION)
    private LocalDateTime lastSuccessfulConnection;

    /**
     * Timestamp of the last connection attempt
     */
    @Column(name = SchemaColumnConstantName.C_LAST_CONNECTION_ATTEMPT)
    private LocalDateTime lastConnectionAttempt;

    /**
     * Timestamp of the last health check
     */
    @Column(name = SchemaColumnConstantName.C_LAST_HEALTH_CHECK)
    private LocalDateTime lastHealthCheck;

    /**
     * Error message from the last failed connection attempt
     */
    @Column(name = SchemaColumnConstantName.C_CONNECTION_ERROR, length = 1000)
    private String connectionError;

    /**
     * Active connection ID (when connected)
     */
    @Column(name = SchemaColumnConstantName.C_CONNECTION_ID, length = 255)
    private String connectionId;

    // ============================================================================
    // CLOUDHSM TYPE SPECIFIC FIELDS
    // ============================================================================

    /**
     * CloudHSM cluster identifier (for CLOUDHSM type)
     */
    @Column(name = SchemaColumnConstantName.C_CLOUDHSM_CLUSTER_ID, length = 255)
    private String cloudHsmClusterId;

    /**
     * Encrypted password for accessing the CloudHSM cluster
     */
    @Column(name = SchemaColumnConstantName.C_KEY_STORE_PASSWORD, length = 512)
    private String keyStorePassword;

    /**
     * Trust anchor certificate for validating the CloudHSM cluster
     */
    @Column(name = SchemaColumnConstantName.C_TRUST_ANCHOR_CERTIFICATE, columnDefinition = "TEXT")
    private String trustAnchorCertificate;

    // ============================================================================
    // EXTERNAL KEY STORE (XKS) TYPE SPECIFIC FIELDS
    // ============================================================================

    /**
     * XKS proxy URI endpoint (e.g., https://xks.example.com:8080)
     */
    @Column(name = SchemaColumnConstantName.C_XKS_PROXY_URI_ENDPOINT, length = 500)
    private String xksProxyUriEndpoint;

    /**
     * XKS proxy URI path (e.g., /api/v1/kms)
     */
    @Column(name = SchemaColumnConstantName.C_XKS_PROXY_URI_PATH, length = 255)
    private String xksProxyUriPath;

    /**
     * Authentication credential for the XKS proxy (encrypted)
     */
    @Column(name = SchemaColumnConstantName.C_XKS_PROXY_AUTH_CREDENTIAL, length = 512)
    private String xksProxyAuthenticationCredential;

    @Column(name = SchemaColumnConstantName.C_XKS_PROXY_CONNECTIVITY, length = 255)
    private String xksProxyConnectivity;

    // ============================================================================
    // ADDITIONAL CONFIGURATION
    // ============================================================================

    /**
     * Store-specific configuration data in JSON format
     */
    @Column(name = SchemaColumnConstantName.C_STORE_SPECIFIC_DATA, columnDefinition = "TEXT")
    private String customKeyStoreTypeSpecificData;

    /**
     * Maximum number of keys allowed in this custom key store
     */
    @Column(name = SchemaColumnConstantName.C_MAX_KEYS)
    private Integer maxKeys;

    /**
     * Store health status (HEALTHY, DEGRADED, UNHEALTHY)
     */
    @Column(name = SchemaColumnConstantName.C_HEALTH_STATUS, length = 50)
    private String healthStatus;

    /**
     * Additional metadata as JSON
     */
    @Column(name = SchemaColumnConstantName.C_METADATA, columnDefinition = "TEXT")
    private String metadata;

    /**
     * Tags associated with this custom key store (JSON format)
     */
    @Column(name = SchemaColumnConstantName.C_TAGS, columnDefinition = "TEXT")
    private String tags;

    // ============================================================================
    // CONNECTION SETTINGS (per‑store overrides)
    // ============================================================================

    /**
     * Connection timeout in seconds (overrides global default if set)
     */
    @Column(name = "CONNECTION_TIMEOUT_SECONDS")
    private Integer connectionTimeoutSeconds;

    /**
     * Health check interval in seconds (overrides global default if set)
     */
    @Column(name = "HEALTH_CHECK_INTERVAL_SECONDS")
    private Integer healthCheckIntervalSeconds;

    /**
     * Whether to auto‑reconnect on failure (overrides global default if set)
     */
    @Column(name = "AUTO_RECONNECT")
    private Boolean autoReconnect;

    // ============================================================================
    // RELATIONSHIP TO KMS KEYS
    // ============================================================================

    /**
     * The list of KMS keys hosted in this custom key store.
     * This is a bidirectional OneToMany mapping; the owning side is KmsKey.customKeyStore.
     */
    @OneToMany(mappedBy = "customKeyStore", fetch = FetchType.LAZY, cascade = {})
    private List<KmsKey> keys = new ArrayList<>();

    // ============================================================================
    // BUSINESS METHODS
    // ============================================================================

    /**
     * Check if the custom key store is currently connected
     */
    public boolean isConnected() {
        return status == IEnumCustomKeyStoreStatus.Types.CONNECTED;
    }

    /**
     * Check if the custom key store is disconnected
     */
    public boolean isDisconnected() {
        return status == IEnumCustomKeyStoreStatus.Types.DISCONNECTED;
    }

    /**
     * Check if the custom key store is in failed state
     */
    public boolean isFailed() {
        return status == IEnumCustomKeyStoreStatus.Types.FAILED;
    }

    /**
     * Check if the custom key store is connecting
     */
    public boolean isConnecting() {
        return status == IEnumCustomKeyStoreStatus.Types.CONNECTING;
    }

    /**
     * Check if the custom key store is disconnecting
     */
    public boolean isDisconnecting() {
        return status == IEnumCustomKeyStoreStatus.Types.DISCONNECTING;
    }

    /**
     * Check if this is a CloudHSM type store
     */
    public boolean isCloudHsmType() {
        return type == IEnumCustomKeyStoreType.Types.WAMS_CLOUDHSM;
    }

    /**
     * Check if this is an External Key Store type
     */
    public boolean isExternalKeyStoreType() {
        return type == IEnumCustomKeyStoreType.Types.EXTERNAL_KEY_STORE;
    }

    /**
     * Add a KMS key to this store, updating both sides of the relationship
     * and the cached key count.
     *
     * @param key the KmsKey entity to add
     */
    public void addKey(KmsKey key) {
        keys.add(key);
    }

    /**
     * Remove a KMS key from this store, updating both sides of the relationship
     * and the cached key count.
     *
     * @param key the KmsKey entity to remove
     */
    public void removeKey(KmsKey key) {
        keys.remove(key);
    }

    /**
     * Update health status based on connection state
     */
    public void updateHealthStatus() {
        if (status == IEnumCustomKeyStoreStatus.Types.CONNECTED) {
            this.healthStatus = "HEALTHY";
        } else if (status == IEnumCustomKeyStoreStatus.Types.FAILED) {
            this.healthStatus = "UNHEALTHY";
        } else if (status == IEnumCustomKeyStoreStatus.Types.DISCONNECTED) {
            this.healthStatus = "DEGRADED";
        } else {
            this.healthStatus = "UNKNOWN";
        }
    }
}