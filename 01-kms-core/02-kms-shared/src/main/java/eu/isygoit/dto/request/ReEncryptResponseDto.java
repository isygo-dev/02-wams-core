package eu.isygoit.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO representing the response from a re-encryption operation.
 * Contains the re-encrypted ciphertext and metadata about the operation.
 * The plaintext is never exposed during re-encryption.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        title = "ReEncrypt Response",
        description = "Response from re-encrypting data under a different key"
)
public class ReEncryptResponseDto {

    /**
     * The re-encrypted ciphertext.
     * This is Base64-encoded data encrypted under the destination key.
     */
    @JsonProperty("ciphertext")
    @Schema(
            description = "The re-encrypted ciphertext blob (Base64-encoded)",
            example = "AQIDAHhz+FZo2i8...",
            required = true
    )
    private String ciphertext;

    /**
     * The ID of the destination key used for re-encryption.
     */
    @JsonProperty("destinationKeyId")
    @Schema(
            description = "The ID of the key used to encrypt the plaintext in the response",
            example = "550e8400-e29b-41d4-a716-446655440000",
            required = true
    )
    private String destinationKeyId;

    /**
     * The ARN of the destination key.
     */
    @JsonProperty("destinationKeyArn")
    @Schema(
            description = "The Amazon Resource Name (ARN) of the destination key",
            example = "arn:aws:kms:us-east-1:123456789012:key/550e8400-e29b-41d4-a716-446655440000",
            required = true
    )
    private String destinationKeyArn;

    /**
     * The version of the destination key that was used for encryption.
     */
    @JsonProperty("destinationKeyVersion")
    @Schema(
            description = "The version of the destination key used for encryption",
            example = "2",
            required = true
    )
    private String destinationKeyVersion;

    /**
     * The ID of the source key that was used to decrypt the original ciphertext.
     */
    @JsonProperty("sourceKeyId")
    @Schema(
            description = "The ID of the key that was used to decrypt the original ciphertext",
            example = "a1b2c3d4-e5f6-4a8b-9c0d-e1f2a3b4c5d6",
            required = true
    )
    private String sourceKeyId;

    /**
     * The ARN of the source key.
     */
    @JsonProperty("sourceKeyArn")
    @Schema(
            description = "The Amazon Resource Name (ARN) of the source key",
            example = "arn:aws:kms:us-east-1:123456789012:key/a1b2c3d4-e5f6-4a8b-9c0d-e1f2a3b4c5d6",
            required = true
    )
    private String sourceKeyArn;

    /**
     * The version of the source key that was used to decrypt the original ciphertext.
     */
    @JsonProperty("sourceKeyVersion")
    @Schema(
            description = "The version of the source key used to decrypt the original ciphertext",
            example = "1",
            required = true
    )
    private String sourceKeyVersion;

    /**
     * The timestamp when the re-encryption was performed.
     */
    @JsonProperty("encryptionTimestamp")
    @Schema(
            description = "The date and time the re-encryption was performed (ISO 8601 format)",
            example = "2024-01-15T10:30:45.123Z",
            required = true
    )
    private LocalDateTime encryptionTimestamp;

    /**
     * The ID of the request for auditing and tracking.
     */
    @JsonProperty("requestId")
    @Schema(
            description = "Unique request identifier for tracking and audit purposes",
            example = "a1b2c3d4-e5f6-4a8b-9c0d-e1f2a3b4c5d6",
            required = true
    )
    private String requestId;

    /**
     * Encryption context keys used in the operation.
     * Lists the keys that were included in the encryption context.
     */
    @JsonProperty("encryptionContextKeys")
    @Schema(
            description = "List of keys from the encryption context (values are not returned)",
            example = "[\"Department\", \"Environment\"]",
            nullable = true
    )
    private java.util.List<String> encryptionContextKeys;

    /**
     * The size of the ciphertext blob in bytes.
     */
    @JsonProperty("ciphertextLength")
    @Schema(
            description = "The size of the ciphertext blob in bytes",
            example = "1024",
            required = true
    )
    private Long ciphertextLength;

    /**
     * Whether the source and destination keys are in the same region.
     */
    @JsonProperty("sameRegion")
    @Schema(
            description = "Whether the source and destination keys are in the same AWS region",
            example = "true",
            required = true
    )
    private Boolean sameRegion;

    /**
     * The destination region (if different from source).
     */
    @JsonProperty("destinationRegion")
    @Schema(
            description = "The AWS region of the destination key",
            example = "us-east-1",
            nullable = true
    )
    private String destinationRegion;

    /**
     * The source region.
     */
    @JsonProperty("sourceRegion")
    @Schema(
            description = "The AWS region of the source key",
            example = "us-west-2",
            nullable = true
    )
    private String sourceRegion;

    /**
     * Whether this re-encryption crossed account boundaries.
     */
    @JsonProperty("crossAccountOperation")
    @Schema(
            description = "Whether the re-encryption involved keys in different AWS accounts",
            example = "false",
            required = true
    )
    private Boolean crossAccountOperation;

    /**
     * The source account ID (if cross-account).
     */
    @JsonProperty("sourceAccountId")
    @Schema(
            description = "The AWS account ID containing the source key",
            example = "123456789012",
            nullable = true
    )
    private String sourceAccountId;

    /**
     * The destination account ID (if cross-account).
     */
    @JsonProperty("destinationAccountId")
    @Schema(
            description = "The AWS account ID containing the destination key",
            example = "210987654321",
            nullable = true
    )
    private String destinationAccountId;

    /**
     * Algorithm used for the re-encryption operation.
     */
    @JsonProperty("encryptionAlgorithm")
    @Schema(
            description = "The encryption algorithm used (e.g., SYMMETRIC_DEFAULT for KMS keys)",
            example = "SYMMETRIC_DEFAULT",
            required = true
    )
    private String encryptionAlgorithm;

    /**
     * Determines if this was a key rotation re-encryption.
     * Returns true if source and destination have same base key ID.
     */
    public boolean isKeyRotationReEncryption() {
        return sourceKeyId.equals(destinationKeyId);
    }

    /**
     * Determines if this was a key replacement re-encryption.
     * Returns true if source and destination are different keys.
     */
    public boolean isKeyReplacementReEncryption() {
        return !sourceKeyId.equals(destinationKeyId);
    }

    /**
     * Gets a summary description of the re-encryption operation.
     */
    public String getSummary() {
        if (isKeyRotationReEncryption()) {
            return String.format(
                    "Re-encrypted data using new version of key %s (v%s -> v%s)",
                    sourceKeyId, sourceKeyVersion, destinationKeyVersion
            );
        } else {
            return String.format(
                    "Re-encrypted data from key %s to key %s",
                    sourceKeyId, destinationKeyId
            );
        }
    }
}