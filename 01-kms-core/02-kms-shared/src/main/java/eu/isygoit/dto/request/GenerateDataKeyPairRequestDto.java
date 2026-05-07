package eu.isygoit.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO for requesting generation of an asymmetric data key pair (public/private keys).
 * The response includes both plaintext and encrypted versions of the keys.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        title = "Generate Data Key Pair Request",
        description = "Request payload for generating an asymmetric public/private key pair"
)
public class GenerateDataKeyPairRequestDto {

    /**
     * The ID or alias of the KMS key to use for encrypting the generated key pair.
     */
    @JsonProperty("keyId")
    @Schema(
            description = "The ID or alias of the KMS key to use for encrypting the key pair",
            example = "alias/my-encryption-key",
            required = true
    )
    private Long keyId;

    /**
     * The type and size of the key pair to generate.
     * Options: RSA_2048, RSA_3072, RSA_4096, ECC_NIST_P256, ECC_NIST_P384, ECC_NIST_P521, SM2
     */
    @JsonProperty("keySpec")
    @Schema(
            description = "The type and size of the key pair (RSA_2048, RSA_3072, RSA_4096, ECC_NIST_P256, ECC_NIST_P384, ECC_NIST_P521, SM2)",
            example = "RSA_2048",
            required = true,
            allowableValues = {
                    "RSA_2048", "RSA_3072", "RSA_4096",
                    "ECC_NIST_P256", "ECC_NIST_P384", "ECC_NIST_P521",
                    "SM2"
            }
    )
    private String keySpec;

    /**
     * Optional encryption context for additional encryption security.
     * Must match the context used when decrypting the encrypted keys.
     */
    @JsonProperty("encryptionContext")
    @Schema(
            description = "Optional key-value pairs for additional encryption context. Must be provided to decrypt keys.",
            example = "{\"Department\": \"IT\", \"Environment\": \"Production\"}",
            nullable = true
    )
    private Map<String, String> encryptionContext;

    /**
     * The intended use of the key pair.
     */
    @JsonProperty("keyUsage")
    @Schema(
            description = "Whether the key pair is for SIGN_VERIFY or ENCRYPT_DECRYPT operations",
            example = "SIGN_VERIFY",
            required = true,
            allowableValues = {"SIGN_VERIFY", "ENCRYPT_DECRYPT"}
    )
    private String keyUsage;

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
     * Ensures the same request generates the same key pair.
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
     * Creates a builder for RSA 2048 key pair for signing.
     */
    public static GenerateDataKeyPairRequestDto forRsaSigningKey(Long keyId) {
        return GenerateDataKeyPairRequestDto.builder()
                .keyId(keyId)
                .keySpec("RSA_2048")
                .keyUsage("SIGN_VERIFY")
                .build();
    }

    /**
     * Creates a builder for RSA 2048 key pair for encryption.
     */
    public static GenerateDataKeyPairRequestDto forRsaEncryptionKey(Long keyId) {
        return GenerateDataKeyPairRequestDto.builder()
                .keyId(keyId)
                .keySpec("RSA_2048")
                .keyUsage("ENCRYPT_DECRYPT")
                .build();
    }

    /**
     * Creates a builder for ECC P256 key pair for signing.
     */
    public static GenerateDataKeyPairRequestDto forEccSigningKey(Long keyId) {
        return GenerateDataKeyPairRequestDto.builder()
                .keyId(keyId)
                .keySpec("ECC_NIST_P256")
                .keyUsage("SIGN_VERIFY")
                .build();
    }

    /**
     * Creates a builder for custom configuration.
     */
    public static GenerateDataKeyPairRequestDto custom(Long keyId, String keySpec, String keyUsage) {
        return GenerateDataKeyPairRequestDto.builder()
                .keyId(keyId)
                .keySpec(keySpec)
                .keyUsage(keyUsage)
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

        if (keySpec == null || keySpec.trim().isEmpty()) {
            throw new IllegalArgumentException("keySpec is required");
        }

        // Validate keySpec value
        String[] validKeySpecs = {
                "RSA_2048", "RSA_3072", "RSA_4096",
                "ECC_NIST_P256", "ECC_NIST_P384", "ECC_NIST_P521",
                "SM2"
        };
        boolean validSpec = false;
        for (String spec : validKeySpecs) {
            if (spec.equals(keySpec)) {
                validSpec = true;
                break;
            }
        }
        if (!validSpec) {
            throw new IllegalArgumentException(
                    "Invalid keySpec: " + keySpec + ". Must be one of: " + java.util.Arrays.toString(validKeySpecs)
            );
        }

        if (keyUsage == null || keyUsage.trim().isEmpty()) {
            throw new IllegalArgumentException("keyUsage is required");
        }

        // Validate keyUsage value
        if (!keyUsage.equals("SIGN_VERIFY") && !keyUsage.equals("ENCRYPT_DECRYPT")) {
            throw new IllegalArgumentException(
                    "Invalid keyUsage: " + keyUsage + ". Must be SIGN_VERIFY or ENCRYPT_DECRYPT"
            );
        }

        if (encryptionContext != null && encryptionContext.isEmpty()) {
            throw new IllegalArgumentException("encryptionContext cannot be empty map");
        }

        if (clientToken != null && clientToken.length() > 1024) {
            throw new IllegalArgumentException("clientToken exceeds maximum length of 1024 characters");
        }
    }

    /**
     * Gets the key family from keySpec.
     */
    public String getKeyFamily() {
        if (keySpec.startsWith("RSA")) {
            return "RSA" ;
        } else if (keySpec.startsWith("ECC")) {
            return "ECC" ;
        } else if (keySpec.startsWith("SM2")) {
            return "SM2" ;
        }
        return "UNKNOWN" ;
    }

    /**
     * Gets the key size for RSA keys, returns null for ECC/SM2.
     */
    public Integer getKeySizeBits() {
        if (keySpec.equals("RSA_2048")) return 2048;
        if (keySpec.equals("RSA_3072")) return 3072;
        if (keySpec.equals("RSA_4096")) return 4096;
        if (keySpec.equals("ECC_NIST_P256")) return 256;
        if (keySpec.equals("ECC_NIST_P384")) return 384;
        if (keySpec.equals("ECC_NIST_P521")) return 521;
        if (keySpec.equals("SM2")) return 256;
        return null;
    }

    /**
     * Determines if this is an RSA key pair request.
     */
    public boolean isRsaKey() {
        return keySpec.startsWith("RSA");
    }

    /**
     * Determines if this is an ECC key pair request.
     */
    public boolean isEccKey() {
        return keySpec.startsWith("ECC");
    }

    /**
     * Determines if this is for signing operations.
     */
    public boolean isForSigning() {
        return "SIGN_VERIFY".equals(keyUsage);
    }

    /**
     * Determines if this is for encryption operations.
     */
    public boolean isForEncryption() {
        return "ENCRYPT_DECRYPT".equals(keyUsage);
    }

    /**
     * Adds an encryption context entry.
     */
    public GenerateDataKeyPairRequestDto withEncryptionContext(String key, String value) {
        if (this.encryptionContext == null) {
            this.encryptionContext = new HashMap<>();
        }
        this.encryptionContext.put(key, value);
        return this;
    }
}