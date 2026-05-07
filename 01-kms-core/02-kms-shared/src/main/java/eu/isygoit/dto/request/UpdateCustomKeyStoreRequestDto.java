package eu.isygoit.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Update Custom Key Store Request DTO
 *
 * <p>Used to update an existing custom key store configuration.
 * Supports both CloudHSM and External Key Store types.</p>
 *
 * @author Isygoit Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update an existing custom key store")
public class UpdateCustomKeyStoreRequestDto {

    @Schema(description = "New name for the custom key store",
            example = "Production-CloudHSM-Store-Updated",
            maxLength = 255)
    @Size(min = 1, max = 255, message = "Store name must be between 1 and 255 characters")
    private String newCustomKeyStoreName;

    // ============================================================================
    // CLOUDHSM TYPE FIELDS
    // ============================================================================

    @Schema(description = "CloudHSM cluster endpoint (for CLOUDHSM type)",
            example = "cloudhsm.cluster-12345678.us-east-1.amazonaws.com")
    private String cloudHsmClusterEndpoint;

    @Schema(description = "Password for the CloudHSM cluster (will be encrypted)",
            example = "MySecurePassword123!",
            format = "password")
    private String keyStorePassword;

    @Schema(description = "Trust anchor certificate for CloudHSM cluster validation",
            example = "-----BEGIN CERTIFICATE-----\nMIID...\n-----END CERTIFICATE-----")
    private String trustAnchorCertificate;

    // ============================================================================
    // EXTERNAL KEY STORE (XKS) TYPE FIELDS
    // ============================================================================

    @Schema(description = "XKS proxy URI endpoint",
            example = "https://xks-proxy.example.com:8080")
    @Pattern(regexp = "^https?://[^\\s]+$", message = "Invalid URI endpoint format")
    private String xksProxyUriEndpoint;

    @Schema(description = "XKS proxy URI path",
            example = "/api/v1/kms/operations")
    private String xksProxyUriPath;

    @Schema(description = "Authentication credential for XKS proxy",
            example = "arn:aws:secretsmanager:region:account:secret:xks-auth-key",
            format = "password")
    private String xksProxyAuthenticationCredential;

    // ============================================================================
    // COMMON FIELDS
    // ============================================================================

    @Schema(description = "Enable or disable the custom key store",
            example = "true")
    private Boolean enabled;

    @Schema(description = "Additional configuration as key-value pairs",
            example = "{\"connectionTimeout\": \"30\", \"maxRetries\": \"3\"}")
    private Map<String, String> configuration;

    @Schema(description = "Maximum number of keys allowed in this store",
            example = "1000")
    private Integer maxKeys;

    @Schema(description = "Custom metadata for the store",
            example = "{\"owner\": \"security-team\", \"cost-center\": \"12345\"}")
    private Map<String, String> metadata;

    @Schema(description = "Tags for cost allocation and organization",
            example = "{\"Environment\": \"Production\", \"Project\": \"PCI-Compliance\"}")
    private Map<String, String> tags;

    // ============================================================================
    // CONNECTION SETTINGS
    // ============================================================================

    @Schema(description = "Connection timeout in seconds",
            example = "30")
    private Integer connectionTimeoutSeconds;

    @Schema(description = "Health check interval in seconds",
            example = "60")
    private Integer healthCheckIntervalSeconds;

    @Schema(description = "Auto-reconnect on failure",
            example = "true")
    private Boolean autoReconnect;

    // ============================================================================
    // VALIDATION HELPER METHODS
    // ============================================================================

    /**
     * Check if any CloudHSM specific fields are being updated
     */
    public boolean hasCloudHsmUpdates() {
        return cloudHsmClusterEndpoint != null ||
                keyStorePassword != null ||
                trustAnchorCertificate != null;
    }

    /**
     * Check if any External Key Store specific fields are being updated
     */
    public boolean hasExternalKeyStoreUpdates() {
        return xksProxyUriEndpoint != null ||
                xksProxyUriPath != null ||
                xksProxyAuthenticationCredential != null;
    }

    /**
     * Check if name is being updated
     */
    public boolean isNameUpdating() {
        return newCustomKeyStoreName != null && !newCustomKeyStoreName.isEmpty();
    }

    /**
     * Check if store should be enabled/disabled
     */
    public boolean isStatusUpdating() {
        return enabled != null;
    }

    /**
     * Get effective connection timeout (with default fallback)
     */
    public int getEffectiveConnectionTimeout(int defaultValue) {
        return connectionTimeoutSeconds != null ? connectionTimeoutSeconds : defaultValue;
    }

    /**
     * Get effective health check interval (with default fallback)
     */
    public int getEffectiveHealthCheckInterval(int defaultValue) {
        return healthCheckIntervalSeconds != null ? healthCheckIntervalSeconds : defaultValue;
    }
}