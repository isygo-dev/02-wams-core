package eu.isygoit.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO representing the response from a Generate MAC operation.
 * Contains the computed message authentication code (MAC) for data integrity verification.
 * MACs are faster than signatures and suitable for integrity checking.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        title = "Generate MAC Response",
        description = "Response containing a generated message authentication code (MAC)"
)
public class GenerateMacResponseDto {

    /**
     * The generated MAC value.
     * This is Base64-encoded and can be stored with the data for later verification.
     */
    @JsonProperty("mac")
    @Schema(
            description = "The generated message authentication code (Base64-encoded)",
            example = "AQIDAHhz+FZo2i8...",
            required = true
    )
    private String mac;

    /**
     * The MAC algorithm that was used to generate the MAC.
     */
    @JsonProperty("macAlgorithm")
    @Schema(
            description = "The MAC algorithm used (e.g., HMAC_SHA_256, HMAC_SHA_384, HMAC_SHA_512)",
            example = "HMAC_SHA_256",
            required = true,
            allowableValues = {
                    "HMAC_SHA_224", "HMAC_SHA_256", "HMAC_SHA_384", "HMAC_SHA_512"
            }
    )
    private String macAlgorithm;

    /**
     * The ID of the KMS key used to generate the MAC.
     */
    @JsonProperty("keyId")
    @Schema(
            description = "The ID of the KMS key used to generate the MAC",
            example = "550e8400-e29b-41d4-a716-446655440000",
            required = true
    )
    private Long keyId;

    /**
     * The ARN of the KMS key used to generate the MAC.
     */
    @JsonProperty("keyArn")
    @Schema(
            description = "The Amazon Resource Name (ARN) of the KMS key",
            example = "arn:aws:kms:us-east-1:123456789012:key/550e8400-e29b-41d4-a716-446655440000",
            required = true
    )
    private String keyArn;

    /**
     * The version of the KMS key used to generate the MAC.
     */
    @JsonProperty("keyVersion")
    @Schema(
            description = "The version of the KMS key used to generate the MAC",
            example = "2",
            required = true
    )
    private String keyVersion;

    /**
     * The size of the generated MAC in bytes.
     */
    @JsonProperty("macLength")
    @Schema(
            description = "The size of the generated MAC in bytes",
            example = "32",
            required = true
    )
    private Integer macLength;

    /**
     * The size of the input message that was authenticated.
     */
    @JsonProperty("messageLength")
    @Schema(
            description = "The size of the message that was authenticated in bytes",
            example = "1024",
            required = true
    )
    private Long messageLength;

    /**
     * The timestamp when the MAC was generated.
     */
    @JsonProperty("generationTimestamp")
    @Schema(
            description = "The date and time the MAC was generated (ISO 8601 format)",
            example = "2024-01-15T10:30:45.123Z",
            required = true
    )
    private LocalDateTime generationTimestamp;

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
     * MAC context keys used in the operation (if any).
     * Lists the keys from the MAC context.
     */
    @JsonProperty("macContextKeys")
    @Schema(
            description = "List of keys from the MAC context (values are not returned)",
            example = "[\"Department\", \"Environment\"]",
            nullable = true
    )
    private java.util.List<String> macContextKeys;

    /**
     * Whether the MAC was generated with a signing context.
     */
    @JsonProperty("hasContext")
    @Schema(
            description = "Whether the MAC was generated with a context",
            example = "false",
            required = true
    )
    private Boolean hasContext;

    /**
     * The hash algorithm family used by the MAC.
     */
    @JsonProperty("hashAlgorithmFamily")
    @Schema(
            description = "The cryptographic hash algorithm family (SHA-256, SHA-384, SHA-512)",
            example = "SHA-256",
            required = true
    )
    private String hashAlgorithmFamily;

    /**
     * Whether the MAC is suitable for high-security use cases.
     * Depends on the MAC algorithm strength.
     */
    @JsonProperty("isHighSecurity")
    @Schema(
            description = "Whether this MAC is suitable for high-security integrity verification",
            example = "true",
            required = true
    )
    private Boolean isHighSecurity;

    /**
     * The recommended usage for this MAC.
     */
    @JsonProperty("recommendedUsage")
    @Schema(
            description = "Recommended use cases for this MAC (database integrity, message authentication, etc.)",
            example = "Database integrity verification, message authentication",
            nullable = true
    )
    private String recommendedUsage;

    /**
     * The TTL (time to live) recommendation for storing this MAC.
     * In days. Null if no specific recommendation.
     */
    @JsonProperty("ttlRecommendationDays")
    @Schema(
            description = "Recommended time in days to keep the MAC and data together",
            example = "90",
            nullable = true
    )
    private Integer ttlRecommendationDays;

    /**
     * Performance metrics - time taken to generate MAC.
     */
    @JsonProperty("generationTimeMs")
    @Schema(
            description = "Time taken to generate the MAC in milliseconds (for monitoring)",
            example = "25",
            nullable = true
    )
    private Long generationTimeMs;

    /**
     * Whether this MAC should be rotated after N days.
     */
    @JsonProperty("requiresRotation")
    @Schema(
            description = "Whether this MAC should be regenerated periodically for security",
            example = "false",
            required = true
    )
    private Boolean requiresRotation;

    /**
     * The region where the MAC was generated.
     */
    @JsonProperty("region")
    @Schema(
            description = "The AWS region where the MAC was generated",
            example = "us-east-1",
            required = true
    )
    private String region;

    /**
     * Determines the effective MAC length based on algorithm.
     */
    public int getEffectiveMacLength() {
        if (macAlgorithm.contains("256")) return 32;
        if (macAlgorithm.contains("384")) return 48;
        if (macAlgorithm.contains("512")) return 64;
        if (macAlgorithm.contains("224")) return 28;
        return macLength;
    }

    /**
     * Determines if the MAC is a strong cryptographic MAC.
     */
    public boolean isStrongMac() {
        return macAlgorithm.contains("256") || macAlgorithm.contains("384") || macAlgorithm.contains("512");
    }

    /**
     * Gets the bit length of the MAC.
     */
    public int getMacBitLength() {
        return macLength * 8;
    }

    /**
     * Provides guidance on MAC storage.
     */
    public String getStorageGuidance() {
        if (isHighSecurity) {
            return "Store MAC with encrypted data. Verify on retrieval. High security use case." ;
        } else {
            return "Store MAC alongside data. Verify periodically. Suitable for general integrity checking." ;
        }
    }

    /**
     * Gets a summary of the MAC generation operation.
     */
    public String getSummary() {
        return String.format(
                "Generated %s-bit %s using key version %s. Message size: %d bytes. Hash family: %s",
                getMacBitLength(), macAlgorithm, keyVersion, messageLength, hashAlgorithmFamily
        );
    }

    /**
     * Validates the response has all required fields.
     */
    public void validate() {
        if (mac == null || mac.isEmpty()) {
            throw new IllegalStateException("MAC value is missing");
        }
        if (macAlgorithm == null || macAlgorithm.isEmpty()) {
            throw new IllegalStateException("MAC algorithm is missing");
        }
        if (keyId == null) {
            throw new IllegalStateException("Key ID is missing");
        }
        if (macLength == null || macLength <= 0) {
            throw new IllegalStateException("MAC length is invalid");
        }
        if (generationTimestamp == null) {
            throw new IllegalStateException("Generation timestamp is missing");
        }
    }
}