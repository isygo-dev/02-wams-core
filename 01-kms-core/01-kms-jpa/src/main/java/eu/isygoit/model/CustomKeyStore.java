package eu.isygoit.model;

import eu.isygoit.enums.IEnumCustomKeyStoreStatus;
import eu.isygoit.enums.IEnumCustomKeyStoreType;
import eu.isygoit.model.jakarta.AuditableEntity;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Custom Key Store Entity
 *
 * <p>Represents a custom key store that can host KMS keys.
 * Supports two types of custom key stores:</p>
 * <ul>
 *   <li><b>CLOUDHSM:</b> Software-based HSM simulation for key storage</li>
 *   <li><b>EXTERNAL_KEY_STORE:</b> External KMS proxy simulation</li>
 * </ul>
 *
 * <p>This entity manages the lifecycle and configuration of custom key stores,
 * including connection state, health monitoring, and store-specific properties.</p>
 *
 * @author Isygoit Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "CUSTOM_KEY_STORE",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_CUSTOM_KEY_STORE_NAME_TENANT",
                        columnNames = {"STORE_NAME", "TENANT"}),
                @UniqueConstraint(name = "UK_CUSTOM_KEY_STORE_ID",
                        columnNames = {"STORE_ID"})
        },
        indexes = {
                @Index(name = "IDX_CUSTOM_KEY_STORE_TENANT", columnList = "TENANT"),
                @Index(name = "IDX_CUSTOM_KEY_STORE_STATUS", columnList = "STATUS"),
                @Index(name = "IDX_CUSTOM_KEY_STORE_TYPE", columnList = "STORE_TYPE")
        })
public class CustomKeyStore extends AuditableEntity<Long> implements ITenantAssignable {

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
    @Column(name = "TENANT", nullable = false, length = 100)
    private String tenant;

    /**
     * Display name of the custom key store
     */
    @Column(name = "STORE_NAME", nullable = false, length = 255)
    private String name;

    /**
     * Type of custom key store (CLOUDHSM or EXTERNAL_KEY_STORE)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "STORE_TYPE", nullable = false, length = 50)
    private IEnumCustomKeyStoreType.Types type;

    /**
     * Current connection status of the custom key store
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 50)
    private IEnumCustomKeyStoreStatus.Types status;

    /**
     * Timestamp when the custom key store was created
     */
    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the custom key store was last updated
     */
    @UpdateTimestamp
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Timestamp of the last successful connection to the underlying store
     */
    @Column(name = "LAST_SUCCESSFUL_CONNECTION")
    private LocalDateTime lastSuccessfulConnection;

    /**
     * Timestamp of the last connection attempt
     */
    @Column(name = "LAST_CONNECTION_ATTEMPT")
    private LocalDateTime lastConnectionAttempt;

    /**
     * Timestamp of the last health check
     */
    @Column(name = "LAST_HEALTH_CHECK")
    private LocalDateTime lastHealthCheck;

    /**
     * Error message from the last failed connection attempt
     */
    @Column(name = "CONNECTION_ERROR", length = 1000)
    private String connectionError;

    /**
     * Active connection ID (when connected)
     */
    @Column(name = "CONNECTION_ID", length = 255)
    private String connectionId;

    // ============================================================================
    // CLOUDHSM TYPE SPECIFIC FIELDS
    // ============================================================================

    /**
     * CloudHSM cluster identifier (for CLOUDHSM type)
     */
    @Column(name = "CLOUDHSM_CLUSTER_ID", length = 255)
    private String cloudHsmClusterId;

    /**
     * Encrypted password for accessing the CloudHSM cluster
     */
    @Column(name = "KEY_STORE_PASSWORD", length = 512)
    private String keyStorePassword;

    /**
     * Trust anchor certificate for validating the CloudHSM cluster
     */
    @Column(name = "TRUST_ANCHOR_CERTIFICATE", columnDefinition = "TEXT")
    private String trustAnchorCertificate;

    // ============================================================================
    // EXTERNAL KEY STORE (XKS) TYPE SPECIFIC FIELDS
    // ============================================================================

    /**
     * XKS proxy URI endpoint (e.g., https://xks.example.com:8080)
     */
    @Column(name = "XKS_PROXY_URI_ENDPOINT", length = 500)
    private String xksProxyUriEndpoint;

    /**
     * XKS proxy URI path (e.g., /api/v1/kms)
     */
    @Column(name = "XKS_PROXY_URI_PATH", length = 255)
    private String xksProxyUriPath;

    /**
     * Authentication credential for the XKS proxy (encrypted)
     */
    @Column(name = "XKS_PROXY_AUTH_CREDENTIAL", length = 512)
    private String xksProxyAuthenticationCredential;

    // ============================================================================
    // ADDITIONAL CONFIGURATION
    // ============================================================================

    /**
     * Store-specific configuration data in JSON format
     */
    @Column(name = "STORE_SPECIFIC_DATA", columnDefinition = "TEXT")
    private String customKeyStoreTypeSpecificData;

    /**
     * Number of KMS keys hosted in this custom key store
     */
    @Column(name = "KEY_COUNT")
    private Integer keyCount;

    /**
     * Maximum number of keys allowed in this custom key store
     */
    @Column(name = "MAX_KEYS")
    private Integer maxKeys;

    /**
     * Store health status (HEALTHY, DEGRADED, UNHEALTHY)
     */
    @Column(name = "HEALTH_STATUS", length = 50)
    private String healthStatus;

    /**
     * Additional metadata as JSON
     */
    @Column(name = "METADATA", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Tags associated with this custom key store (JSON format)
     */
    @Column(name = "TAGS", columnDefinition = "TEXT")
    private String tags;

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
     * Check if the custom key store has reached its maximum key limit
     */
    public boolean isAtMaxKeyLimit() {
        return maxKeys != null && keyCount != null && keyCount >= maxKeys;
    }

    /**
     * Check if this is a CloudHSM type store
     */
    public boolean isCloudHsmType() {
        return type == IEnumCustomKeyStoreType.Types.CLOUDHSM;
    }

    /**
     * Check if this is an External Key Store type
     */
    public boolean isExternalKeyStoreType() {
        return type == IEnumCustomKeyStoreType.Types.EXTERNAL_KEY_STORE;
    }

    /**
     * Increment the key count
     */
    public void incrementKeyCount() {
        if (keyCount == null) {
            keyCount = 0;
        }
        keyCount++;
    }

    /**
     * Decrement the key count
     */
    public void decrementKeyCount() {
        if (keyCount != null && keyCount > 0) {
            keyCount--;
        }
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