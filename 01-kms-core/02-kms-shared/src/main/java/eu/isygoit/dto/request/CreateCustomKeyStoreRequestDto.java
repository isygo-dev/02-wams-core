package eu.isygoit.dto.request;

import eu.isygoit.annotation.ValidCreateCustomKeyStoreRequest;
import eu.isygoit.enums.IEnumCustomKeyStoreType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a custom key store with support for multiple key store types.
 * <p>
 * This DTO supports two key store types:
 * <p>
 * 1. CLOUDHSM:
 * - keyStoreName (required)
 * - type = CLOUDHSM (required)
 * - cloudHsmClusterId (required) - The ID of the CloudHSM cluster
 * - keyStorePassword (required) - The password for cluster authentication
 * - trustAnchorCertificate (required) - The trust anchor certificate in PEM format
 * <p>
 * 2. EXTERNAL_KEY_STORE (AWS KMS XKS):
 * - keyStoreName (required)
 * - type = EXTERNAL_KEY_STORE (required)
 * - xksProxyUriEndpoint (required) - The XKS proxy service endpoint URL
 * - xksProxyUriPath (optional) - The path component of the XKS proxy URI
 * - xksProxyAuthenticationCredential (required) - Bearer token or API key for authentication
 *
 * @author Isygoit Team
 * @version 2.0 - Fixed to match CustomKeyStoreService expectations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidCreateCustomKeyStoreRequest // Type-specific validation
public class CreateCustomKeyStoreRequestDto {

    /**
     * Name of the custom key store. Must be unique per tenant.
     * <p>
     * Constraints:
     * - Not blank
     * - Max length typically 255 characters
     * <p>
     * Applicable to: ALL types
     */
    @NotBlank(message = "Key store name cannot be blank")
    private String keyStoreName;

    /**
     * Type of custom key store.
     * <p>
     * Allowed values:
     * - CLOUDHSM: Software-based HSM simulation
     * - EXTERNAL_KEY_STORE: External KMS proxy (AWS XKS compatible)
     * <p>
     * This field determines which other fields are required.
     * Applicable to: ALL types
     */
    @NotNull(message = "Key store type cannot be null")
    private IEnumCustomKeyStoreType.Types type;

    // ============================================================================
    // CLOUDHSM TYPE SPECIFIC FIELDS
    // Applicable when: type == IEnumCustomKeyStoreType.Types.CLOUDHSM
    // ============================================================================

    /**
     * The CloudHSM cluster ID for CLOUDHSM type key stores.
     * <p>
     * REQUIRED when type = CLOUDHSM
     * IGNORED when type = EXTERNAL_KEY_STORE
     * <p>
     * Example value: "cluster-abcdef123456"
     * <p>
     * Constraints (via @ValidCreateCustomKeyStoreRequest):
     * - Not null and not blank (when type is CLOUDHSM)
     */
    private String cloudHsmClusterId;

    /**
     * The key store password for CloudHSM cluster authentication.
     * <p>
     * REQUIRED when type = CLOUDHSM
     * IGNORED when type = EXTERNAL_KEY_STORE
     * <p>
     * This password is hashed before storage using SHA-256.
     * Used in: validateCloudHsmRequest() (line 414), configureInternalCloudHsmStore() (line 441)
     * <p>
     * Constraints (via @ValidCreateCustomKeyStoreRequest):
     * - Not null and not blank (when type is CLOUDHSM)
     * - Minimum length: 8 characters (recommended)
     * - Special characters recommended for security
     */
    private String keyStorePassword;

    /**
     * The trust anchor certificate for CloudHSM.
     * <p>
     * REQUIRED when type = CLOUDHSM
     * IGNORED when type = EXTERNAL_KEY_STORE
     * <p>
     * This is the public certificate used to establish trust with the CloudHSM cluster.
     * Format: PEM-encoded X.509 certificate
     * Used in: validateCloudHsmRequest() (line 417), configureInternalCloudHsmStore() (line 445)
     * <p>
     * Example:
     * -----BEGIN CERTIFICATE-----
     * MIIDXTCCAkWgAwIBAgIJAJC1/iNAZwqDMA0GCSqGSIb3DQEBBQUAMEUxCzAJBgNV
     * BAYTAkFVMRMwEQYDVQQIDApTb21lLVN0YXRlMSEwHwYDVQQKDBhJbnRlcm5ldCBX
     * ...
     * -----END CERTIFICATE-----
     * <p>
     * Constraints (via @ValidCreateCustomKeyStoreRequest):
     * - Not null and not blank (when type is CLOUDHSM)
     * - Must be a valid PEM-encoded certificate
     */
    private String trustAnchorCertificate;

    // ============================================================================
    // EXTERNAL KEY STORE (XKS) TYPE SPECIFIC FIELDS
    // Applicable when: type == IEnumCustomKeyStoreType.Types.EXTERNAL_KEY_STORE
    // ============================================================================

    /**
     * The URI endpoint for the XKS proxy service.
     * <p>
     * REQUIRED when type = EXTERNAL_KEY_STORE
     * IGNORED when type = CLOUDHSM
     * <p>
     * Example values:
     * - https://xks-proxy.example.com
     * - https://192.168.1.100:8080
     * <p>
     * Constraints (via @ValidCreateCustomKeyStoreRequest):
     * - Not null and not blank (when type is EXTERNAL_KEY_STORE)
     * - Must be a valid URI (starts with http:// or https://)
     * - HTTPS recommended for production
     */
    private String xksProxyUriEndpoint;

    /**
     * The URI path for the XKS proxy service operations.
     * <p>
     * OPTIONAL when type = EXTERNAL_KEY_STORE
     * IGNORED when type = CLOUDHSM
     * <p>
     * This is the path component appended to the endpoint URI for operations.
     * <p>
     * Example values:
     * - /v1/kms
     * - /xks/api/v1
     * - (empty string for root-level operations)
     * <p>
     * Constraints:
     * - May be null or blank (truly optional)
     * - If provided, should be a valid URL path
     */
    private String xksProxyUriPath;

    /**
     * The authentication credential for XKS proxy.
     * <p>
     * REQUIRED when type = EXTERNAL_KEY_STORE
     * IGNORED when type = CLOUDHSM
     * <p>
     * This credential is used to authenticate requests to the XKS proxy service.
     * It is hashed before storage using SHA-256.
     * <p>
     * Typical format:
     * - Bearer token (JWT or opaque)
     * - API key
     * - Any token-based authentication credential
     * <p>
     * Example values:
     * - eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ
     * - sk-1234567890abcdefghij
     * <p>
     * Constraints (via @ValidCreateCustomKeyStoreRequest):
     * - Not null and not blank (when type is EXTERNAL_KEY_STORE)
     * - Recommended minimum length: 20 characters
     */
    private String xksProxyAuthenticationCredential;
}