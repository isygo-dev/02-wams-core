package eu.isygoit.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO representing a generated asymmetric key pair (public and private keys).
 * Contains both plaintext and encrypted versions of the keys.
 * The public key can be freely distributed; the private key must be protected.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        title = "Data Key Pair Response",
        description = "Response containing a generated asymmetric public/private key pair"
)
public class DataKeyPairResponseDto {

    /**
     * The plaintext public key in Base64 encoding.
     * Can be freely distributed.
     */
    @JsonProperty("publicKey")
    @Schema(
            description = "The plaintext public key (Base64-encoded PEM format)",
            example = "MIIBIjANBgkqhkiG9w0BA...",
            required = true
    )
    private String publicKey;

    /**
     * The plaintext private key in Base64 encoding.
     * Must be protected and stored securely.
     * Only available in GenerateDataKeyPair response, not in WithoutPlaintext variant.
     */
    @JsonProperty("privateKey")
    @Schema(
            description = "The plaintext private key (Base64-encoded PEM format). Not present if GenerateDataKeyPairWithoutPlaintext was used.",
            example = "MIIEowIBAAKCAQEA...",
            nullable = true
    )
    private String privateKey;

    /**
     * The encrypted public key.
     * Can be used to recover the public key if encrypted storage is needed.
     */
    @JsonProperty("encryptedPublicKey")
    @Schema(
            description = "The public key encrypted under the KMS key (Base64-encoded)",
            example = "AQIDAHhz+FZo2i8...",
            required = true
    )
    private String encryptedPublicKey;

    /**
     * The encrypted private key.
     * Must be decrypted using the KMS key before use.
     */
    @JsonProperty("encryptedPrivateKey")
    @Schema(
            description = "The private key encrypted under the KMS key (Base64-encoded)",
            example = "AQIDAHhz+FZo2i8...",
            required = true
    )
    private String encryptedPrivateKey;

    /**
     * The ID of the KMS key used to encrypt the key pair.
     */
    @JsonProperty("keyId")
    @Schema(
            description = "The ID of the KMS key used to encrypt the key pair",
            example = "550e8400-e29b-41d4-a716-446655440000",
            required = true
    )
    private Long keyId;

    /**
     * The ARN of the KMS key used to encrypt the key pair.
     */
    @JsonProperty("keyArn")
    @Schema(
            description = "The Amazon Resource Name (ARN) of the KMS key",
            example = "arn:aws:kms:us-east-1:123456789012:key/550e8400-e29b-41d4-a716-446655440000",
            required = true
    )
    private String keyArn;

    /**
     * The version of the KMS key used for encryption.
     */
    @JsonProperty("keyVersion")
    @Schema(
            description = "The version of the KMS key used to encrypt the keys",
            example = "2",
            required = true
    )
    private String keyVersion;

    /**
     * The type of key pair (RSA_2048, RSA_3072, RSA_4096, ECC_NIST_P256, ECC_NIST_P384, ECC_NIST_P521, SM2).
     */
    @JsonProperty("keySpec")
    @Schema(
            description = "The type and size of the key pair generated",
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
     * The signing algorithm(s) supported by this key pair.
     */
    @JsonProperty("signingAlgorithms")
    @Schema(
            description = "The signing algorithms supported by this key pair (e.g., RSASSA_PSS_SHA_256)",
            example = "[\"RSASSA_PSS_SHA_256\", \"RSASSA_PKCS1_V1_5_SHA_256\"]",
            required = true
    )
    private java.util.List<String> signingAlgorithms;

    /**
     * The encryption algorithms supported by this key pair.
     */
    @JsonProperty("encryptionAlgorithms")
    @Schema(
            description = "The encryption algorithms supported by this key pair (e.g., RSAES_OAEP_SHA_256)",
            example = "[\"RSAES_OAEP_SHA_256\"]",
            required = true
    )
    private java.util.List<String> encryptionAlgorithms;

    /**
     * The timestamp when the key pair was generated.
     */
    @JsonProperty("generationTimestamp")
    @Schema(
            description = "The date and time the key pair was generated (ISO 8601 format)",
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
     * Whether this is without plaintext (encrypted only).
     */
    @JsonProperty("withoutPlaintext")
    @Schema(
            description = "Whether plaintext keys are included (false) or only encrypted versions (true)",
            example = "false",
            required = true
    )
    private Boolean withoutPlaintext;

    /**
     * The size of the public key in bits.
     */
    @JsonProperty("publicKeySize")
    @Schema(
            description = "The size of the public key in bits",
            example = "2048",
            required = true
    )
    private Integer publicKeySize;

    /**
     * The size of the encrypted public key blob in bytes.
     */
    @JsonProperty("encryptedPublicKeyLength")
    @Schema(
            description = "The size of the encrypted public key blob in bytes",
            example = "2048",
            required = true
    )
    private Long encryptedPublicKeyLength;

    /**
     * The size of the encrypted private key blob in bytes.
     */
    @JsonProperty("encryptedPrivateKeyLength")
    @Schema(
            description = "The size of the encrypted private key blob in bytes",
            example = "4096",
            required = true
    )
    private Long encryptedPrivateKeyLength;

    /**
     * The usage flags for the key pair.
     */
    @JsonProperty("keyUsage")
    @Schema(
            description = "The intended use of the key pair (SIGN_VERIFY or ENCRYPT_DECRYPT)",
            example = "SIGN_VERIFY",
            required = true,
            allowableValues = {"SIGN_VERIFY", "ENCRYPT_DECRYPT"}
    )
    private String keyUsage;

    /**
     * Whether plaintext keys are available in this response.
     */
    public boolean hasPlaintextKeys() {
        return privateKey != null && !privateKey.isEmpty();
    }

    /**
     * Determines if this key pair is suitable for signing operations.
     */
    public boolean isForSigning() {
        return "SIGN_VERIFY".equalsIgnoreCase(keyUsage);
    }

    /**
     * Determines if this key pair is suitable for encryption operations.
     */
    public boolean isForEncryption() {
        return "ENCRYPT_DECRYPT".equalsIgnoreCase(keyUsage);
    }

    /**
     * Gets the key family (RSA, ECC, or SM2).
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
     * Gets a summary of the generated key pair.
     */
    public String getSummary() {
        String keyMaterial = hasPlaintextKeys() ? "with plaintext" : "without plaintext" ;
        return String.format(
                "Generated %s key pair (%s) %s using KMS key version %s",
                keySpec, keyUsage, keyMaterial, keyVersion
        );
    }

    /**
     * Validates the key pair has required fields.
     */
    public void validate() {
        if (publicKey == null || publicKey.isEmpty()) {
            throw new IllegalStateException("Public key is missing");
        }
        if (encryptedPublicKey == null || encryptedPublicKey.isEmpty()) {
            throw new IllegalStateException("Encrypted public key is missing");
        }
        if (encryptedPrivateKey == null || encryptedPrivateKey.isEmpty()) {
            throw new IllegalStateException("Encrypted private key is missing");
        }
        if (!withoutPlaintext && (privateKey == null || privateKey.isEmpty())) {
            throw new IllegalStateException("Private key should be present when withoutPlaintext is false");
        }
    }
}