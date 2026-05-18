package eu.isygoit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.isygoit.annotation.ValidCreateCustomKeyStoreRequest;
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
 * Container class for all AWS KMS‑style DTOs (Requests and Responses).
 * Aligned with the internal entity model.
 */
public final class KmsDtos {

    // =========================================================================
    // Key Management
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request to create a new KMS key")
    public static class CreateKeyRequest {
        @Schema(description = "Human‑readable description of the key (max 1024 characters)", example = "My encryption key")
        private String description;

        @Schema(description = "Cryptographic key specification (type and length)", required = true)
        private IEnumKeySpec.Types keySpec;

        @Schema(description = "Intended usage of the key", required = true)
        private IEnumKeyUsage.Types keyUsage;

        @Schema(description = "Optional friendly alias (e.g., 'alias/my-key')")
        private String keyAlias;

        @Schema(description = "Origin of the key material", defaultValue = "WAMS_KMS")
        private IEnumKeyOrigin.Types origin;

        @Schema(description = "Tags to attach to the key (max 50)")
        private List<Tag> tags;

        @Schema(description = "Whether automatic rotation is enabled (default false)")
        private Boolean rotationEnabled;

        @Schema(description = "Rotation period in days (default 365). Only used if rotationEnabled = true")
        private Integer rotationPeriodInDays;

        @Schema(description = "Whether this key is part of a multi‑region setup")
        private Boolean multiRegion;

        @Schema(description = "Primary region for multi‑region key")
        private String primaryRegion;

        @Schema(description = "Comma‑separated list of replica regions for multi‑region key")
        private String replicaRegions;

        @Schema(description = "Bypass policy lockout safety check (use with caution)")
        @JsonProperty("BypassPolicyLockoutSafetyCheck")
        private Boolean bypassPolicyLockoutSafetyCheck;

        @Schema(description = "IAM policy document as a JSON object")
        private Map<String, Object> policy;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "Key‑value tag")
        public static class Tag {
            @Schema(description = "Tag key (max 128 characters)", required = true)
            private String tagKey;

            @Schema(description = "Tag value (max 256 characters)", required = true)
            private String tagValue;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Response containing key metadata")
    public static class CreateKeyResponse {
        @Schema(description = "Detailed key metadata")
        private KeyMetadata keyMetadata;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "Key metadata (subset of full key properties)")
        public static class KeyMetadata {
            @Schema(description = "Tenant (account) that owns the key")
            private String tenant;

            @Schema(description = "Unique key identifier (UUID)")
            private String keyId;

            @Schema(description = "Key ARN (WAMS Resource Name)")
            private String wrn;

            @Schema(description = "Key creation timestamp")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            private LocalDateTime createDate;

            @Schema(description = "Whether the key is enabled")
            private Boolean enabled;

            @Schema(description = "Key description")
            private String description;

            @Schema(description = "Whether automatic rotation is enabled")
            private Boolean rotationEnabled;

            @Schema(description = "Rotation period in days (default 365). Only used if rotationEnabled = true")
            private Integer rotationPeriodInDays;

            @Schema(description = "Key specification")
            private IEnumKeySpec.Types keySpec;

            @Schema(description = "Key usage")
            private IEnumKeyUsage.Types keyUsage;

            @Schema(description = "Current active version ID")
            private String currentVersion;

            @Schema(description = "Origin of key material")
            private IEnumKeyOrigin.Types origin;

            @Schema(description = "Current key state (Enabled, Disabled, PendingDeletion, etc.)")
            private IEnumKeyStatus.Types keyStatus;

            @Schema(description = "Creation timestamp (alias for createDate)")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            private LocalDateTime createdAt;

            @Schema(description = "Last update timestamp")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            private LocalDateTime updatedAt;

            @Schema(description = "Primary alias (if any)")
            private String keyAlias;

            @Schema(description = "Expiration model for imported key material")
            private IEnumKeyExpirationModel.Types expirationModel;

            @Schema(description = "Alias for keySpec (for AWS compatibility)")
            private String customerMasterKeySpec;

            @Schema(description = "List of supported encryption algorithms (derived)")
            private List<String> encryptionAlgorithmSpecs;

            @Schema(description = "List of supported signing algorithms (derived)")
            private List<String> signingAlgorithms;

            @Schema(description = "Key manager (WAMS or CUSTOMER)")
            private String keyManager;

            @Schema(description = "Whether this key is multi‑region")
            private Boolean multiRegion;

            @Schema(description = "Multi‑region configuration (if applicable)")
            private Object multiRegionConfiguration;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Response containing full key metadata")
    public static class DescribeKeyResponse {
        @Schema(description = "Full key metadata")
        private CreateKeyResponse.KeyMetadata keyMetadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Response listing keys (basic info)")
    public static class ListKeysResponse {
        @Schema(description = "List of key entries")
        private List<KeyEntry> keys;

        @Schema(description = "Pagination token for next page")
        private String nextToken;

        @Schema(description = "Whether the result list is truncated")
        private Boolean truncated;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "Basic key entry")
        public static class KeyEntry {
            @Schema(description = "Key ID")
            private String keyId;

            @Schema(description = "Key ARN")
            private String keyWrn;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Response after scheduling key deletion")
    public static class ScheduleKeyDeletionResponse {
        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "Key status (should be PENDING_DELETION)")
        private IEnumKeyStatus.Types keyStatus;

        @Schema(description = "Date when the key will be permanently deleted")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime deletionDate;

        @Schema(description = "Key state as string")
        private String keyState;

        @Schema(description = "Waiting period in days")
        private Integer pendingWindowInDays;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Response after cancelling key deletion")
    public static class CancelKeyDeletionResponse {
        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "Restored key status (ENABLED or DISABLED)")
        private IEnumKeyStatus.Types keyStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Response after permanent key deletion")
    public static class DeleteKeyResponse {
        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "Final key status")
        private IEnumKeyStatus.Types keyStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Response after enabling a key")
    public static class EnableKeyResponse {
        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "New key status (ENABLED)")
        private IEnumKeyStatus.Types keyStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Response after disabling a key")
    public static class DisableKeyResponse {
        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "New key status (DISABLED)")
        private IEnumKeyStatus.Types status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request to update key description and/or alias")
    public static class UpdateKeyDescriptionRequest {
        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "New alias (optional)")
        private String keyAlias;

        @Schema(description = "New description")
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Response after updating key description")
    public static class UpdateKeyDescriptionResponse {
        @Schema(description = "Updated key metadata")
        private CreateKeyResponse.KeyMetadata keyMetadata;
    }

    // =========================================================================
    // Key Rotation
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Response with key rotation status")
    public static class GetKeyRotationStatusResponse {
        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "Whether rotation is enabled")
        private Boolean rotationEnabled;

        @Schema(description = "Rotation period in days")
        private Integer rotationPeriodInDays;

        @Schema(description = "Date of last rotation")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime lastRotationDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request to enable key rotation")
    public static class EnableKeyRotationResponse {
        @Schema(description = "Key ID")
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request to disable key rotation")
    public static class DisableKeyRotationResponse {
        @Schema(description = "Key ID")
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Response after manual key rotation")
    public static class RotateKeyResponse {
        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "New version ID")
        private String newVersionId;

        @Schema(description = "Rotation timestamp")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime rotationDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Response listing key versions")
    public static class ListKeyVersionsResponse {
        @Schema(description = "List of versions")
        private List<KeyVersion> versions;

        @Schema(description = "Pagination token")
        private String nextToken;

        @Schema(description = "Whether truncated")
        private Boolean truncated;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "Key version entry")
        public static class KeyVersion {
            @Schema(description = "Key ID")
            private String keyId;

            @Schema(description = "Version ID")
            private String versionId;

            @Schema(description = "Creation date")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            private LocalDateTime createDate;

            @Schema(description = "Status")
            private IEnumKeyStatus.Types status;

            @Schema(description = "Signing algorithm (if asymmetric)")
            private String signingAlgorithm;

            @Schema(description = "Expiration model (for imported)")
            private IEnumKeyExpirationModel.Types expirationModel;

            @Schema(description = "Origin")
            private IEnumKeyOrigin.Types origin;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Response containing key rotation history")
    public static class ListKeyRotationsResponse {
        @Schema(description = "List of rotations")
        private List<RotationDto> rotations;

        @Schema(description = "Pagination token")
        private String nextToken;

        @Schema(description = "Total number of rotations")
        private Integer totalCount;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "Single rotation entry")
        public static class RotationDto {
            @Schema(description = "Version ID")
            private String versionId;

            @Schema(description = "Rotation date")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            private LocalDateTime rotationDate;

            @Schema(description = "Status")
            private String status;

            @Schema(description = "Optional description")
            private String description;
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "Current active key version")
    public static class ActiveVersionResponse implements Serializable {
        private static final long serialVersionUID = 1L;
        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "Active version ID")
        private String versionId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Response after updating key rotation settings")
    public static class UpdateKeyRotationResponse {
        private String keyId;
        private Boolean rotationEnabled;
        private Integer rotationPeriodInDays;
        private LocalDateTime lastRotationDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Request to update key rotation settings")
    public static class UpdateKeyRotationRequest {
        @Schema(hidden = true)
        private String keyId;

        @JsonProperty("enableRotation")
        @Schema(description = "Enable/disable automatic rotation", required = true)
        private Boolean enableRotation;

        @JsonProperty("rotationPeriodInDays")
        @Schema(description = "Custom rotation period in days (90‑3650)", minimum = "90", maximum = "3650")
        private Integer rotationPeriodInDays;

        @JsonProperty("reason")
        @Schema(description = "Audit reason for change", maxLength = 512)
        private String reason;

        @JsonProperty("applyImmediately")
        @Schema(description = "Apply change immediately", defaultValue = "false")
        private Boolean applyImmediately;

        public static UpdateKeyRotationRequest enableAnnualRotation() {
            return UpdateKeyRotationRequest.builder()
                    .enableRotation(true)
                    .rotationPeriodInDays(365)
                    .applyImmediately(true)
                    .build();
        }

        public static UpdateKeyRotationRequest disableRotation() {
            return UpdateKeyRotationRequest.builder()
                    .enableRotation(false)
                    .applyImmediately(true)
                    .build();
        }

        public static UpdateKeyRotationRequest withCustomPeriod(Integer periodDays) {
            if (periodDays < 90 || periodDays > 3650) {
                throw new IllegalArgumentException("Rotation period must be between 90 and 3650 days");
            }
            return UpdateKeyRotationRequest.builder()
                    .enableRotation(true)
                    .rotationPeriodInDays(periodDays)
                    .applyImmediately(true)
                    .build();
        }

        public void validate() {
            if (enableRotation == null) throw new IllegalArgumentException("enableRotation is required");
            if (enableRotation && rotationPeriodInDays != null && (rotationPeriodInDays < 90 || rotationPeriodInDays > 3650)) {
                throw new IllegalArgumentException("Rotation period must be between 90 and 3650 days");
            }
            if (reason != null && reason.length() > 512)
                throw new IllegalArgumentException("Reason too long (max 512)");
        }

        public Integer getEffectiveRotationPeriod() {
            return rotationPeriodInDays != null ? rotationPeriodInDays : 365;
        }

        public boolean isStateChange(boolean currentlyEnabled) {
            return !enableRotation.equals(currentlyEnabled);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Detailed key rotation status (full info)")
    public static class KeyRotationStatusResponseDto {
        @JsonProperty("keyId")
        @Schema(description = "Key ID")
        private String keyId;

        @JsonProperty("keyWrn")
        @Schema(description = "Key ARN")
        private String keyWrn;

        @JsonProperty("rotationEnabled")
        @Schema(description = "Whether automatic rotation is enabled")
        private Boolean rotationEnabled;

        @JsonProperty("rotationPeriodInDays")
        @Schema(description = "Rotation period in days")
        private Integer rotationPeriodInDays;

        @JsonProperty("lastRotationDate")
        @Schema(description = "Date of last rotation")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime lastRotationDate;

        @JsonProperty("nextRotationDate")
        @Schema(description = "Date of next scheduled rotation")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime nextRotationDate;

        @JsonProperty("versionCount")
        @Schema(description = "Number of key versions")
        private Integer versionCount;

        @JsonProperty("keyCreateDate")
        @Schema(description = "Key creation date")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime keyCreateDate;

        @JsonProperty("keyState")
        @Schema(description = "Current key state")
        private String keyState;

        @JsonProperty("isCustomerManagedKey")
        @Schema(description = "Whether the key is customer managed")
        private Boolean isCustomerManagedKey;

        @JsonProperty("rotationDisabledReason")
        @Schema(description = "Reason if rotation is disabled")
        private String rotationDisabledReason;

        @JsonProperty("daysUntilNextRotation")
        @Schema(description = "Days until next rotation (-1 if disabled)")
        private Integer daysUntilNextRotation;

        @JsonProperty("lastRotationInitiatedBy")
        @Schema(description = "Principal that initiated last rotation")
        private String lastRotationInitiatedBy;

        public void calculateDaysUntilNextRotation() {
            if (Boolean.TRUE.equals(rotationEnabled) && nextRotationDate != null) {
                this.daysUntilNextRotation = (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), nextRotationDate);
            } else {
                this.daysUntilNextRotation = -1;
            }
        }

        public boolean isRotationOverdue() {
            return Boolean.TRUE.equals(rotationEnabled) && nextRotationDate != null && LocalDateTime.now().isAfter(nextRotationDate);
        }

        public boolean rotatedWithinDays(int days) {
            return lastRotationDate != null && lastRotationDate.isAfter(LocalDateTime.now().minusDays(days));
        }
    }

    // =========================================================================
    // Cryptographic Operations (all standard)
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Encrypt request")
    public static class EncryptRequest {
        @Schema(description = "Key ID", required = true)
        private String keyId;

        @Schema(description = "Plaintext as base64‑encoded string", required = true)
        private String plaintext;

        @Schema(description = "Encryption context (key‑value pairs)")
        private Map<String, String> encryptionContext;

        @Schema(description = "Grant tokens (optional)")
        private List<String> grantTokens;

        @Schema(description = "Encryption algorithm spec")
        private String encryptionAlgorithmSpec;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Encrypt response")
    public static class EncryptResponse {
        @Schema(description = "Ciphertext as base64‑encoded string")
        private String ciphertextBlob;

        @Schema(description = "Key ID used")
        private String keyId;

        @Schema(description = "Key version ID used")
        private String keyVersionId;

        @Schema(description = "Algorithm used")
        private String encryptionAlgorithmSpec;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Decrypt request")
    public static class DecryptRequest {
        @Schema(description = "Ciphertext as base64‑encoded string", required = true)
        private String ciphertextBlob;

        @Schema(description = "Encryption context (key‑value pairs)")
        private Map<String, String> encryptionContext;

        @Schema(description = "Grant tokens (optional)")
        private List<String> grantTokens;

        @Schema(description = "Key ID (optional, can be derived)")
        private String keyId;

        @Schema(description = "Encryption algorithm spec")
        private String encryptionAlgorithmSpec;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Decrypt response")
    public static class DecryptResponse {
        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "Key version ID")
        private String keyVersionId;

        @Schema(description = "Plaintext as base64‑encoded string")
        private String plaintext;

        @Schema(description = "Algorithm used")
        private String encryptionAlgorithmSpec;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Re‑encrypt request")
    public static class ReEncryptRequest {
        @Schema(description = "Source key ID", required = true)
        private String sourceKeyId;

        @Schema(description = "Destination key ID", required = true)
        private String destinationKeyId;

        @Schema(description = "Ciphertext to re‑encrypt", required = true)
        private String ciphertextBlob;

        @Schema(description = "Source encryption context")
        private Map<String, String> sourceEncryptionContext;

        @Schema(description = "Destination encryption context")
        private Map<String, String> destinationEncryptionContext;

        @Schema(description = "Grant tokens")
        private List<String> grantTokens;

        @Schema(description = "Source encryption algorithm")
        private String sourceEncryptionAlgorithmSpec;

        @Schema(description = "Destination encryption algorithm")
        private String destinationEncryptionAlgorithmSpec;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Re‑encrypt response")
    public static class ReEncryptResponse {
        @Schema(description = "Re‑encrypted ciphertext")
        private String ciphertextBlob;

        @Schema(description = "Source key ID")
        private String sourceKeyId;

        @Schema(description = "Destination key ID")
        private String destinationKeyId;

        @Schema(description = "Destination key version ID")
        private String destinationKeyVersionId;

        @Schema(description = "Destination encryption algorithm")
        private String destinationEncryptionAlgorithmSpec;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Generate data key request")
    public static class GenerateDataKeyRequest {
        @Schema(description = "Key ID", required = true)
        private String keyId;

        @Schema(description = "Key specification (e.g., AES_256)")
        private String keySpec;

        @Schema(description = "Key size in bits (e.g., 256)")
        private Integer keySize;

        @Schema(description = "Encryption context")
        private Map<String, String> encryptionContext;

        @Schema(description = "Grant tokens")
        private List<String> grantTokens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Generate data key response")
    public static class GenerateDataKeyResponse {
        @Schema(description = "Encrypted data key (ciphertext)")
        private String ciphertextBlob;

        @Schema(description = "Plaintext data key (base64‑encoded)")
        private String plaintext;

        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "Encryption algorithm used")
        private String encryptionAlgorithmSpec;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Generate data key without plaintext request")
    public static class GenerateDataKeyWithoutPlaintextRequest {
        @Schema(description = "Key ID", required = true)
        private String keyId;

        @Schema(description = "Key specification")
        private String keySpec;

        @Schema(description = "Key size in bits")
        private Integer keySize;

        @Schema(description = "Encryption context")
        private Map<String, String> encryptionContext;

        @Schema(description = "Grant tokens")
        private List<String> grantTokens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Generate data key without plaintext response")
    public static class GenerateDataKeyWithoutPlaintextResponse {
        @Schema(description = "Encrypted data key (ciphertext)")
        private String ciphertextBlob;

        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "Encryption algorithm used")
        private String encryptionAlgorithmSpec;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Generate data key pair request")
    public static class GenerateDataKeyPairRequest {
        @NotNull
        @Schema(description = "Key ID", required = true)
        private String keyId;

        @NotNull
        @Schema(description = "Key pair specification (e.g., RSA_2048)", required = true)
        private IEnumKeySpec.Types keyPairSpec;

        @Schema(description = "Encryption context")
        private Map<String, String> encryptionContext;

        @Schema(description = "Grant tokens")
        private List<String> grantTokens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Generate data key pair response")
    public static class GenerateDataKeyPairResponse {
        @Schema(description = "Public key (DER or PEM, base64‑encoded)")
        private String publicKey;

        @Schema(description = "Encrypted private key (ciphertext)")
        private String privateKeyCiphertextBlob;

        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "Key pair specification")
        private IEnumKeySpec.Types keyPairSpec;

        @Schema(description = "Encryption algorithm used")
        private String encryptionAlgorithmSpec;

        @Schema(description = "Key version ID")
        private String keyVersionId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Generate data key pair without plaintext request")
    public static class GenerateDataKeyPairWithoutPlaintextRequest {
        @Schema(description = "Key ID", required = true)
        private String keyId;

        @Schema(description = "Key pair specification", required = true)
        private IEnumKeySpec.Types keyPairSpec;

        @Schema(description = "Encryption context")
        private Map<String, String> encryptionContext;

        @Schema(description = "Grant tokens")
        private List<String> grantTokens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Generate data key pair without plaintext response")
    public static class GenerateDataKeyPairWithoutPlaintextResponse {
        @Schema(description = "Public key (base64‑encoded)")
        private String publicKey;

        @Schema(description = "Encrypted private key (ciphertext)")
        private String privateKeyCiphertextBlob;

        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "Key version ID")
        private String keyVersionId;

        @Schema(description = "Key pair specification")
        private IEnumKeySpec.Types keyPairSpec;

        @Schema(description = "Encryption algorithm used")
        private String encryptionAlgorithmSpec;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Sign request")
    public static class SignRequest {
        @Schema(description = "Key ID", required = true)
        private String keyId;

        @Schema(description = "Message (base64‑encoded)", required = true)
        private String message;

        @Schema(description = "Message type (RAW or DIGEST)")
        private String messageType;

        @Schema(description = "Signing algorithm", required = true)
        private String signingAlgorithm;

        @Schema(description = "Grant tokens")
        private List<String> grantTokens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Sign response")
    public static class SignResponse {
        @Schema(description = "Signature (base64‑encoded)")
        private String signature;

        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "Key version ID")
        private String keyVersionId;

        @Schema(description = "Signing algorithm used")
        private String signingAlgorithm;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Verify signature request")
    public static class VerifyRequest {
        @Schema(description = "Key ID", required = true)
        private String keyId;

        @Schema(description = "Original message (base64‑encoded)", required = true)
        private String message;

        @Schema(description = "Message type")
        private String messageType;

        @Schema(description = "Signature to verify (base64‑encoded)", required = true)
        private String signature;

        @Schema(description = "Signing algorithm", required = true)
        private String signingAlgorithm;

        @Schema(description = "Grant tokens")
        private List<String> grantTokens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Verify signature response")
    public static class VerifyResponse {
        @Schema(description = "Whether signature is valid")
        private boolean valid;

        @Schema(description = "Signature validity (alias)")
        private Boolean signatureValid;

        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "Signing algorithm used")
        private String signingAlgorithm;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Generate MAC request")
    public static class GenerateMacRequest {
        @Schema(description = "Key ID", required = true)
        private String keyId;

        @Schema(description = "Message (base64‑encoded)", required = true)
        private String message;

        @Schema(description = "MAC algorithm (e.g., HMAC_SHA_256)", required = true)
        private String macAlgorithm;

        @Schema(description = "Grant tokens")
        private List<String> grantTokens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Generate MAC response")
    public static class GenerateMacResponse {
        @Schema(description = "MAC (base64‑encoded)")
        private String mac;

        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "MAC algorithm used")
        private String macAlgorithm;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Verify MAC request")
    public static class VerifyMacRequest {
        @Schema(description = "Key ID", required = true)
        private String keyId;

        @Schema(description = "Original message (base64‑encoded)", required = true)
        private String message;

        @Schema(description = "MAC to verify (base64‑encoded)", required = true)
        private String mac;

        @Schema(description = "MAC algorithm", required = true)
        private String macAlgorithm;

        @Schema(description = "Grant tokens")
        private List<String> grantTokens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Verify MAC response")
    public static class VerifyMacResponse {
        @Schema(description = "Whether MAC is valid")
        private Boolean macValid;

        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "MAC algorithm used")
        private String macAlgorithm;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Get public key response")
    public static class GetPublicKeyResponse {
        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "Public key (base64‑encoded DER/PEM)")
        private String publicKey;

        @Schema(description = "Key specification")
        private IEnumKeySpec.Types customerMasterKeySpec;

        @Schema(description = "Key usage")
        private IEnumKeyUsage.Types keyUsage;

        @Schema(description = "Supported encryption algorithms")
        private List<String> encryptionAlgorithmSpecs;

        @Schema(description = "Supported signing algorithms")
        private List<String> signingAlgorithms;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Generate random bytes request")
    public static class GenerateRandomRequest {
        @Schema(description = "Number of random bytes (1‑1024)", required = true)
        private Integer numberOfBytes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Generate random bytes response")
    public static class GenerateRandomResponse {
        @Schema(description = "Random bytes (base64‑encoded)")
        private String plaintext;
    }

    // =========================================================================
    // Aliases
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Create alias request")
    public static class CreateAliasRequest {
        @NotBlank
        @Pattern(regexp = "^alias:.*", message = "alias.name.must.start.with.alias")
        @Schema(description = "Alias name (must start with 'alias:')", required = true)
        private String aliasName;

        @NotNull
        @Schema(description = "Target key ID", required = true)
        private String targetKeyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Create alias response")
    public static class CreateAliasResponse {
        @Schema(description = "Key ID")
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Update alias request")
    public static class UpdateAliasRequest {
        @NotBlank
        @Schema(description = "New target key ID", required = true)
        private String targetKeyId;

        @NotBlank
        @Schema(description = "Alias name", required = true)
        private String aliasName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Update alias response")
    public static class UpdateAliasResponse {
        @Schema(description = "Key ID")
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Delete alias request")
    public static class DeleteAliasRequest {
        @Schema(description = "Alias name", required = true)
        private String aliasName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Delete alias response")
    public static class DeleteAliasResponse {
        @Schema(description = "Key ID")
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "List aliases response")
    public static class ListAliasesResponse {
        @Schema(description = "List of alias entries")
        private List<AliasEntry> aliases;

        @Schema(description = "Pagination token")
        private String nextToken;

        @Schema(description = "Whether truncated")
        private Boolean truncated;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "Single alias entry")
        public static class AliasEntry {
            @Schema(description = "Alias name")
            private String aliasName;

            @Schema(description = "Alias ARN")
            private String aliasWrn;

            @Schema(description = "Target key ID")
            private String targetKeyId;

            @Schema(description = "Creation date")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            private String createDate;

            @Schema(description = "Last updated date")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            private String lastUpdatedDate;
        }
    }

    // =========================================================================
    // Tags
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "List tags response")
    public static class ListResourceTagsResponse {
        @Schema(description = "List of tags")
        private List<Tag> tags;

        @Schema(description = "Pagination token")
        private String nextToken;

        @Schema(description = "Whether truncated")
        private Boolean truncated;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "Tag key‑value pair")
        public static class Tag {
            @Schema(description = "Tag key")
            private String tagKey;

            @Schema(description = "Tag value")
            private String tagValue;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Tag resource request")
    public static class TagResourceRequest {
        @Schema(description = "Key ID", required = true)
        private String keyId;

        @Schema(description = "List of tags to add/update", required = true)
        private List<ListResourceTagsResponse.Tag> tags;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Tag resource response")
    public static class TagResourceResponse {
        @Schema(description = "Key ID")
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Untag resource request")
    public static class UntagResourceRequest {
        @Schema(description = "Key ID")
        private String keyId;

        @NotEmpty
        @Schema(description = "Tag keys to remove", required = true)
        private List<String> tagKeys;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Untag resource response")
    public static class UntagResourceResponse {
        @Schema(description = "Key ID")
        private String keyId;
    }

    // =========================================================================
    // Key Policies
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Put key policy request")
    public static class PutKeyPolicyRequest {
        @Schema(description = "Key ID", required = true)
        private String keyId;

        @NotNull
        @Schema(description = "Policy document as JSON object", required = true)
        private Map<String, Object> policy;

        @JsonProperty("BypassPolicyLockoutSafetyCheck")
        @Schema(description = "Bypass safety checks (use with caution)")
        private Boolean bypassPolicyLockoutSafetyCheck;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Put key policy response")
    public static class PutKeyPolicyResponse {
        @Schema(description = "Key ID")
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Get key policy response")
    public static class GetKeyPolicyResponse {
        @Schema(description = "Policy document as JSON string")
        private Map<String, Object> policy;
    }

    // =========================================================================
    // Grants
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Create grant request")
    public static class CreateGrantRequest {
        @Schema(description = "Key ID", required = true)
        private String keyId;

        @Schema(description = "Grantee principal (WRN or account ID)", required = true)
        private String granteePrincipal;

        @Schema(description = "Principal that can retire the grant (optional)")
        private String retiringPrincipal;

        @NotEmpty
        @Schema(description = "List of allowed operations", required = true)
        private List<String> operations;

        @Schema(description = "Grant constraints (encryption context conditions)")
        private GrantConstraints constraints;

        @Schema(description = "Optional friendly name")
        private String name;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "Grant constraints")
        public static class GrantConstraints {
            @Schema(description = "Encryption context subset condition")
            private Map<String, String> encryptionContextSubset;

            @Schema(description = "Encryption context equals condition")
            private Map<String, String> encryptionContextEquals;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Create grant response")
    public static class CreateGrantResponse {
        @Schema(description = "Grant ID")
        private String grantId;

        @Schema(description = "Grant token (opaque)")
        private String grantToken;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "List grants response")
    public static class ListGrantsResponse {
        @Schema(description = "List of grants")
        private List<Grant> grants;

        @Schema(description = "Pagination token")
        private String nextToken;

        @Schema(description = "Whether truncated")
        private Boolean truncated;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "Grant entry")
        public static class Grant {
            @Schema(description = "Grant ID")
            private String grantId;

            @Schema(description = "Grantee principal")
            private String granteePrincipal;

            @Schema(description = "Retiring principal")
            private String retiringPrincipal;

            @Schema(description = "Operations allowed")
            private List<String> operations;

            @Schema(description = "Constraints")
            private CreateGrantRequest.GrantConstraints constraints;

            @Schema(description = "Creation date")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            private LocalDateTime createDate;

            @Schema(description = "Last updated date")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            private String lastUpdatedDate;

            @Schema(description = "Key ID")
            private String keyId;

            @Schema(description = "Friendly name")
            private String name;

            @Schema(description = "Revocation date (if revoked)")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            private LocalDateTime revocationDate;

            @Schema(description = "Retirement date (if retired)")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            private LocalDateTime retirementDate;

            @Schema(description = "Status (ACTIVE, REVOKED, RETIRED)")
            private String status;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Revoke grant request")
    public static class RevokeGrantRequest {
        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "Grant ID")
        private String grantId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Revoke grant response")
    public static class RevokeGrantResponse {
        @Schema(description = "Key ID")
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Retire grant request")
    public static class RetireGrantRequest {
        @Schema(description = "Grant token (alternative to keyId+grantId)")
        private String grantToken;

        @Schema(description = "Key ID (if using keyId+grantId)")
        private String keyId;

        @Schema(description = "Grant ID (if using keyId+grantId)")
        private String grantId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Retire grant response")
    public static class RetireGrantResponse {
        @Schema(description = "Key ID")
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "List retirable grants response")
    public static class ListRetirableGrantsResponse {
        @Schema(description = "List of grants")
        private List<ListGrantsResponse.Grant> grants;

        @Schema(description = "Pagination token")
        private String nextToken;

        @Schema(description = "Whether truncated")
        private Boolean truncated;
    }

    // =========================================================================
    // BYOK (Key Material Import)
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Get parameters for import request")
    public static class GetParametersForImportRequest {
        @Schema(description = "Wrapping algorithm", required = true, example = "RSAES_OAEP_SHA_256")
        private String wrappingAlgorithm;

        @Schema(description = "Wrapping key spec", required = true, example = "RSA_2048")
        private String wrappingKeySpec;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Get parameters for import response")
    public static class GetParametersForImportResponse {
        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "Import token (base64‑encoded)")
        private String importToken;

        @Schema(description = "Public wrapping key (base64‑encoded)")
        private String publicKey;

        @Schema(description = "Parameter expiration timestamp")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime validTo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Import key material request")
    public static class ImportKeyMaterialRequest {
        @Schema(description = "Key ID", required = true)
        private String keyId;

        @Schema(description = "Import token (base64‑encoded)", required = true)
        private String importToken;

        @Schema(description = "Encrypted key material (base64‑encoded)", required = true)
        private String encryptedKeyMaterial;

        @Schema(description = "Expiration date for imported material")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime validTo;

        @Schema(description = "Expiration model")
        private IEnumKeyExpirationModel.Types expirationModel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Import key material response")
    public static class ImportKeyMaterialResponse {
        @Schema(description = "Key ID")
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Delete imported key material response")
    public static class DeleteImportedKeyMaterialResponse {
        @Schema(description = "Key ID")
        private String keyId;
    }

    // =========================================================================
    // Multi‑Region
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Update primary region request")
    public static class UpdatePrimaryRegionRequest {
        @NotBlank
        @Schema(description = "New primary region", required = true)
        private String primaryRegion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Update primary region response")
    public static class UpdatePrimaryRegionResponse {
        @Schema(description = "Key ID")
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Replicate key request")
    public static class ReplicateKeyRequest {
        @NotBlank
        @Schema(description = "Replica region", required = true)
        private String replicaRegion;

        @Schema(description = "Description for replica")
        private String description;

        @Schema(description = "Whether replica should be enabled")
        private Boolean enabled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Replicate key response")
    public static class ReplicateKeyResponse {
        @Schema(description = "Replica key metadata")
        private CreateKeyResponse.KeyMetadata replicaKeyMetadata;

        @Schema(description = "Replica region")
        private String replicaRegion;

        @Schema(description = "Replication status")
        private String replicationStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Synchronize multi‑region key response")
    public static class SynchronizeMultiRegionKeyResponse {
        @Schema(description = "Key ID")
        private String keyId;
    }

    // =========================================================================
    // Audit & Usage
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Audit log response")
    public static class AuditLogResponse {
        @Schema(description = "List of audit entries")
        private List<LogEntry> logs;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "Single audit log entry")
        public static class LogEntry {
            @Schema(description = "Timestamp of the operation")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            private LocalDateTime timestamp;

            @Schema(description = "Action performed")
            private String action;

            @Schema(description = "Key ID affected")
            private String keyId;

            @Schema(description = "Principal that performed the action")
            private String principal;

            @Schema(description = "Client IP address")
            private String ipAddress;

            @Schema(description = "Result status (SUCCESS/FAILURE)")
            private String status;

            @Schema(description = "Error message if failed")
            private String errorMessage;

            @Schema(description = "Execution time in milliseconds")
            private Long executionTimeMs;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Key usage statistics response")
    public static class KeyUsageStatsResponse {
        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "Number of encrypt operations")
        private Long encryptCount;

        @Schema(description = "Number of decrypt operations")
        private Long decryptCount;

        @Schema(description = "Number of sign operations")
        private Long signCount;

        @Schema(description = "Number of verify operations")
        private Long verifyCount;

        @Schema(description = "Last used timestamp")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime lastUsedDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Key validation response")
    public static class ValidateKeyResponse {
        @Schema(description = "Whether the key is valid")
        private Boolean valid;
    }

    // =========================================================================
    // Custom Key Stores
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ValidCreateCustomKeyStoreRequest
    @Schema(description = "Request to create a custom key store (CloudHSM or XKS)")
    public static class CreateCustomKeyStoreRequest {
        @NotBlank
        @Size(max = 255)
        @Schema(description = "Unique name of the custom key store", required = true)
        private String customKeyStoreName;

        @Schema(description = "Type of custom key store", required = true)
        private IEnumCustomKeyStoreType.Types customKeyStoreType;

        // ========== CloudHSM specific fields ==========
        @Schema(description = "CloudHSM cluster ID (required for CLOUDHSM type)")
        private String cloudHsmClusterId;

        @Schema(description = "Password for CloudHSM cluster authentication (will be hashed)")
        private String keyStorePassword;

        @Schema(description = "Trust anchor certificate (PEM format) – required for CLOUDHSM type")
        private String trustAnchorCertificate;

        // ========== XKS (external key store) specific fields ==========
        @Schema(description = "XKS proxy URI endpoint (required for EXTERNAL_KEY_STORE type)")
        private String xksProxyUriEndpoint;

        @Schema(description = "XKS proxy URI path (optional)")
        private String xksProxyUriPath;

        @Schema(description = "Authentication credential for XKS proxy (will be hashed)")
        private String xksProxyAuthenticationCredential;

        @Schema(description = "XKS proxy connectivity type (e.g., PUBLIC_ENDPOINT, VPC_ENDPOINT_SERVICE)")
        private String xksProxyConnectivity;

        // ========== Common configuration ==========
        @Min(1)
        @Max(10000)
        @Schema(description = "Maximum number of keys allowed in this custom key store", example = "1000")
        private Integer maxKeys;

        @Schema(description = "Custom metadata as key‑value pairs", example = "{\"environment\": \"production\", \"team\": \"security\"}")
        private Map<String, String> metadata;

        @Schema(description = "Tags for cost allocation and organization", example = "{\"Project\": \"PCI\", \"CostCenter\": \"12345\"}")
        private Map<String, String> tags;

        // ========== Connection settings ==========
        @Min(1)
        @Schema(description = "Connection timeout in seconds")
        private Integer connectionTimeoutSeconds;

        @Min(10)
        @Schema(description = "Health check interval in seconds")
        private Integer healthCheckIntervalSeconds;

        @Schema(description = "Whether to auto‑reconnect on failure")
        private Boolean autoReconnect;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Create custom key store response")
    public static class CreateCustomKeyStoreResponse {
        @Schema(description = "Custom key store ID")
        private Long customKeyStoreId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Describe custom key store response")
    public static class DescribeCustomKeyStoreResponse {
        @Schema(description = "Custom key store details")
        private CustomKeyStore customKeyStore;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "Custom key store details")
        public static class CustomKeyStore {
            @Schema(description = "Store ID")
            private Long customKeyStoreId;

            @Schema(description = "Store name")
            private String customKeyStoreName;

            @Schema(description = "Creation date")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            private LocalDateTime createDate;

            @Schema(description = "Connection state (CONNECTED, DISCONNECTED, FAILED)")
            private String connectionState;

            @Schema(description = "Connection error code")
            private String connectionErrorCode;

            @Schema(description = "CloudHSM cluster ID (if applicable)")
            private String cloudHsmClusterId;

            @Schema(description = "Trust anchor certificate")
            private String trustAnchorCertificate;

            @Schema(description = "Custom key store type")
            private String customKeyStoreType;

            @Schema(description = "XKS proxy URI endpoint")
            private String xksProxyUriEndpoint;

            @Schema(description = "XKS proxy URI path")
            private String xksProxyUriPath;

            @Schema(description = "XKS proxy authentication credential")
            private String xksProxyAuthenticationCredential;

            @Schema(description = "XKS proxy connectivity")
            private String xksProxyConnectivity;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request to update a custom key store")
    public static class UpdateCustomKeyStoreRequest {
        @Schema(description = "Numeric ID of the custom key store", required = true)
        private Long customKeyStoreId;

        @Size(max = 255)
        @Schema(description = "New name for the custom key store")
        private String newCustomKeyStoreName;

        // ========== CloudHSM update fields ==========
        @Schema(description = "New CloudHSM cluster ID")
        private String cloudHsmClusterId;

        @Schema(description = "New password for CloudHSM cluster (will be hashed)")
        private String keyStorePassword;

        @Schema(description = "New trust anchor certificate (PEM format)")
        private String trustAnchorCertificate;

        // ========== XKS update fields ==========
        @Schema(description = "New XKS proxy URI endpoint")
        private String xksProxyUriEndpoint;

        @Schema(description = "New XKS proxy URI path")
        private String xksProxyUriPath;

        @Schema(description = "New authentication credential for XKS proxy (will be hashed)")
        private String xksProxyAuthenticationCredential;

        @Schema(description = "New XKS proxy connectivity type")
        private String xksProxyConnectivity;

        // ========== Common update fields ==========
        @Schema(description = "Enable or disable the custom key store")
        private Boolean enabled;

        @Min(1)
        @Max(10000)
        @Schema(description = "New maximum number of keys allowed")
        private Integer maxKeys;

        @Schema(description = "New custom metadata (replaces existing)")
        private Map<String, String> metadata;

        @Schema(description = "New tags (replaces existing)")
        private Map<String, String> tags;

        // ========== Connection settings ==========
        @Min(1)
        @Schema(description = "New connection timeout in seconds")
        private Integer connectionTimeoutSeconds;

        @Min(10)
        @Schema(description = "New health check interval in seconds")
        private Integer healthCheckIntervalSeconds;

        @Schema(description = "New auto‑reconnect setting")
        private Boolean autoReconnect;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Update custom key store response")
    public static class UpdateCustomKeyStoreResponse {
        @Schema(description = "Key ID (or confirmation)")
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Delete custom key store response")
    public static class DeleteCustomKeyStoreResponse {
        @Schema(description = "Key ID")
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Connect custom key store response")
    public static class ConnectCustomKeyStoreResponse {
        @Schema(description = "Key ID")
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Disconnect custom key store response")
    public static class DisconnectCustomKeyStoreResponse {
        @Schema(description = "Key ID")
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "List custom key stores response")
    public static class ListCustomKeyStoresResponse {
        @Schema(description = "List of custom key stores")
        private List<DescribeCustomKeyStoreResponse.CustomKeyStore> customKeyStores;

        @Schema(description = "Pagination token")
        private String nextToken;

        @Schema(description = "Whether truncated")
        private Boolean truncated;
    }

    // =========================================================================
    // Additional DTOs (for completeness)
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Grant response (simple)")
    public static class GrantResponseDto {
        @Schema(description = "Grant ID")
        private String grantId;

        @Schema(description = "Grant token")
        private String grantToken;

        @Schema(description = "Key ID")
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "List grants response DTO")
    public static class ListGrantsResponseDto {
        @Schema(description = "List of grants")
        private List<GrantDto> grants;

        @Schema(description = "Pagination token")
        private String nextToken;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "Grant DTO")
        public static class GrantDto {
            @Schema(description = "Grant ID")
            private String grantId;

            @Schema(description = "Grantee principal")
            private String granteePrincipal;

            @Schema(description = "Retiring principal")
            private String retiringPrincipal;

            @Schema(description = "Operations")
            private List<String> operations;

            @Schema(description = "Constraints (JSON)")
            private String constraints;

            @Schema(description = "Creation date")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            private LocalDateTime createDate;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "List keys response DTO (basic summary)")
    public static class ListKeysResponseDto {
        @Schema(description = "List of key summaries")
        private List<KeySummaryDto> keys;

        @Schema(description = "Pagination token")
        private String nextToken;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "Key summary")
        public static class KeySummaryDto {
            @Schema(description = "Key ID")
            private String keyId;

            @Schema(description = "Primary alias")
            private String alias;

            @Schema(description = "Key status")
            private IEnumKeyStatus.Types status;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Import parameters response DTO")
    public static class ImportParametersResponseDto {
        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "Key ARN")
        private String keyWrn;

        @Schema(description = "Wrapping key (public key)")
        private byte[] wrappingKey;

        @Schema(description = "Import token")
        private byte[] importToken;

        @Schema(description = "Wrapping algorithm")
        private String wrappingAlgorithm;

        @Schema(description = "Validity period in hours")
        private Integer validityPeriodHours;

        @Schema(description = "Expiration timestamp")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime validTo;

        @Schema(description = "Parameters generation timestamp")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime parametersGeneratedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "List aliases response DTO")
    public static class ListAliasesResponseDto {
        @Schema(description = "List of aliases")
        private List<AliasResponseDto> aliases;

        @Schema(description = "Pagination token")
        private String nextToken;

        @Schema(description = "Whether truncated")
        private Boolean truncated;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "List custom key stores response DTO")
    public static class ListCustomKeyStoresResponseDto {
        @Schema(description = "List of custom key stores")
        private List<CustomKeyStoreResponseDto> customKeyStores;

        @Schema(description = "Pagination token")
        private String nextToken;

        @Schema(description = "Whether truncated")
        private boolean truncated;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagDto {
        private String tagKey;
        private String tagValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "List tags response DTO")
    public static class ListTagsResponseDto {
        @Schema(description = "List of tags")
        private List<TagDto> tags;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Key description response DTO")
    public static class KeyDescriptionResponseDto {
        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "Key status")
        private IEnumKeyStatus.Types status;

        @Schema(description = "Key specification")
        private IEnumKeySpec.Types keySpec;

        @Schema(description = "Key usage")
        private IEnumKeyUsage.Types keyUsage;

        @Schema(description = "Current version ID")
        private String currentVersion;

        @Schema(description = "Creation timestamp")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        @Schema(description = "Primary alias")
        private String alias;

        @Schema(description = "Description")
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Custom key store response DTO")
    public static class CustomKeyStoreResponseDto {
        @Schema(description = "Last successful connection timestamp")
        private LocalDateTime lastSuccessfulConnection;

        @Schema(description = "Key store ID")
        private Long keyStoreId;

        @Schema(description = "Key store name")
        private String customKeyStoreName;

        @Schema(description = "Key store type")
        private IEnumCustomKeyStoreType.Types type;

        @Schema(description = "Key store status")
        private IEnumCustomKeyStoreStatus.Types status;

        @Schema(description = "Connection state")
        private String connectionState;

        @Schema(description = "Endpoint")
        private String endpoint;

        @Schema(description = "Vendor")
        private String vendor;

        @Schema(description = "Region")
        private String region;

        @Schema(description = "Whether connected")
        private Boolean connected;

        @Schema(description = "Creation timestamp")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        @Schema(description = "Last update timestamp")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime updatedAt;

        @Schema(description = "CloudHSM cluster ID (if applicable)")
        private String cloudHsmClusterId;

        @Schema(description = "XKS proxy URI endpoint")
        private String xksProxyUriEndpoint;

        @Schema(description = "XKS proxy URI path")
        private String xksProxyUriPath;

        @Schema(description = "Additional configuration")
        private Map<String, String> configuration;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Audit log response DTO")
    public static class AuditLogResponseDto {
        @Schema(description = "List of audit entries")
        private List<AuditLogEntryDto> logs;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "Audit log entry DTO")
        public static class AuditLogEntryDto {
            @Schema(description = "Action performed")
            private String action;

            @Schema(description = "Key ID")
            private String keyId;

            @Schema(description = "Timestamp")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            private LocalDateTime timestamp;

            @Schema(description = "Principal")
            private String principal;

            @Schema(description = "IP address")
            private String ipAddress;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Alias response DTO")
    public static class AliasResponseDto {
        @Schema(description = "Alias name")
        private String aliasName;

        @Schema(description = "Target key ID")
        private String targetKeyId;

        @Schema(description = "Target key ARN")
        private String targetKeyWrn;

        @Schema(description = "Creation timestamp")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createDate;

        @Schema(description = "Last update timestamp")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime updateDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Set key policy request DTO")
    public static class SetKeyPolicyRequestDto {
        @NotNull
        @Schema(description = "Policy document", required = true)
        private Map<String, Object> policy;

        @JsonProperty("BypassPolicyLockoutSafetyCheck")
        @Schema(description = "Bypass safety checks")
        private Boolean bypassPolicyLockoutSafetyCheck;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Untag resource request DTO")
    public static class UntagResourceRequestDto {
        @NotEmpty
        @Schema(description = "Tag keys to remove", required = true)
        private List<String> tagKeys;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Update primary region request DTO")
    public static class UpdatePrimaryRegionRequestDto {
        @Schema(description = "Key ID")
        private String keyId;

        @NotBlank
        @Schema(description = "New primary region", required = true)
        private String primaryRegion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Tag resource request DTO")
    public static class TagResourceRequestDto {
        @NotEmpty
        @Schema(description = "Tags as map", required = true)
        private Map<String, String> tags;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Replicate key request DTO")
    public static class ReplicateKeyRequestDto {
        @NotBlank
        @Schema(description = "Key ID", required = true)
        private String keyId;

        @NotBlank
        @Schema(description = "Replica region", required = true)
        private String replicaRegion;

        @Schema(description = "Description for replica")
        private String description;

        @Schema(description = "Whether replica should be enabled")
        private Boolean enabled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Create grant request DTO")
    public static class CreateGrantRequestDto {
        @NotBlank
        @Schema(description = "Principal", required = true)
        private String principal;

        @Schema(description = "Grantee principal")
        private String granteePrincipal;

        @NotEmpty
        @Schema(description = "Operations", required = true)
        private List<String> operations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "List aliases request")
    public static class ListAliasesRequest {
        @Schema(description = "Max results")
        private Integer limit;

        @Schema(description = "Pagination token")
        private String nextToken;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "List aliases for key request")
    public static class ListAliasesForKeyRequest {
        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "Max results")
        private Integer limit;

        @Schema(description = "Pagination token")
        private String nextToken;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Key usage stats response DTO")
    public static class KeyUsageStatsResponseDto {
        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "Key ARN")
        private String keyWrn;

        @Schema(description = "Encrypt count")
        private Long encryptCount;

        @Schema(description = "Decrypt count")
        private Long decryptCount;

        @Schema(description = "Generate data key count")
        private Long generateDataKeyCount;

        @Schema(description = "Re‑encrypt count")
        private Long reEncryptCount;

        @Schema(description = "Sign count")
        private Long signCount;

        @Schema(description = "Verify count")
        private Long verifyCount;

        @Schema(description = "Last used date")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime lastUsedDate;

        @Schema(description = "First used date")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime firstUsedDate;

        @Schema(description = "Average operations per day")
        private Double averageOpsPerDay;

        @Schema(description = "Usage by operation")
        private Map<String, Long> usageByOperation;

        @Schema(description = "Hourly distribution")
        private Map<Integer, Long> hourlyDistribution;

        @Schema(description = "Daily distribution")
        private Map<String, Long> dailyDistribution;

        @Schema(description = "Usage tracking enabled")
        private Boolean usageTrackingEnabled;

        @Schema(description = "Tracking start date")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime trackingStartDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "List key rotations request DTO")
    public static class ListKeyRotationsRequestDto {
        @Schema(description = "Key ID")
        private String keyId;

        @Schema(description = "Max results")
        private Integer limit;

        @Schema(description = "Pagination token")
        private String nextToken;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Active version request DTO")
    public static class ActiveVersionRequestDto {
        @Schema(description = "Key ID")
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Validate key request")
    public static class ValidateKeyRequest {
        @Schema(description = "Key ID")
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Key usage stats request")
    public static class KeyUsageStatsRequest {
        @Schema(description = "Key ID")
        private String keyId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Error response")
    public static class KmsErrorResponse {
        @Schema(description = "Error type (e.g., NotFoundException)")
        private String __type;

        @Schema(description = "Error message")
        private String message;
    }
}