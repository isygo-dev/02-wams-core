package eu.isygoit.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO for requesting generation of a message authentication code (MAC).
 * MACs provide a fast method for verifying data integrity without asymmetric cryptography.
 * The MAC should be stored with the data and verified on retrieval.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        title = "Generate MAC Request",
        description = "Request payload for generating a message authentication code (MAC)"
)
public class GenerateMacRequestDto {

    /**
     * The ID or alias of the KMS key to use for MAC generation.
     */
    @JsonProperty("keyId")
    @Schema(
            description = "The ID or alias of the KMS key to use for generating the MAC",
            example = "alias/my-mac-key",
            required = true
    )
    private Long keyId;

    /**
     * The message data to authenticate.
     * Should be Base64-encoded.
     */
    @JsonProperty("message")
    @Schema(
            description = "The message data to authenticate (Base64-encoded)",
            example = "SGVsbG8gV29ybGQ=",
            required = true
    )
    private String message;

    /**
     * The MAC algorithm to use for generating the code.
     */
    @JsonProperty("macAlgorithm")
    @Schema(
            description = "The MAC algorithm to use (HMAC_SHA_224, HMAC_SHA_256, HMAC_SHA_384, HMAC_SHA_512)",
            example = "HMAC_SHA_256",
            required = true,
            allowableValues = {
                    "HMAC_SHA_224", "HMAC_SHA_256", "HMAC_SHA_384", "HMAC_SHA_512"
            }
    )
    private String macAlgorithm;

    /**
     * Optional context for additional security.
     * Must be provided to VerifyMac to verify the MAC.
     * Key-value pairs for binding MAC to specific conditions.
     */
    @JsonProperty("macContext")
    @Schema(
            description = "Optional key-value pairs for additional MAC context. Must be provided to VerifyMac.",
            example = "{\"Department\": \"Finance\", \"TransactionType\": \"Payment\"}",
            nullable = true
    )
    private Map<String, String> macContext;

    /**
     * Optional grant tokens for authorization.
     * Use if the request is made on behalf of a principal with limited permissions.
     */
    @JsonProperty("grantTokens")
    @Schema(
            description = "Optional grant tokens for request authorization",
            nullable = true
    )
    private java.util.List<String> grantTokens;

    /**
     * Optional idempotency token.
     * Ensures the same request generates the same MAC.
     */
    @JsonProperty("clientToken")
    @Schema(
            description = "Optional idempotency token to ensure idempotent requests",
            example = "a1b2c3d4-e5f6-4a8b-9c0d-e1f2a3b4c5d6",
            nullable = true,
            maxLength = 1024
    )
    private String clientToken;

    /**
     * Optional request metadata for tracking and auditing.
     */
    @JsonProperty("requestMetadata")
    @Schema(
            description = "Optional metadata about the request for auditing purposes",
            example = "{\"application\": \"payment-service\", \"version\": \"1.2.3\"}",
            nullable = true
    )
    private Map<String, String> requestMetadata;

    /**
     * Whether the message is already Base64-encoded.
     */
    @JsonProperty("messageIsBase64Encoded")
    @Schema(
            description = "Whether the message field is Base64-encoded (true) or raw text (false)",
            example = "true",
            defaultValue = "true"
    )
    private Boolean messageIsBase64Encoded;

    /**
     * Creates a builder for SHA-256 HMAC with default settings.
     */
    public static GenerateMacRequestDto forSha256(Long keyId, String message) {
        return GenerateMacRequestDto.builder()
                .keyId(keyId)
                .message(message)
                .macAlgorithm("HMAC_SHA_256")
                .messageIsBase64Encoded(true)
                .build();
    }

    /**
     * Creates a builder for SHA-256 HMAC with context.
     */
    public static GenerateMacRequestDto forSha256WithContext(Long keyId, String message, Map<String, String> context) {
        return GenerateMacRequestDto.builder()
                .keyId(keyId)
                .message(message)
                .macAlgorithm("HMAC_SHA_256")
                .macContext(context)
                .messageIsBase64Encoded(true)
                .build();
    }

    /**
     * Creates a builder for SHA-512 HMAC (highest security).
     */
    public static GenerateMacRequestDto forSha512(Long keyId, String message) {
        return GenerateMacRequestDto.builder()
                .keyId(keyId)
                .message(message)
                .macAlgorithm("HMAC_SHA_512")
                .messageIsBase64Encoded(true)
                .build();
    }

    /**
     * Creates a builder with custom configuration.
     */
    public static GenerateMacRequestDto custom(Long keyId, String message, String macAlgorithm) {
        return GenerateMacRequestDto.builder()
                .keyId(keyId)
                .message(message)
                .macAlgorithm(macAlgorithm)
                .messageIsBase64Encoded(true)
                .build();
    }

    /**
     * Creates a request from raw (non-Base64) message data.
     */
    public static GenerateMacRequestDto fromRawMessage(Long keyId, String rawMessage, String macAlgorithm) {
        String encoded = Base64.getEncoder().encodeToString(
                rawMessage.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
        return GenerateMacRequestDto.builder()
                .keyId(keyId)
                .message(encoded)
                .macAlgorithm(macAlgorithm)
                .messageIsBase64Encoded(true)
                .build();
    }

    /**
     * Validates the request payload.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (keyId == null) {
            throw new IllegalArgumentException("keyId is required");
        }

        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("message is required");
        }

        if (macAlgorithm == null || macAlgorithm.trim().isEmpty()) {
            throw new IllegalArgumentException("macAlgorithm is required");
        }

        // Validate macAlgorithm value
        String[] validAlgorithms = {
                "HMAC_SHA_224", "HMAC_SHA_256", "HMAC_SHA_384", "HMAC_SHA_512"
        };
        boolean validAlgorithm = false;
        for (String algo : validAlgorithms) {
            if (algo.equals(macAlgorithm)) {
                validAlgorithm = true;
                break;
            }
        }
        if (!validAlgorithm) {
            throw new IllegalArgumentException(
                    "Invalid macAlgorithm: " + macAlgorithm + ". Must be one of: " + java.util.Arrays.toString(validAlgorithms)
            );
        }

        if (macContext != null && macContext.isEmpty()) {
            throw new IllegalArgumentException("macContext cannot be empty map");
        }

        if (clientToken != null && clientToken.length() > 1024) {
            throw new IllegalArgumentException("clientToken exceeds maximum length of 1024 characters");
        }

        // Validate message is Base64 if specified
        if (Boolean.TRUE.equals(messageIsBase64Encoded)) {
            try {
                Base64.getDecoder().decode(message);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("message must be valid Base64-encoded data", e);
            }
        }
    }

    /**
     * Gets the hash strength level of the MAC algorithm.
     */
    public int getHashStrengthBits() {
        if (macAlgorithm.contains("256")) return 256;
        if (macAlgorithm.contains("384")) return 384;
        if (macAlgorithm.contains("512")) return 512;
        if (macAlgorithm.contains("224")) return 224;
        return 0;
    }

    /**
     * Determines if this is a strong security MAC (256+ bits).
     */
    public boolean isStrongSecurity() {
        return getHashStrengthBits() >= 256;
    }

    /**
     * Determines if MAC context is provided.
     */
    public boolean hasContext() {
        return macContext != null && !macContext.isEmpty();
    }

    /**
     * Adds a context entry.
     */
    public GenerateMacRequestDto withContextEntry(String key, String value) {
        if (this.macContext == null) {
            this.macContext = new HashMap<>();
        }
        this.macContext.put(key, value);
        return this;
    }

    /**
     * Decodes the message from Base64 if applicable.
     * Returns the raw message bytes.
     */
    public byte[] getMessageBytes() {
        if (Boolean.TRUE.equals(messageIsBase64Encoded)) {
            return Base64.getDecoder().decode(message);
        }
        return message.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Gets the message size in bytes.
     */
    public long getMessageSizeBytes() {
        return getMessageBytes().length;
    }

    /**
     * Gets a summary of the request.
     */
    public String getSummary() {
        String size = formatBytes(getMessageSizeBytes());
        String contextInfo = hasContext() ? String.format(" with %d context entries", macContext.size()) : "" ;
        return String.format(
                "Generate %s MAC for %s message%s using key %s",
                macAlgorithm, size, contextInfo, keyId
        );
    }

    /**
     * Formats bytes in human-readable format.
     */
    private String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B" ;
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}