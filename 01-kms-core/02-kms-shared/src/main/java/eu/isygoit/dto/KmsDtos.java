package eu.isygoit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.isygoit.annotation.ValidCreateCustomKeyStoreRequest;
import eu.isygoit.dto.data.TagDto;
import eu.isygoit.enums.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Container class for all WAMS KMS API DTOs (Requests and Responses).
 * <p>
 * Each static nested class corresponds to an WAMS KMS operation.
 * Field names and types follow the official WAMS KMS JSON 1.1 specification.
 * </p>
 */
public final class KmsDtos {

    // =========================================================================
    // Key Management
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateKeyRequest {
        private String description;
        private IEnumKeySpec.Types keySpec;          // SYMMETRIC_DEFAULT, RSA_2048, ECC_NIST_P256, etc.
        private IEnumKeyUsage.Types keyUsage;         // ENCRYPT_DECRYPT, SIGN_VERIFY, GENERATE_VERIFY_MAC
        private String alias;
        private IEnumKeyOrigin.Types origin;           // WAMS_KMS, EXTERNAL
        private List<Tag> tags;
        private Boolean multiRegion;
        @JsonProperty("BypassPolicyLockoutSafetyCheck")
        private Boolean bypassPolicyLockoutSafetyCheck;
        private String policy;            // IAM policy JSON

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Tag {
            private String tagKey;
            private String tagValue;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateKeyResponse {
        private KeyMetadata keyMetadata;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class KeyMetadata {
            private String awsAccountId;
            private String keyId;
            private String arn;
            private LocalDateTime creationDate;
            private Boolean enabled;
            private String description;
            private Boolean rotationEnabled;
            private IEnumKeySpec.Types keySpec;
            private IEnumKeyUsage.Types keyUsage;
            private String currentVersion;
            private String keyState;
            private IEnumKeyOrigin.Types origin;
            private IEnumKeyStatus.Types status;
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            private LocalDateTime createdAt;
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            private LocalDateTime updatedAt;
            private String alias;
            private IEnumKeyExpirationModel.Types expirationModel;
            private String customerMasterKeySpec; // alias for keySpec
            private List<String> encryptionAlgorithms;
            private List<String> signingAlgorithms;
            private String keyManager;   // WAMS, CUSTOMER
            private Boolean multiRegion;
            private Object multiRegionConfiguration;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DescribeKeyRequest {
        private String keyId;
        private List<String> grantTokens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DescribeKeyResponse {
        private CreateKeyResponse.KeyMetadata keyMetadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListKeysRequest {
        private Integer limit;
        private String marker;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListKeysResponse {
        private List<KeyEntry> keys;
        private String nextMarker;
        private Boolean truncated;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class KeyEntry {
            private String keyId;
            private String keyArn;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleKeyDeletionRequest {
        private String keyId;
        private Integer pendingWindowInDays;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleKeyDeletionResponse {
        private String keyId;
        private IEnumKeyStatus.Types keyStatus;
        private LocalDateTime deletionDate;
        private String keyState;
        private Integer pendingWindowInDays;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelKeyDeletionRequest {
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelKeyDeletionResponse {
        private String keyId;
        private IEnumKeyStatus.Types keyStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteKeyRequest {
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteKeyResponse {
        private String keyId;
        private IEnumKeyStatus.Types keyStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnableKeyRequest {
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnableKeyResponse {
        private String keyId;
        private IEnumKeyStatus.Types keyStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisableKeyRequest {
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisableKeyResponse {
        private String keyId;
        private IEnumKeyStatus.Types status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateKeyDescriptionRequest {
        private String keyId;
        private String alias;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateKeyDescriptionResponse {
        private CreateKeyResponse.KeyMetadata keyMetadata;
    }

    // =========================================================================
    // Key Rotation
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetKeyRotationStatusRequest {
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetKeyRotationStatusResponse {
        private String keyId;
        private Boolean rotationEnabled;
        private Integer rotationPeriodDays;
        private LocalDateTime lastRotationDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnableKeyRotationRequest {
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnableKeyRotationResponse {
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisableKeyRotationRequest {
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisableKeyRotationResponse {
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RotateKeyRequest {
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RotateKeyResponse {
        private String keyId;
        private String newVersionId;
        private LocalDateTime rotationDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListKeyVersionsRequest {
        private String keyId;
        private Integer limit;
        private String marker;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListKeyVersionsResponse {
        private List<KeyVersion> versions;
        private String nextMarker;
        private Boolean truncated;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class KeyVersion {
            private String keyId;
            private String versionId;
            private LocalDateTime creationDate;
            private IEnumKeyStatus.Types status;
            private String signingAlgorithm;
            private IEnumKeyExpirationModel.Types expirationModel;
            private IEnumKeyOrigin.Types origin;
        }
    }

    // =========================================================================
    // Cryptographic Operations
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EncryptRequest {
        private String keyId;
        private String plaintext;      // base64-encoded
        private Map<String, String> encryptionContext;
        private List<String> grantTokens;
        private String encryptionAlgorithm; // SYMMETRIC_DEFAULT, RSAES_OAEP_SHA_256, etc.
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EncryptResponse {
        private String ciphertextBlob; // base64-encoded
        private String keyId;
        private String keyVersionId;
        private String encryptionAlgorithm;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DecryptRequest {
        private String ciphertextBlob; // base64-encoded
        private Map<String, String> encryptionContext;
        private List<String> grantTokens;
        private String keyId; // optional, can be derived from ciphertext
        private String encryptionAlgorithm;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DecryptResponse {
        private String keyId;
        private String keyVersionId;
        private String plaintext;      // base64-encoded
        private String encryptionAlgorithm;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReEncryptRequest {
        // REQUIRED: source key for decrypt phase
        private String sourceKeyId;

        // REQUIRED: destination key for encrypt phase
        private String destinationKeyId;

        // REQUIRED: encrypted payload
        private String ciphertextBlob;

        // Optional encryption context for source decryption
        private Map<String, String> sourceEncryptionContext;

        // Optional encryption context for destination encryption
        private Map<String, String> destinationEncryptionContext;

        // Optional auth tokens (WAMS-style compatibility)
        private List<String> grantTokens;

        // Optional: algorithm used for source decryption
        private String sourceEncryptionAlgorithm;

        // Optional: algorithm used for destination encryption
        private String destinationEncryptionAlgorithm;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReEncryptResponse {
        private String ciphertextBlob;
        private String sourceKeyId;
        private String destinationKeyId;
        private String destinationKeyVersionId;
        private String destinationEncryptionAlgorithm;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateDataKeyRequest {
        private String keyId;
        private String keySpec;       // AES_128, AES_256
        private Integer keySize;
        private Map<String, String> encryptionContext;
        private List<String> grantTokens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateDataKeyResponse {
        private String ciphertextBlob;
        private String plaintext;      // base64-encoded
        private String keyId;
        private String encryptionAlgorithm;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateDataKeyWithoutPlaintextRequest {
        private String keyId;
        private String keySpec;
        private Integer keySize;
        private Map<String, String> encryptionContext;
        private List<String> grantTokens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateDataKeyWithoutPlaintextResponse {
        private String ciphertextBlob;
        private String keyId;
        private String encryptionAlgorithm;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateDataKeyPairRequest {
        private String keyId;
        private String keyPairSpec;    // RSA_2048, ECC_NIST_P256, etc.
        private Map<String, String> encryptionContext;
        private List<String> grantTokens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateDataKeyPairResponse {
        // Public key is safe to expose
        private String publicKey;

        // Encrypted private key (this is the ONLY form of private key returned)
        private String privateKeyCiphertextBlob;

        // Key metadata
        private String keyId;
        private String keyPairSpec;
        private String encryptionAlgorithm;

        // Optional: versioning (useful for rotation / audit / tracing)
        private String keyVersionId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateDataKeyPairWithoutPlaintextRequest {
        private String keyId;
        private String keyPairSpec;
        private Map<String, String> encryptionContext;
        private List<String> grantTokens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateDataKeyPairWithoutPlaintextResponse {
        // Public key is safe
        private String publicKey;

        // Encrypted private key only (no plaintext ever exposed)
        private String privateKeyCiphertextBlob;

        // Key identity
        private String keyId;

        // Optional but recommended for traceability
        private String keyVersionId;

        // Metadata
        private String keyPairSpec;
        private String encryptionAlgorithm;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignRequest {
        private String keyId;
        private String message;        // base64-encoded
        private String messageType;    // RAW or DIGEST
        private String signingAlgorithm;  // RSASSA_PSS_SHA_256, ECDSA_SHA_256, etc.
        private List<String> grantTokens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignResponse {
        private String signature;      // base64-encoded
        private String keyId;
        private String keyVersionId;
        private String signingAlgorithm;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyRequest {
        private String keyId;
        private String message;
        private String messageType;
        private String signature;
        private String signingAlgorithm;
        private List<String> grantTokens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyResponse {
        boolean valid;
        private Boolean signatureValid;
        private String keyId;
        private String signingAlgorithm;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateMacRequest {
        private String keyId;
        private String message;        // base64-encoded
        private String macAlgorithm;   // HMAC_SHA_256, etc.
        private List<String> grantTokens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateMacResponse {
        private String mac;            // base64-encoded
        private String keyId;
        private String macAlgorithm;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyMacRequest {
        private String keyId;
        private String message;
        private String mac;
        private String macAlgorithm;
        private List<String> grantTokens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyMacResponse {
        private Boolean macValid;
        private String keyId;
        private String macAlgorithm;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetPublicKeyRequest {
        private String keyId;
        private List<String> grantTokens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetPublicKeyResponse {
        private String keyId;
        private String publicKey;      // base64-encoded DER or PEM
        private IEnumKeySpec.Types customerMasterKeySpec;
        private String keyUsage;
        private List<String> encryptionAlgorithms;
        private List<String> signingAlgorithms;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateRandomRequest {
        private Integer numberOfBytes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateRandomResponse {
        private String plaintext;      // base64-encoded random bytes
    }

    // =========================================================================
    // Aliases
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateAliasRequestDto {
        @NotBlank
        private String aliasName;

        @NotNull
        private String targetKeyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ValidCreateCustomKeyStoreRequest // Type-specific validation
    public static class CreateCustomKeyStoreRequestDto {

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
         * - EXTERNAL_KEY_STORE: External KMS proxy (WAMS XKS compatible)
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

        private String xksProxyConnectivity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportKeyMaterialRequestDto {

        /**
         * The encrypted key material
         * This should be encrypted using the wrapping key obtained from GetParametersForImport
         */
        @NotNull(message = "Encrypted key material is required")
        private byte[] encryptedKeyMaterial;

        private LocalDateTime validTo;

        private IEnumKeyExpirationModel.Types expirationModel;
        /**
         * The import token obtained from GetParametersForImport
         */
        @NotNull(message = "Import token is required")
        private byte[] importToken;

        /**
         * The algorithm used to encrypt the key material
         * Must match the wrapping algorithm used when generating the parameters
         */
        private String wrappingAlgorithm;

        /**
         * The expiration date for the imported key material
         * After this date, the key material becomes invalid
         * Optional - if not set, the key material never expires
         */
        private LocalDateTime expirationDate;

        /**
         * Whether to expire the key material (if expirationDate is set)
         */
        private Boolean expireKeyMaterial;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetireGrantRequestDto {
        private String grantToken;
        private String retiringPrincipal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateAliasRequestDto {
        @NotBlank
        private String targetKeyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request to update an existing custom key store")
    public static class UpdateCustomKeyStoreRequestDto {

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

        private String cloudHsmClusterId;

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

        private String xksProxyConnectivity;
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(
            title = "Update Key Rotation Request",
            description = "Request payload for enabling or disabling automatic key rotation"
    )
    public static class UpdateKeyRotationRequestDto {

        private String keyId;
        /**
         * Whether to enable automatic key rotation.
         * True to enable annual automatic rotation, false to disable.
         */
        @JsonProperty("enableRotation")
        @Schema(
                description = "Set to true to enable automatic annual key rotation, false to disable",
                example = "true",
                required = true
        )
        private Boolean enableRotation;

        /**
         * Optional custom rotation period in days.
         * If null, uses the default 365 days (annual rotation).
         * Valid range: 90-3650 days.
         */
        @JsonProperty("rotationPeriodDays")
        @Schema(
                description = "Optional custom rotation period in days (default is 365 for annual rotation, min 90, max 3650)",
                example = "365",
                minimum = "90",
                maximum = "3650",
                nullable = true
        )
        private Integer rotationPeriodDays;

        /**
         * Reason for enabling/disabling rotation (for audit purposes).
         * Useful for tracking why rotation was changed.
         */
        @JsonProperty("reason")
        @Schema(
                description = "Optional reason for this rotation change (for audit trail)",
                example = "Compliance requirement for PCI-DSS",
                maxLength = 512,
                nullable = true
        )
        private String reason;

        /**
         * Whether this change should be applied immediately.
         * If false, may be scheduled for off-peak hours.
         */
        @JsonProperty("applyImmediately")
        @Schema(
                description = "Whether to apply this change immediately (true) or during maintenance window (false)",
                example = "false",
                defaultValue = "false"
        )
        private Boolean applyImmediately;

        /**
         * Creates a default request with annual rotation enabled.
         */
        public static UpdateKeyRotationRequestDto enableAnnualRotation() {
            return UpdateKeyRotationRequestDto.builder()
                    .enableRotation(true)
                    .rotationPeriodDays(365)
                    .applyImmediately(true)
                    .build();
        }

        /**
         * Creates a request to disable rotation.
         */
        public static UpdateKeyRotationRequestDto disableRotation() {
            return UpdateKeyRotationRequestDto.builder()
                    .enableRotation(false)
                    .applyImmediately(true)
                    .build();
        }

        /**
         * Creates a request with custom rotation period.
         */
        public static UpdateKeyRotationRequestDto withCustomPeriod(Integer periodDays) {
            if (periodDays < 90 || periodDays > 3650) {
                throw new IllegalArgumentException(
                        "Rotation period must be between 90 and 3650 days. Got: " + periodDays
                );
            }
            return UpdateKeyRotationRequestDto.builder()
                    .enableRotation(true)
                    .rotationPeriodDays(periodDays)
                    .applyImmediately(true)
                    .build();
        }

        /**
         * Validates the request payload.
         *
         * @throws IllegalArgumentException if validation fails
         */
        public void validate() {
            if (enableRotation == null) {
                throw new IllegalArgumentException("enableRotation field is required");
            }

            if (enableRotation && rotationPeriodDays != null) {
                if (rotationPeriodDays < 90 || rotationPeriodDays > 3650) {
                    throw new IllegalArgumentException(
                            "Rotation period must be between 90 and 3650 days. Got: " + rotationPeriodDays
                    );
                }
            }

            if (reason != null && reason.length() > 512) {
                throw new IllegalArgumentException("Reason exceeds maximum length of 512 characters");
            }
        }

        /**
         * Gets the effective rotation period.
         * Returns the custom period if provided, otherwise the default (365 days).
         */
        public Integer getEffectiveRotationPeriod() {
            if (rotationPeriodDays != null) {
                return rotationPeriodDays;
            }
            return 365; // Default annual rotation
        }

        /**
         * Determines if this is a change from previous state.
         * Used to avoid redundant updates.
         *
         * @param currentlyEnabled whether rotation is currently enabled
         * @return true if this request changes the current state
         */
        public boolean isStateChange(boolean currentlyEnabled) {
            return !enableRotation.equals(currentlyEnabled);
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ActiveVersionResponseDto implements Serializable {

        private static final long serialVersionUID = 1L;

        private String keyId;
        private String versionId;
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class GrantResponseDto {

        private String grantId;

        private String grantToken;

        private String keyId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(
            title = "Key Rotation Status",
            description = "Contains key rotation configuration and status information"
    )
    public static class KeyRotationStatusResponseDto {

        /**
         * The ID of the key.
         */
        @JsonProperty("keyId")
        @Schema(
                description = "The unique identifier of the key",
                example = "550e8400-e29b-41d4-a716-446655440000",
                required = true
        )
        private String keyId;

        /**
         * The ARN of the key.
         */
        @JsonProperty("keyArn")
        @Schema(
                description = "The Amazon Resource Name (ARN) of the key",
                example = "arn:aws:kms:us-east-1:123456789012:key/550e8400-e29b-41d4-a716-446655440000",
                required = true
        )
        private String keyArn;

        /**
         * Whether automatic key rotation is enabled.
         */
        @JsonProperty("rotationEnabled")
        @Schema(
                description = "Whether automatic annual key rotation is enabled",
                example = "true",
                required = true
        )
        private Boolean rotationEnabled;

        /**
         * The rotation period in days (typically 365 for annual rotation).
         */
        @JsonProperty("rotationPeriodDays")
        @Schema(
                description = "The rotation period in days (standard is 365 for annual rotation)",
                example = "365",
                required = true
        )
        private Integer rotationPeriodDays;

        /**
         * The date of the most recent key rotation.
         * Null if the key has never been rotated.
         */
        @JsonProperty("lastRotationDate")
        @Schema(
                description = "The date and time of the last key rotation (ISO 8601 format)",
                example = "2024-01-15T10:30:00Z",
                nullable = true
        )
        private LocalDateTime lastRotationDate;

        /**
         * The date of the next scheduled automatic rotation.
         * Null if automatic rotation is disabled.
         */
        @JsonProperty("nextRotationDate")
        @Schema(
                description = "The date and time of the next scheduled automatic rotation (ISO 8601 format)",
                example = "2025-01-15T10:30:00Z",
                nullable = true
        )
        private LocalDateTime nextRotationDate;

        /**
         * Number of versions (rotations) of the key.
         */
        @JsonProperty("versionCount")
        @Schema(
                description = "The total number of key versions (rotations) that exist",
                example = "5",
                required = true
        )
        private Integer versionCount;

        /**
         * The creation date of the key.
         */
        @JsonProperty("keyCreationDate")
        @Schema(
                description = "The date and time the key was created (ISO 8601 format)",
                example = "2020-06-10T08:15:00Z",
                required = true
        )
        private LocalDateTime keyCreationDate;

        /**
         * The current state of the key.
         */
        @JsonProperty("keyState")
        @Schema(
                description = "The current state of the key (Enabled, Disabled, PendingDeletion, etc.)",
                example = "Enabled",
                required = true,
                allowableValues = {"Enabled", "Disabled", "PendingDeletion", "PendingImport"}
        )
        private String keyState;

        /**
         * Whether the key is a customer managed key (vs WAMS managed).
         */
        @JsonProperty("isCustomerManagedKey")
        @Schema(
                description = "Whether the key is customer managed (true) or WAMS managed (false)",
                example = "true",
                required = true
        )
        private Boolean isCustomerManagedKey;

        /**
         * The reason rotation is not enabled (if applicable).
         * Only populated when rotationEnabled is false.
         */
        @JsonProperty("rotationDisabledReason")
        @Schema(
                description = "Reason why rotation is disabled (e.g., 'ManuallyDisabled', 'ImportedKeyMaterial')",
                example = "ManuallyDisabled",
                nullable = true
        )
        private String rotationDisabledReason;

        /**
         * Days until next automatic rotation.
         */
        @JsonProperty("daysUntilNextRotation")
        @Schema(
                description = "Days remaining until the next automatic rotation occurs (-1 if rotation is disabled)",
                example = "156",
                nullable = true
        )
        private Integer daysUntilNextRotation;

        /**
         * Last rotation initiated by principal (for auditing).
         */
        @JsonProperty("lastRotationInitiatedBy")
        @Schema(
                description = "The principal (user/role/service) that last initiated key rotation",
                example = "arn:aws:iam::123456789012:user/admin",
                nullable = true
        )
        private String lastRotationInitiatedBy;

        /**
         * Calculates days until next rotation.
         * Returns -1 if rotation is disabled or nextRotationDate is null.
         */
        public void calculateDaysUntilNextRotation() {
            if (rotationEnabled && nextRotationDate != null) {
                LocalDateTime now = LocalDateTime.now();
                this.daysUntilNextRotation = Math.toIntExact(
                        java.time.temporal.ChronoUnit.DAYS.between(now, nextRotationDate)
                );
            } else {
                this.daysUntilNextRotation = -1;
            }
        }

        /**
         * Determines if rotation is overdue (if enabled).
         */
        public boolean isRotationOverdue() {
            if (!rotationEnabled || nextRotationDate == null) {
                return false;
            }
            return LocalDateTime.now().isAfter(nextRotationDate);
        }

        /**
         * Determines if a rotation happened recently (within N days).
         */
        public boolean rotatedWithinDays(int days) {
            if (lastRotationDate == null) {
                return false;
            }
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
            return lastRotationDate.isAfter(cutoffDate);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeyUsageStatsResponseDto {

        /**
         * The ID of the key
         */
        private String keyId;

        /**
         * The ARN of the key
         */
        private String keyArn;

        /**
         * Total number of encrypt operations performed with this key
         */
        private Long encryptCount;

        /**
         * Total number of decrypt operations performed with this key
         */
        private Long decryptCount;

        /**
         * Total number of generate data key operations
         */
        private Long generateDataKeyCount;

        /**
         * Total number of re-encrypt operations
         */
        private Long reEncryptCount;

        private Long signCount;
        private Long verifyCount;

        /**
         * Date and time when the key was last used
         */
        private LocalDateTime lastUsedDate;

        /**
         * Date and time when the key was first used
         */
        private LocalDateTime firstUsedDate;

        /**
         * Average operations per day (last 30 days)
         */
        private Double averageOpsPerDay;

        /**
         * Usage by operation type breakdown
         * Map of operation type to count
         */
        private Map<String, Long> usageByOperation;

        /**
         * Hourly usage distribution (last 24 hours)
         * Map of hour to count
         */
        private Map<Integer, Long> hourlyDistribution;

        /**
         * Daily usage distribution (last 30 days)
         * Map of date to count
         */
        private Map<String, Long> dailyDistribution;

        /**
         * Whether usage tracking is enabled for this key
         */
        private Boolean usageTrackingEnabled;

        /**
         * The date when usage tracking started
         */
        private LocalDateTime trackingStartDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListGrantsResponseDto {
        private List<GrantDto> grants;
        private String nextToken;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class GrantDto {
            private String grantId;
            private String granteePrincipal;
            private String retiringPrincipal;
            private List<String> operations;
            private String constraints;
            private LocalDateTime createdAt;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListKeyRotationsResponseDto {

        /**
         * List of key rotations
         */
        private List<RotationDto> rotations;

        /**
         * Token for pagination to get the next page of results
         */
        private String nextToken;

        /**
         * Total number of rotations
         */
        private Integer totalCount;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class RotationDto {
            /**
             * The version ID of the rotated key
             */
            private String versionId;

            /**
             * The date and time when the rotation occurred
             */
            private LocalDateTime rotationDate;

            /**
             * The status of the rotated version
             */
            private String status;

            /**
             * Optional description of the rotation
             */
            private String description;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ListKeysResponseDto {

        private List<KeySummaryDto> keys;

        private String nextToken;

        /**
         * The type Key summary dto.
         */
        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class KeySummaryDto {
            private String keyId;
            private String alias;
            private IEnumKeyStatus.Types status;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportParametersResponseDto {

        /**
         * The ID of the key for which import parameters are being retrieved
         */
        private String keyId;

        /**
         * The ARN of the key
         */
        private String keyArn;

        /**
         * The wrapping key (public key) used to encrypt the key material
         * This is typically an RSA public key
         */
        private byte[] wrappingKey;

        /**
         * The import token that must be used when importing the key material
         * This token binds the imported key material to the specific key
         */
        private byte[] importToken;

        /**
         * The algorithm used to encrypt the key material
         * Possible values: RSAES_PKCS1_V1_5, RSAES_OAEP_SHA_1, RSAES_OAEP_SHA_256
         */
        private String wrappingAlgorithm;

        /**
         * The validity period of the import parameters in hours
         * After this period, the parameters expire and cannot be used
         * Default is typically 24 hours
         */
        private Integer validityPeriodHours;

        /**
         * The expiration date and time of the import parameters
         */
        private LocalDateTime expirationDate;

        /**
         * The date and time when the parameters were generated
         */
        private LocalDateTime parametersGeneratedAt;

        private LocalDateTime validTo;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListAliasesResponseDto {
        private List<AliasResponseDto> aliases;
        private String nextToken;
        private String nextMarker;
        private Boolean truncated;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListCustomKeyStoresResponseDto {
        private List<CustomKeyStoreResponseDto> customKeyStores;
        private String nextToken;
        private boolean truncated;
        private String nextMarker;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListTagsResponseDto {
        private List<TagDto> tags;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class KeyDescriptionResponseDto {

        private String keyId;

        private IEnumKeyStatus.Types status;

        private IEnumKeySpec.Types keySpec;

        private IEnumKeyUsage.Types keyUsage;

        private String currentVersion;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        private String alias;

        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomKeyStoreResponseDto {

        LocalDateTime lastSuccessfulConnection;
        private Long keyStoreId;
        private String keyStoreName;
        private IEnumCustomKeyStoreType.Types type;
        private IEnumCustomKeyStoreStatus.Types status;
        private String connectionState;
        private String endpoint;
        private String vendor;
        private String region;
        private Boolean connected;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String cloudHsmClusterId;
        private String xksProxyUriEndpoint;
        private String xksProxyUriPath;
        private Map<String, String> configuration;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class AuditLogResponseDto {

        private List<AuditLogEntryDto> logs;

        /**
         * The type Audit log entry dto.
         */
        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class AuditLogEntryDto {
            private String action;
            private String keyId;

            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            private LocalDateTime timestamp;

            private String principal;
            private String ipAddress;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AliasResponseDto {
        private String aliasName;
        private String targetKeyId;
        private String targetKeyArn;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class SetKeyPolicyRequestDto {

        private String policyName;

        @NotNull(message = "policy cannot be null")
        private Map<String, Object> policy;

        @JsonProperty("BypassPolicyLockoutSafetyCheck")
        private Boolean bypassPolicyLockoutSafetyCheck;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UntagResourceRequestDto {
        @NotEmpty
        private List<String> tagKeys;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdatePrimaryRegionRequestDto {

        private String keyId;
        @NotBlank
        private String primaryRegion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagResourceRequestDto {
        @NotEmpty
        private Map<String, String> tags;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReplicateKeyRequestDto {

        private String keyId;
        @NotBlank
        private String replicaRegion;

        private String description;

        private Boolean enabled;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CreateGrantRequestDto {

        @NotBlank(message = "principal cannot be blank")
        private String principal;
        private String granteePrincipal;
        @NotEmpty(message = "operations cannot be empty")
        private List<String> operations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListAliasesRequest {
        private Integer limit;
        private String marker;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListAliasesForKeyRequest {
        private String keyId;
        private Integer limit;
        private String marker;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListAliasesResponse {
        private List<AliasEntry> aliases;
        private String nextMarker;
        private Boolean truncated;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AliasEntry {
            private String aliasName;
            private String aliasArn;
            private String targetKeyId;
            private String creationDate;
            private String lastUpdatedDate;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateAliasRequest {
        private String aliasName;
        private String targetKeyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateAliasResponse {
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateAliasRequest {
        private String aliasName;
        private String targetKeyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateAliasResponse {
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteAliasRequest {
        private String aliasName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteAliasResponse {
        private String keyId;
    }

    // =========================================================================
    // Tags
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResourceTagsRequest {
        private String keyId;
        private Integer limit;
        private String marker;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResourceTagsResponse {
        private List<Tag> tags;
        private String nextMarker;
        private Boolean truncated;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Tag {
            private String tagKey;
            private String tagValue;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagResourceRequest {
        private String keyId;
        private List<ListResourceTagsResponse.Tag> tags;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagResourceResponse {
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UntagResourceRequest {
        private String keyId;
        private List<String> tagKeys;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UntagResourceResponse {
        private String keyId;
    }

    // =========================================================================
    // Key Policies & Grants
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PutKeyPolicyRequest {
        private String keyId;
        private String policyName;   // default "default"
        private Map<String, Object> policy;    // JSON document
        @JsonProperty("BypassPolicyLockoutSafetyCheck")
        private Boolean bypassPolicyLockoutSafetyCheck;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PutKeyPolicyResponse {
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetKeyPolicyRequest {
        private String keyId;
        private String policyName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetKeyPolicyResponse {
        private String policy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListKeyPoliciesRequest {
        private String keyId;
        private Integer limit;
        private String marker;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListKeyPoliciesResponse {
        private List<String> policyNames;
        private String nextMarker;
        private Boolean truncated;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateGrantRequest {
        private String keyId;
        private String granteePrincipal;
        private String retiringPrincipal;
        private List<String> operations;
        private GrantConstraints constraints;
        private List<String> grantTokens;
        private String name;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class GrantConstraints {
            private Map<String, String> encryptionContextSubset;
            private Map<String, String> encryptionContextEquals;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateGrantResponse {
        private String grantId;
        private String grantToken;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListGrantsRequest {
        private String keyId;
        private Integer limit;
        private String marker;
        private String grantId;
        private String granteePrincipal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListGrantsResponse {
        private List<Grant> grants;
        private String nextMarker;
        private Boolean truncated;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Grant {
            private String grantId;
            private String granteePrincipal;
            private String retiringPrincipal;
            private List<String> operations;
            private CreateGrantRequest.GrantConstraints constraints;
            private String creationDate;
            private String lastUpdatedDate;
            private String keyId;
            private String name;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevokeGrantRequest {
        private String keyId;
        private String grantId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevokeGrantResponse {
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetireGrantRequest {
        private String grantToken;
        private String keyId;
        private String grantId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetireGrantResponse {
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListRetirableGrantsRequest {
        private String retiringPrincipal;
        private Integer limit;
        private String marker;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListRetirableGrantsResponse {
        private List<ListGrantsResponse.Grant> grants;
        private String nextMarker;
        private Boolean truncated;
    }

    // =========================================================================
    // BYOK (Key Material Import)
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetParametersForImportRequest {
        private String keyId;
        private String wrappingAlgorithm; // RSAES_OAEP_SHA_256, RSAES_PKCS1_V1_5
        private String wrappingKeySpec;   // RSA_2048
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetParametersForImportResponse {
        private String keyId;
        private String importToken;
        private String publicKey;
        private LocalDateTime validTo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportKeyMaterialRequest {
        private String keyId;
        private String importToken;
        private String encryptedKeyMaterial;
        private LocalDateTime validTo;
        private IEnumKeyExpirationModel.Types expirationModel; // KEY_MATERIAL_EXPIRES, KEY_MATERIAL_DOES_NOT_EXPIRE
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportKeyMaterialResponse {
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteImportedKeyMaterialRequest {
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteImportedKeyMaterialResponse {
        private String keyId;
    }

    // =========================================================================
    // Custom Key Stores
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCustomKeyStoreRequest {
        private String customKeyStoreName;
        private String cloudHsmClusterId;
        private String keyStorePassword;
        private String trustAnchorCertificate;
        private IEnumCustomKeyStoreType.Types customKeyStoreType; // WAMS_CLOUDHSM, EXTERNAL_KEY_STORE
        private String xksProxyUriEndpoint;
        private String xksProxyUriPath;
        private String xksProxyAuthenticationCredential;
        private String xksProxyConnectivity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCustomKeyStoreResponse {
        private Long customKeyStoreId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DescribeCustomKeyStoreRequest {
        private Long customKeyStoreId;
        private String customKeyStoreName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DescribeCustomKeyStoreResponse {
        private CustomKeyStore customKeyStore;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CustomKeyStore {
            private Long customKeyStoreId;
            private String customKeyStoreName;
            private LocalDateTime creationDate;          // ISO 8601
            private String connectionState;       // CONNECTED, DISCONNECTED, FAILED
            private String connectionErrorCode;
            private String cloudHsmClusterId;     // for CloudHSM type
            private String trustAnchorCertificate;
            private String customKeyStoreType;     // AWS_CLOUDHSM, EXTERNAL_KEY_STORE
            private String xksProxyUriEndpoint;    // for external key store
            private String xksProxyUriPath;
            private String xksProxyAuthenticationCredential;
            private String xksProxyConnectivity;   // PUBLIC_ENDPOINT, VPC_ENDPOINT_SERVICE
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateCustomKeyStoreRequest {
        private Long customKeyStoreId;
        private String newCustomKeyStoreName;
        private String keyStorePassword;
        private String cloudHsmClusterId;
        private String xksProxyUriEndpoint;
        private String xksProxyUriPath;
        private String xksProxyAuthenticationCredential;
        private String xksProxyConnectivity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateCustomKeyStoreResponse {
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteCustomKeyStoreRequest {
        private Long customKeyStoreId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteCustomKeyStoreResponse {
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectCustomKeyStoreRequest {
        private Long customKeyStoreId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectCustomKeyStoreResponse {
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisconnectCustomKeyStoreRequest {
        private Long customKeyStoreId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisconnectCustomKeyStoreResponse {
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListCustomKeyStoresRequest {
        private Integer limit;
        private String marker;
        private Long customKeyStoreId;
        private String customKeyStoreName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListCustomKeyStoresResponse {
        private List<DescribeCustomKeyStoreResponse.CustomKeyStore> customKeyStores;
        private String nextMarker;
        private Boolean truncated;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdatePrimaryRegionResponse {
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReplicateKeyResponse {
        private KeyMetadata replicaKeyMetadata;
        private String replicaRegion;
        private String replicationStatus;  // ENABLED, DISABLED, etc.

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class KeyMetadata {
            private String awsAccountId;
            private String keyId;
            private String arn;
            private LocalDateTime creationDate;
            private Boolean enabled;
            private String description;
            private IEnumKeySpec.Types keySpec;
            private IEnumKeyUsage.Types keyUsage;
            private IEnumKeyStatus.Types keyStatus;
            private IEnumKeyOrigin.Types origin;
            private Boolean multiRegion;
            private String multiRegionConfiguration;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SynchronizeMultiRegionKeyRequest {
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SynchronizeMultiRegionKeyResponse {
        private String keyId;
    }

    // Audit & utility
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditLogRequest {
        private String keyId;
        private String fromDate;
        private String toDate;
        private Integer limit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditLogResponse {
        private List<LogEntry> logs;

        @Data
        @Builder
        public static class LogEntry {
            LocalDateTime timestamp;
            String action;
            String keyId;
            String principal;
            String ipAddress;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeyUsageStatsResponse {
        private String keyId;
        private Long encryptCount;
        private Long decryptCount;
        private Long signCount;
        private Long verifyCount;
        private LocalDateTime lastUsedDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidateKeyResponse {
        private Boolean valid;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListKeyRotationsRequestDto {
        private String keyId;
        private Integer limit;
        private String nextToken;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveVersionRequestDto {
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidateKeyRequest {
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeyUsageStatsRequest {
        private String keyId;
    }

    // =========================================================================
    // Error Response (common)
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KmsErrorResponse {
        private String __type;    // e.g. "NotFoundException"
        private String message;
    }
}