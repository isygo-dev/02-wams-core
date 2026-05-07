package eu.isygoit.api;

import eu.isygoit.dto.request.*;
import eu.isygoit.dto.response.*;
import eu.isygoit.dto.response.DataKeyPairResponseDto;
import eu.isygoit.dto.response.GenerateMacResponseDto;
import eu.isygoit.dto.response.ImportParametersResponseDto;
import eu.isygoit.dto.response.KeyRotationStatusDto;
import eu.isygoit.dto.response.ListGrantsResponseDto;
import eu.isygoit.dto.response.ReEncryptResponseDto;
import eu.isygoit.enums.IEnumCharSet;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Key Management Service API
 * <p>
 * This interface provides REST APIs for managing
 * cryptographic keys and performing cryptographic operations. It supports:
 * </p>
 * <ul>
 *   <li>Symmetric and asymmetric key management (AES, RSA, ECC)</li>
 *   <li>Envelope encryption with data keys</li>
 *   <li>Digital signatures (RSA, ECDSA, HMAC)</li>
 *   <li>Key rotation (automatic and manual)</li>
 *   <li>Multi-region key replication</li>
 *   <li>Key policies, grants, and aliases</li>
 *   <li>Key material import (BYOK)</li>
 *   <li>Audit logging and usage statistics</li>
 * </ul>
 *
 * @author Isygoit Team
 * @version 1.0
 * @since 1.0
 */
@Tag(name = "KMS Service API",
        description = "key management and cryptographic operations. " +
                "Provides enterprise-grade key lifecycle management, encryption/decryption, " +
                "digital signatures, and envelope encryption capabilities.")
public interface KmsServiceApi {

    // ============================================================================
    // KEY MANAGEMENT APIs
    // ============================================================================

    /**
     * Creates a new customer managed key (CMK).
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * POST /keys
     * {
     *   "description": "Production encryption key",
     *   "keySpec": "SYMMETRIC_DEFAULT",
     *   "keyUsage": "ENCRYPT_DECRYPT",
     *   "origin": "WAMS_KMS",
     *   "tags": [{"tagKey": "Environment", "tagValue": "Production"}]
     * }
     * </pre>
     *
     * @param request The key creation parameters including key specification,
     *                usage type, origin, description, and optional tags
     * @return ResponseEntity containing the created key metadata including KeyId,
     * ARN, creation date, and current state
     * @throws IllegalArgumentException if keySpec or keyUsage are invalid
     * @throws IllegalStateException    if key limit exceeded
     */
    @PostMapping("/keys")
    @Operation(
            summary = "Create Key",
            description = "Creates a new customer managed key (CMK) with specified metadata, " +
                    "key material, and optional tags. Supports symmetric (AES-256) and " +
                    "asymmetric (RSA-2048/4096, ECC-NIST-P256/P384) keys. " +
                    "The key is immediately usable for cryptographic operations upon creation.",
            operationId = "createKey"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Key created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CreateKeyResponseDto.class),
                            examples = @ExampleObject(
                                    value = "{\"keyId\":\"1234abcd-12ab-34cd-56ef-1234567890ab\"," +
                                            "\"arn\":\"arn:wams:kms:us-east-1:123456789012:key/1234abcd-12ab-34cd-56ef-1234567890ab\"," +
                                            "\"creationDate\":\"2024-01-15T10:30:00Z\"," +
                                            "\"keyState\":\"Enabled\"}"
                            ))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters - check keySpec, keyUsage, or origin"),
            @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions"),
            @ApiResponse(responseCode = "409", description = "Conflict - alias already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error - see error message for details")
    })
    ResponseEntity<CreateKeyResponseDto> createKey(@RequestBody CreateKeyRequestDto request);

    /**
     * Retrieves detailed metadata about the specified key.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * GET /keys/1234abcd-12ab-34cd-56ef-1234567890ab
     * </pre>
     *
     * @param keyId The unique identifier of the key (UUID format or ARN)
     * @return ResponseEntity containing key metadata including ARN, creation date,
     * state, description, rotation status, and key policy
     */
    @GetMapping("/keys/{keyId}")
    @Operation(
            summary = "Describe Key",
            description = "Returns detailed metadata about the specified key including ARN, " +
                    "creation date, current state (Enabled/Disabled/PendingDeletion), " +
                    "key policy, rotation configuration, and multi-region information. " +
                    "Use this endpoint to inspect key properties before performing operations.",
            operationId = "describeKey"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Key metadata retrieved successfully",
                    content = @Content(schema = @Schema(implementation = KeyMetadataResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Key not found - the specified keyId does not exist"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<KeyMetadataResponseDto> describeKey(
            @Parameter(description = "Unique identifier of the KMS key (UUID format or ARN)",
                    required = true,
                    example = "1234abcd-12ab-34cd-56ef-1234567890ab")
            @PathVariable Long keyId);

    /**
     * Lists all customer managed keys in the account with pagination support.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * GET /keys?limit=50&nextToken=abc123def456
     * </pre>
     *
     * @param limit     Maximum number of keys to return (default: 100, max: 1000)
     * @param nextToken Pagination token for retrieving the next page of results
     * @return ResponseEntity containing list of key metadata and pagination token
     */
    @GetMapping("/keys")
    @Operation(
            summary = "List Keys",
            description = "Returns a paginated list of all customer managed keys in the account. " +
                    "Results include basic metadata (KeyId, ARN, state, creation date) but not " +
                    "full key details. Use DescribeKey for complete metadata. " +
                    "Results are sorted by creation date descending.",
            operationId = "listKeys"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Keys listed successfully",
                    content = @Content(schema = @Schema(implementation = ListKeysResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<ListKeysResponseDto> listKeys(
            @Parameter(description = "Maximum number of keys to return (1-1000, default: 100)",
                    example = "50")
            @RequestParam(required = false) Integer limit,

            @Parameter(description = "Pagination token from previous response for retrieving next page",
                    example = "eyJleGNsdXNpdmVTdGFydEtleSI6ImRlZmF1bHQtcmVnaW9uLWlkeDoyIn0=")
            @RequestParam(required = false) String nextToken);

    /**
     * Updates descriptive metadata for an existing key.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * PATCH /keys/1234abcd-12ab-34cd-56ef-1234567890ab/metadata
     * {
     *   "description": "Updated key description for production use",
     *   "displayName": "Production-Key-2024"
     * }
     * </pre>
     *
     * @param keyId   The unique identifier of the key
     * @param request Contains updated description and/or display name
     * @return ResponseEntity containing updated key metadata
     */
    @PatchMapping("/keys/{keyId}/metadata")
    @Operation(
            summary = "Update Key Metadata",
            description = "Updates descriptive metadata such as description and display name. " +
                    "This operation does not affect the key material or cryptographic operations. " +
                    "Changes are applied immediately and tracked in audit logs.",
            operationId = "updateKeyMetadata"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metadata updated successfully",
                    content = @Content(schema = @Schema(implementation = KeyMetadataResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<KeyMetadataResponseDto> updateKeyMetadata(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable Long keyId,

            @Parameter(description = "Updated metadata fields (description and/or displayName)",
                    required = true)
            @RequestBody UpdateKeyMetadataRequestDto request);

    /**
     * Enables a disabled KMS key, making it available for cryptographic operations.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * PATCH /keys/1234abcd-12ab-34cd-56ef-1234567890ab/enable
     * </pre>
     *
     * @param keyId The unique identifier of the key to enable
     * @return ResponseEntity containing updated key metadata with state "Enabled"
     */
    @PatchMapping("/keys/{keyId}/enable")
    @Operation(
            summary = "Enable Key",
            description = "Enables a disabled KMS key, allowing it to be used for cryptographic operations. " +
                    "Once enabled, the key immediately becomes available for encrypt, decrypt, sign, " +
                    "and verify operations. Cannot enable a key that is pending deletion.",
            operationId = "enableKey"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Key enabled successfully"),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "400", description = "Key is not disabled or is pending deletion"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    ResponseEntity<KeyMetadataResponseDto> enableKey(@PathVariable Long keyId);

    /**
     * Disables a KMS key, preventing all cryptographic operations.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * PATCH /keys/1234abcd-12ab-34cd-56ef-1234567890ab/disable
     * </pre>
     *
     * @param keyId The unique identifier of the key to disable
     * @return ResponseEntity containing updated key metadata with state "Disabled"
     */
    @PatchMapping("/keys/{keyId}/disable")
    @Operation(
            summary = "Disable Key",
            description = "Disables a KMS key, preventing all cryptographic operations including " +
                    "encrypt, decrypt, sign, verify, generate data key, and re-encrypt. " +
                    "The key metadata and aliases remain accessible. Use Enable Key to reactivate.",
            operationId = "disableKey"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Key disabled successfully"),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "400", description = "Key is already disabled or pending deletion"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    ResponseEntity<KeyMetadataResponseDto> disableKey(@PathVariable Long keyId);

    /**
     * Schedules a key for deletion with a configurable waiting period.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * DELETE /keys/1234abcd-12ab-34cd-56ef-1234567890ab?pendingWindowInDays=7
     * </pre>
     *
     * @param keyId               The unique identifier of the key to schedule for deletion
     * @param pendingWindowInDays Number of days to wait before deletion (7-30, default: 30)
     * @return ResponseEntity containing key metadata with state "PendingDeletion"
     */
    @DeleteMapping("/keys/{keyId}")
    @Operation(
            summary = "Schedule Key Deletion",
            description = "Schedules a key for deletion with a configurable pending window (grace period). " +
                    "During the pending window, the key cannot be used for cryptographic operations " +
                    "but can be canceled using Cancel Key Deletion. After the grace period ends, " +
                    "the key is permanently deleted and cannot be recovered. Minimum pending window: 7 days, " +
                    "Maximum: 30 days.",
            operationId = "scheduleKeyDeletion"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Key deletion scheduled successfully"),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "400", description = "Invalid pending window (must be 7-30 days)"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "409", description = "Key already pending deletion")
    })
    ResponseEntity<KeyMetadataResponseDto> scheduleKeyDeletion(
            @PathVariable Long keyId,

            @Parameter(description = "Number of days to wait before deletion (7-30, default: 30). " +
                    "Use 0 to cancel scheduled deletion.",
                    example = "7")
            @RequestParam(required = false) Integer pendingWindowInDays);

    /**
     * Cancels a previously scheduled key deletion.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * POST /keys/1234abcd-12ab-34cd-56ef-1234567890ab/cancel-deletion
     * </pre>
     *
     * @param keyId The unique identifier of the key with scheduled deletion
     * @return ResponseEntity containing key metadata with restored state "Disabled"
     */
    @PostMapping("/keys/{keyId}/cancel-deletion")
    @Operation(
            summary = "Cancel Key Deletion",
            description = "Cancels a previously scheduled key deletion, restoring the key to its " +
                    "previous state (Enabled or Disabled). Must be called before the pending " +
                    "window expires. Once the deletion window has passed, the key cannot be recovered.",
            operationId = "cancelKeyDeletion"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Key deletion canceled successfully"),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "400", description = "Key is not pending deletion"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    ResponseEntity<KeyMetadataResponseDto> cancelKeyDeletion(@PathVariable Long keyId);

    /**
     * Permanently deletes a key after the pending deletion window has expired.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * DELETE /keys/1234abcd-12ab-34cd-56ef-1234567890ab/delete
     * </pre>
     *
     * @param keyId The unique identifier of the key to permanently delete
     * @return ResponseEntity with empty body on success
     */
    @DeleteMapping("/keys/{keyId}/delete")
    @Operation(
            summary = "Delete Key",
            description = "Permanently deletes a key after the pending deletion window has expired. " +
                    "This operation is irreversible and should be used with extreme caution. " +
                    "All data encrypted under this key becomes permanently inaccessible.",
            operationId = "deleteKey",
            hidden = true
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Key deleted permanently"),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "400", description = "Key is not pending deletion or grace period not expired"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    ResponseEntity<?> deleteKey(@PathVariable Long keyId);

    // ============================================================================
    // KEY ROTATION
    // ============================================================================

    /**
     * Enables or disables automatic key rotation.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * PUT /keys/1234abcd-12ab-34cd-56ef-1234567890ab/rotation
     * {
     *   "enabled": true,
     *   "rotationPeriodInDays": 365
     * }
     * </pre>
     *
     * @param keyId   The unique identifier of the key
     * @param request Contains rotation configuration (enabled status and period)
     * @return ResponseEntity containing rotation status and next rotation date
     */
    @PutMapping("/keys/{keyId}/rotation")
    @Operation(
            summary = "Update Key Rotation",
            description = "Enables or disables automatic key rotation for symmetric keys. " +
                    "When enabled, KMS automatically generates new cryptographic material " +
                    "annually (or custom period) while preserving old versions for decryption. " +
                    "Rotation does not affect existing encrypted data and is recommended for " +
                    "compliance (PCI-DSS, HIPAA, SOC2). Asymmetric keys cannot be rotated automatically.",
            operationId = "updateKeyRotation"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rotation configuration updated"),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "400", description = "Automatic rotation not supported for asymmetric keys"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    ResponseEntity<KeyRotationStatusDto> updateKeyRotation(
            @PathVariable Long keyId,
            @RequestBody UpdateKeyRotationRequestDto request);

    /**
     * Manually rotates a key, creating a new key version immediately.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * POST /keys/1234abcd-12ab-34cd-56ef-1234567890ab/rotate
     * </pre>
     *
     * @param keyId The unique identifier of the key to rotate
     * @return ResponseEntity containing new key version information
     */
    @PostMapping("/keys/{keyId}/rotate")
    @Operation(
            summary = "Rotate Key",
            description = "Creates a new key version immediately (manual rotation). " +
                    "Useful for compliance requirements or security incidents. " +
                    "The new version becomes the primary key for encryption, while old versions " +
                    "remain available for decrypting existing data. Supports both symmetric and " +
                    "asymmetric keys.",
            operationId = "rotateKey"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Key rotated successfully"),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "409", description = "Rotation already in progress")
    })
    ResponseEntity<RotateKeyResponseDto> rotateKey(@PathVariable Long keyId);

    /**
     * Retrieves the current rotation configuration for a key.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * GET /keys/1234abcd-12ab-34cd-56ef-1234567890ab/rotation
     * </pre>
     *
     * @param keyId The unique identifier of the key
     * @return ResponseEntity containing rotation status (enabled/disabled, last rotation date, next rotation date)
     */
    @GetMapping("/keys/{keyId}/rotation")
    @Operation(
            summary = "Get Key Rotation Status",
            description = "Returns the rotation configuration and last rotation date for the specified key. " +
                    "For keys with automatic rotation enabled, also returns the next scheduled rotation date. " +
                    "Use this information for compliance auditing and key lifecycle management.",
            operationId = "getKeyRotationStatus"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rotation status retrieved"),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<KeyRotationStatusDto> getKeyRotationStatus(@PathVariable Long keyId);

    /**
     * Lists the complete history of all key rotations for the specified key.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * GET /keys/1234abcd-12ab-34cd-56ef-1234567890ab/rotations?limit=20
     * </pre>
     *
     * @param keyId     The unique identifier of the key
     * @param limit     Maximum number of rotation records to return
     * @param nextToken Pagination token for next page
     * @return ResponseEntity containing list of rotation records with timestamps and version IDs
     */
    @GetMapping("/keys/{keyId}/rotations")
    @Operation(
            summary = "List Key Rotations",
            description = "Returns the complete history of all key rotations (both automatic and manual). " +
                    "Each rotation record includes the rotation timestamp, key version ID, and rotation type. " +
                    "Essential for security auditing and compliance reporting.",
            operationId = "listKeyRotations"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rotations listed successfully"),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<ListKeyRotationsResponseDto> listKeyRotations(
            @PathVariable Long keyId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String nextToken);

    // ============================================================================
    // CRYPTOGRAPHIC OPERATIONS
    // ============================================================================

    /**
     * Encrypts plaintext using the specified KMS key.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * POST /encrypt
     * {
     *   "keyId": "1234abcd-12ab-34cd-56ef-1234567890ab",
     *   "plaintext": "SGVsbG8gV29ybGQ=",
     *   "encryptionContext": {
     *     "service": "payment-processor",
     *     "environment": "production"
     *   }
     * }
     * </pre>
     *
     * @param request Contains key ID, base64-encoded plaintext, and optional encryption context
     * @return ResponseEntity containing base64-encoded ciphertext and metadata
     */
    @PostMapping("/encrypt")
    @Operation(
            summary = "Encrypt",
            description = "Encrypts plaintext using the specified symmetric or asymmetric KMS key. " +
                    "For symmetric keys, returns ciphertext that includes the key ID and encryption algorithm. " +
                    "For asymmetric keys (RSA), uses the public key. Encryption context provides " +
                    "additional authenticated data (AAD) that must be provided during decryption. " +
                    "Maximum plaintext size: 4096 bytes (use data keys for larger data).",
            operationId = "encrypt"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Encryption successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request - plaintext too large or invalid keyId"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "409", description = "Key disabled or pending deletion")
    })
    ResponseEntity<EncryptResponseDto> encrypt(@RequestBody EncryptRequestDto request);

    /**
     * Decrypts ciphertext and returns the original plaintext.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * POST /decrypt
     * {
     *   "ciphertextBlob": "AQIDAHh....",
     *   "encryptionContext": {
     *     "service": "payment-processor"
     *   }
     * }
     * </pre>
     *
     * @param request Contains base64-encoded ciphertext and optional encryption context
     * @return ResponseEntity containing base64-encoded plaintext and key metadata
     */
    @PostMapping("/decrypt")
    @Operation(
            summary = "Decrypt",
            description = "Decrypts ciphertext and returns the original plaintext. " +
                    "Automatically detects the correct key and algorithm from the ciphertext. " +
                    "Encryption context must match exactly what was used during encryption. " +
                    "Returns the key ID used for decryption and information about key version. " +
                    "Works with both symmetric and asymmetric (RSA private key) keys.",
            operationId = "decrypt"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Decryption successful"),
            @ApiResponse(responseCode = "400", description = "Invalid ciphertext or encryption context mismatch"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Key not found or corrupted ciphertext"),
            @ApiResponse(responseCode = "409", description = "Key disabled or pending deletion")
    })
    ResponseEntity<DecryptResponseDto> decrypt(@RequestBody DecryptRequestDto request);

    /**
     * Re-encrypts data from one key to another without exposing plaintext.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * POST /reencrypt
     * {
     *   "ciphertextBlob": "AQIDAHh....",
     *   "destinationKeyId": "5678efgh-56ef-78gh-90ij-1234567890ab",
     *   "sourceEncryptionContext": {...},
     *   "destinationEncryptionContext": {...}
     * }
     * </pre>
     *
     * @param request Contains source ciphertext, destination key ID, and optional contexts
     * @return ResponseEntity containing new ciphertext under destination key
     */
    @PostMapping("/reencrypt")
    @Operation(
            summary = "Re-Encrypt",
            description = "Decrypts ciphertext under one key and re-encrypts it under another key or key version " +
                    "without exposing the plaintext in the response. Ideal for key rotation and migration scenarios. " +
                    "The server performs both operations in memory, ensuring data never leaves KMS in plaintext. " +
                    "Supports different encryption contexts for source and destination.",
            operationId = "reEncrypt"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Re-encryption successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "403", description = "Access denied to source or destination key"),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "409", description = "Key disabled or pending deletion")
    })
    ResponseEntity<ReEncryptResponseDto> reEncrypt(@RequestBody ReEncryptRequestDto request);

    /**
     * Generates a data key for envelope encryption.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * POST /datakey/generate
     * {
     *   "keyId": "1234abcd-12ab-34cd-56ef-1234567890ab",
     *   "keySpec": "AES_256",
     *   "encryptionContext": {...}
     * }
     * </pre>
     *
     * @param request Contains CMK ID, data key specification, and optional context
     * @return ResponseEntity containing plaintext data key and encrypted data key
     */
    @PostMapping("/datakey/generate")
    @Operation(
            summary = "Generate Data Key",
            description = "Generates a unique data key for envelope encryption. Returns both the plaintext data key " +
                    "for local encryption and the encrypted data key for storage. Ideal for encrypting large " +
                    "data objects (files, database records, messages). The data key can be used for client-side " +
                    "encryption and is discarded after use. Supports AES_128, AES_256, and various HMAC specs. " +
                    "Use this for encrypting data larger than 4KB.",
            operationId = "generateDataKey"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Data key generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid keySpec or keyId"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Key not found")
    })
    ResponseEntity<DataKeyResponseDto> generateDataKey(@RequestBody GenerateDataKeyRequestDto request);

    /**
     * Generates a data key and returns only the encrypted version.
     *
     * @param request Contains CMK ID and data key specification
     * @return ResponseEntity containing only the encrypted data key (no plaintext)
     * @see #generateDataKey(GenerateDataKeyRequestDto)
     */
    @PostMapping("/datakey/generate-without-plaintext")
    @Operation(
            summary = "Generate Data Key Without Plaintext",
            description = "Generates a data key but returns only the encrypted version, not the plaintext. " +
                    "Useful when the plaintext key is not needed immediately or for stricter security " +
                    "postures where plaintext keys should not transit the network. The plaintext key " +
                    "is still generated but immediately discarded after encryption.",
            operationId = "generateDataKeyWithoutPlaintext"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Encrypted data key generated"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    ResponseEntity<DataKeyResponseDto> generateDataKeyWithoutPlaintext(@RequestBody GenerateDataKeyRequestDto request);

    /**
     * Generates an asymmetric data key pair for public-key cryptography.
     *
     * @param request Contains CMK ID, key pair specification (RSA, ECC)
     * @return ResponseEntity containing public key, private key, and encrypted private key
     */
    @PostMapping("/datakey/generate-pair")
    @Operation(
            summary = "Generate Data Key Pair",
            description = "Generates an asymmetric data key pair (public and private keys) for digital signatures " +
                    "and public-key encryption without requiring a persistent KMS key. Returns the private key " +
                    "plaintext, encrypted private key (for secure storage), and the public key. Supports RSA " +
                    "and ECC key types. The key pair is ephemeral - suitable for session-based cryptography.",
            operationId = "generateDataKeyPair"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Key pair generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid key pair specification"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    ResponseEntity<DataKeyPairResponseDto> generateDataKeyPair(@RequestBody GenerateDataKeyPairRequestDto request);

    /**
     * Generates an asymmetric key pair and returns only encrypted private key.
     *
     * @param request Contains CMK ID and key pair specification
     * @return ResponseEntity containing public key and encrypted private key only
     */
    @PostMapping("/datakey/generate-pair-without-plaintext")
    @Operation(
            summary = "Generate Data Key Pair Without Plaintext",
            description = "Generates an asymmetric data key pair but returns only the public key and encrypted " +
                    "private key. The plaintext private key is never exposed over the network, enhancing " +
                    "security for sensitive operations. The private key must be decrypted using the KMS key " +
                    "when needed for signing or decryption.",
            operationId = "generateDataKeyPairWithoutPlaintext"
    )
    ResponseEntity<DataKeyPairResponseDto> generateDataKeyPairWithoutPlaintext(@RequestBody GenerateDataKeyPairRequestDto request);

    /**
     * Generates a digital signature using an asymmetric KMS key.
     *
     * @param request Contains key ID, message, and signing algorithm
     * @return ResponseEntity containing the digital signature
     */
    @PostMapping("/sign")
    @Operation(
            summary = "Sign",
            description = "Generates a digital signature using an asymmetric KMS key (private key). " +
                    "Supports RSA-PSS, RSA-PKCS#1-v1.5, and ECDSA signature algorithms. " +
                    "The signature can be verified using the corresponding public key. " +
                    "Essential for code signing, document authentication, and JWT signing. " +
                    "Maximum message size: 4096 bytes (use message digest for larger data).",
            operationId = "sign"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Signature generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid message or algorithm"),
            @ApiResponse(responseCode = "403", description = "Access denied - key not usable for signing"),
            @ApiResponse(responseCode = "404", description = "Key not found")
    })
    ResponseEntity<SignResponseDto> sign(@RequestBody SignRequestDto request);

    /**
     * Verifies a digital signature using an asymmetric KMS key.
     *
     * @param request Contains key ID, message, signature, and algorithm
     * @return ResponseEntity containing verification result (valid/invalid)
     */
    @PostMapping("/verify")
    @Operation(
            summary = "Verify Signature",
            description = "Verifies a digital signature using the public key of an asymmetric KMS key. " +
                    "Returns a boolean indicating whether the signature is valid for the given message and algorithm. " +
                    "Can verify signatures generated by the Sign operation or external signatures. " +
                    "Critical for document validation, code verification, and authenticity checks.",
            operationId = "verify"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification result returned"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "403", description = "Access denied - key not usable for verification"),
            @ApiResponse(responseCode = "404", description = "Key not found")
    })
    ResponseEntity<VerifyResponseDto> verify(@RequestBody VerifyRequestDto request);

    /**
     * Generates a Message Authentication Code (MAC) using HMAC key.
     *
     * @param request Contains key ID, message, and MAC algorithm
     * @return ResponseEntity containing the generated MAC
     */
    @PostMapping("/mac/generate")
    @Operation(
            summary = "Generate MAC",
            description = "Generates a Message Authentication Code (MAC) using a symmetric HMAC KMS key. " +
                    "Supports HMAC_SHA_224, HMAC_SHA_256, HMAC_SHA_384, and HMAC_SHA_512. " +
                    "MACs provide data integrity and authenticity without non-repudiation. " +
                    "Faster than digital signatures and suitable for high-throughput scenarios. " +
                    "Common use cases: API request authentication, data integrity checks, " +
                    "session token validation, and message queue authentication.",
            operationId = "generateMac"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "MAC generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid message or algorithm"),
            @ApiResponse(responseCode = "403", description = "Access denied - key not usable for MAC generation"),
            @ApiResponse(responseCode = "404", description = "Key not found")
    })
    ResponseEntity<GenerateMacResponseDto> generateMac(@RequestBody GenerateMacRequestDto request);

    /**
     * Verifies a Message Authentication Code (MAC).
     *
     * @param request Contains key ID, message, MAC, and algorithm
     * @return ResponseEntity containing verification result
     */
    @PostMapping("/mac/verify")
    @Operation(
            summary = "Verify MAC",
            description = "Verifies a Message Authentication Code (MAC) generated by the GenerateMAC operation. " +
                    "Returns a boolean indicating whether the MAC is valid for the given message and key. " +
                    "Essential for API request validation, data integrity verification, and " +
                    "preventing tampering in distributed systems.",
            operationId = "verifyMac"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "MAC verification result returned"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Key not found")
    })
    ResponseEntity<VerifyMacResponseDto> verifyMac(@RequestBody VerifyMacRequestDto request);

    /**
     * Retrieves the public key of an asymmetric KMS key.
     *
     * @param keyId The unique identifier of the asymmetric key
     * @return ResponseEntity containing the public key in PEM or DER format
     */
    @GetMapping("/keys/{keyId}/public-key")
    @Operation(
            summary = "Get Public Key",
            description = "Returns the public key of an asymmetric KMS key in PEM (default) or DER format. " +
                    "The public key can be distributed freely for encryption and signature verification. " +
                    "Only works with asymmetric keys (RSA or ECC). Symmetric keys do not have public keys. " +
                    "Use this for client-side encryption or integration with external systems that cannot use KMS.",
            operationId = "getPublicKey"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Public key retrieved"),
            @ApiResponse(responseCode = "404", description = "Key not found or is symmetric"),
            @ApiResponse(responseCode = "403", description = "Access denied - need kms:GetPublicKey permission"),
            @ApiResponse(responseCode = "400", description = "Asymmetric key not found or not supported")
    })
    ResponseEntity<PublicKeyResponseDto> getPublicKey(@PathVariable Long keyId);

    // ============================================================================
    // KEY VERSIONING & MULTI-REGION
    // ============================================================================

    /**
     * Lists all versions of a multi-region or rotated key.
     *
     * @param keyId The unique identifier of the key
     * @return ResponseEntity containing list of key versions with metadata
     */
    @GetMapping("/keys/{keyId}/versions")
    @Operation(
            summary = "List Key Versions",
            description = "Lists all versions of a key that has been rotated or replicated. " +
                    "Each version represents a distinct cryptographic material instance. " +
                    "For multi-region keys, versions are region-specific. Key versions are immutable " +
                    "and persist even after newer versions exist. Essential for auditing and " +
                    "understanding which key version encrypted specific data.",
            operationId = "listKeyVersions"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Key versions listed"),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<KeyVersionListResponseDto> listKeyVersions(@PathVariable Long keyId);

    /**
     * Retrieves the currently active (primary) version of a key.
     *
     * @param keyId The unique identifier of the key
     * @return ResponseEntity containing the active key version ID and metadata
     */
    @GetMapping("/keys/{keyId}/active-version")
    @Operation(
            summary = "Get Active Version",
            description = "Returns the currently active version of a key, which is used for all new " +
                    "encryption and signing operations. Older versions are preserved only for " +
                    "decrypting existing data. This information is important for understanding " +
                    "which key material is currently in use.",
            operationId = "getActiveVersion"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Active version retrieved"),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<ActiveVersionResponseDto> getActiveVersion(@PathVariable Long keyId);

    /**
     * Updates the primary region for a multi-region key.
     *
     * @param keyId   The unique identifier of the key
     * @param request Contains new primary region specification
     * @return ResponseEntity containing updated key metadata
     */
    @PutMapping("/keys/{keyId}/primary-region")
    @Operation(
            summary = "Update Primary Region",
            description = "Updates the primary region for a multi-region key. The primary region is " +
                    "the source of truth for key material and the only region where " +
                    "automatic rotation is managed. Replica keys in other regions are synchronized " +
                    "from the primary region. Changing the primary region may require manual " +
                    "replication of key material to the new region.",
            operationId = "updatePrimaryRegion",
            hidden = true
    )
    ResponseEntity<KeyMetadataResponseDto> updatePrimaryRegion(
            @PathVariable Long keyId,
            @RequestBody UpdatePrimaryRegionRequestDto request);

    /**
     * Replicates a multi-region key to another KMS region.
     *
     * @param keyId   The unique identifier of the source key
     * @param request Contains destination region specification
     * @return ResponseEntity containing the replicated key metadata
     */
    @PostMapping("/keys/{keyId}/replicate")
    @Operation(
            summary = "Replicate Key",
            description = "Replicates a multi-region key to another WAMS region, creating a replica key. " +
                    "The replica key is independent but shares the same key material ID. " +
                    "Changes in primary region key material are synced to replicas. " +
                    "Useful for disaster recovery, latency reduction, and cross-region data access. " +
                    "Requires kms:ReplicateKey permission in both regions.",
            operationId = "replicateKey",
            hidden = true
    )
    ResponseEntity<ReplicateKeyResponseDto> replicateKey(
            @PathVariable Long keyId,
            @RequestBody ReplicateKeyRequestDto request);

    /**
     * Synchronizes a multi-region key replica with its primary.
     *
     * @param keyId The unique identifier of the replica key
     * @return ResponseEntity containing synchronized key metadata
     */
    @PostMapping("/keys/{keyId}/synchronize")
    @Operation(
            summary = "Synchronize Multi-Region Key",
            description = "Synchronizes a replicated multi-region key with its primary region. " +
                    "Ensures the replica has the latest key material and rotation schedule. " +
                    "Should be called after key rotation or configuration changes in the primary region. " +
                    "Automatic synchronization occurs periodically, but manual sync can be forced.",
            operationId = "synchronizeMultiRegionKey"
    )
    ResponseEntity<KeyMetadataResponseDto> synchronizeMultiRegionKey(@PathVariable Long keyId);

    // ============================================================================
    // ALIAS, POLICY, GRANTS, TAGGING, IMPORT, etc.
    // ============================================================================

    /**
     * Creates a friendly name alias for a KMS key.
     *
     * @param request Contains alias name and target key ID
     * @return ResponseEntity containing created alias information
     */
    @PostMapping("/aliases")
    @Operation(
            summary = "Create Alias",
            description = "Creates a friendly name alias for a KMS key. Aliases make keys easier to reference " +
                    "in applications and scripts (e.g., 'alias/production-key' instead of a UUID). " +
                    "Aliases must be unique within the account and start with 'alias/'. " +
                    "Multiple aliases can point to the same key, but aliases cannot be shared across keys.",
            operationId = "createAlias"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Alias created"),
            @ApiResponse(responseCode = "400", description = "Invalid alias name format"),
            @ApiResponse(responseCode = "409", description = "Alias already exists"),
            @ApiResponse(responseCode = "404", description = "Target key not found")
    })
    ResponseEntity<AliasResponseDto> createAlias(@RequestBody CreateAliasRequestDto request);

    /**
     * Updates an existing alias to point to a different key.
     *
     * @param aliasName The alias name to update
     * @param request   Contains new target key ID
     * @return ResponseEntity containing updated alias information
     */
    @PutMapping("/aliases/{aliasName}")
    @Operation(
            summary = "Update Alias",
            description = "Updates an existing alias to point to a different KMS key. " +
                    "The alias name remains the same, but its target key changes. " +
                    "Useful for key rotation scenarios where applications reference a fixed alias. " +
                    "The old key is not automatically deleted or disabled.",
            operationId = "updateAlias"
    )
    ResponseEntity<AliasResponseDto> updateAlias(
            @PathVariable String aliasName,
            @RequestBody UpdateAliasRequestDto request);

    /**
     * Deletes an alias.
     *
     * @param aliasName The alias name to delete
     * @return ResponseEntity with empty body on success
     */
    @DeleteMapping("/aliases/{aliasName}")
    @Operation(
            summary = "Delete Alias",
            description = "Deletes an alias. Deleting an alias does not affect the underlying key. " +
                    "Applications using the alias will need to be updated to reference the key directly " +
                    "or use a different alias.",
            operationId = "deleteAlias"
    )
    ResponseEntity<?> deleteAlias(@PathVariable String aliasName);

    /**
     * Lists all aliases in the account with pagination.
     *
     * @param limit     Maximum number of aliases to return
     * @param nextToken Pagination token
     * @return ResponseEntity containing list of aliases
     */
    @GetMapping("/aliases")
    @Operation(
            summary = "List Aliases",
            description = "Lists all aliases in the account. Returns alias name, target key ID, and creation date. " +
                    "Results are paginated for large accounts. Aliases are sorted alphabetically by name.",
            operationId = "listAliases"
    )
    ResponseEntity<ListAliasesResponseDto> listAliases(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String nextToken);

    /**
     * Lists all aliases associated with a specific key.
     *
     * @param keyId The key identifier
     * @return ResponseEntity containing list of aliases for the key
     */
    @GetMapping("/keys/{keyId}/aliases")
    @Operation(
            summary = "List Aliases for Key",
            description = "Lists all aliases associated with a specific KMS key. " +
                    "Useful for understanding how a key is referenced in applications.",
            operationId = "listAliasesForKey"
    )
    ResponseEntity<ListAliasesResponseDto> listAliasesForKey(@PathVariable Long keyId);

    /**
     * Sets or updates the key policy for a KMS key.
     *
     * @param keyId   The key identifier
     * @param request Contains the key policy document
     * @return ResponseEntity with empty body on success
     */
    @PutMapping("/keys/{keyId}/policy")
    @Operation(
            summary = "Set Key Policy",
            description = "Sets or updates the key policy for a KMS key. Key policies define who can use, manage, " +
                    "or rotate the key. Policies are JSON documents conforming to WAMS IMS policy language. " +
                    "A key must have at least one policy. The policy can specify users, roles, services, " +
                    "and conditions (IP, MFA, time-based, etc.).",
            operationId = "setKeyPolicy"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Policy set successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid policy document"),
            @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Key not found")
    })
    ResponseEntity<?> setKeyPolicy(@PathVariable Long keyId, @RequestBody SetKeyPolicyRequestDto request);

    /**
     * Retrieves the key policy for a KMS key.
     *
     * @param keyId The key identifier
     * @return ResponseEntity containing the key policy document
     */
    @GetMapping("/keys/{keyId}/policy")
    @Operation(
            summary = "Get Key Policy",
            description = "Retrieves the key policy for a KMS key. Returns the JSON policy document " +
                    "that defines permissions for the key. Use for auditing and compliance verification.",
            operationId = "getKeyPolicy"
    )
    ResponseEntity<?> getKeyPolicy(@PathVariable Long keyId);

    /**
     * Creates a grant for a KMS key.
     *
     * @param keyId   The key identifier
     * @param request Contains grantee principal, operations, and constraints
     * @return ResponseEntity containing grant information and ID
     */
    @PostMapping("/keys/{keyId}/grants")
    @Operation(
            summary = "Create Grant",
            description = "Creates a grant for a KMS key, allowing specific principals to perform specific " +
                    "operations without editing the key policy. Grants are useful for temporary access, " +
                    "cross-account access, or when the key policy cannot be modified. Grants can have " +
                    "constraints (encryption context, grant tokens) and can be retired by grantees.",
            operationId = "createGrant"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Grant created"),
            @ApiResponse(responseCode = "400", description = "Invalid grant parameters"),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "409", description = "Grant already exists")
    })
    ResponseEntity<GrantResponseDto> createGrant(
            @PathVariable Long keyId,
            @RequestBody CreateGrantRequestDto request);

    /**
     * Lists all grants for a KMS key.
     *
     * @param keyId     The key identifier
     * @param limit     Maximum number of grants to return
     * @param nextToken Pagination token
     * @return ResponseEntity containing list of grants
     */
    @GetMapping("/keys/{keyId}/grants")
    @Operation(
            summary = "List Grants",
            description = "Lists all grants for a KMS key. Returns grant ID, grantee principal, " +
                    "operations, and status information. Useful for auditing permissions and " +
                    "understanding who has access to the key.",
            operationId = "listGrants"
    )
    ResponseEntity<ListGrantsResponseDto> listGrants(
            @PathVariable Long keyId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String nextToken);

    /**
     * Revokes a grant for a KMS key.
     *
     * @param keyId   The key identifier
     * @param grantId The grant identifier to revoke
     * @return ResponseEntity with empty body on success
     */
    @DeleteMapping("/keys/{keyId}/grants/{grantId}")
    @Operation(
            summary = "Revoke Grant",
            description = "Revokes a grant, immediately removing the permissions it provided. " +
                    "Revocation is immediate and irreversible. Any operations using the grant token " +
                    "that are in progress may still complete, but new operations are denied.",
            operationId = "revokeGrant"
    )
    ResponseEntity<?> revokeGrant(@PathVariable Long keyId, @PathVariable String grantId);

    /**
     * Retires a grant (allows grantee to self-remove permissions).
     *
     * @param grantId The grant identifier
     * @param request Contains optional grant token
     * @return ResponseEntity with empty body on success
     */
    @PutMapping("/grants/{grantId}/retire")
    @Operation(
            summary = "Retire Grant",
            description = "Retires a grant, removing the permissions it provided. Unlike revocation, " +
                    "retirement can be performed by the grantee principal (not just the key admin). " +
                    "Grants can be retired by using the GrantId or GrantToken.",
            operationId = "retireGrant"
    )
    ResponseEntity<?> retireGrant(@PathVariable String grantId, @RequestBody RetireGrantRequestDto request);

    /**
     * Adds tags to a KMS key for cost allocation and organization.
     *
     * @param keyId   The key identifier
     * @param request Contains tags to add
     * @return ResponseEntity with empty body on success
     */
    @PostMapping("/keys/{keyId}/tags")
    @Operation(
            summary = "Tag Resource",
            description = "Adds or updates tags on a KMS key. Tags are key-value pairs used for cost allocation, " +
                    "access control, automation, and organization. You can tag keys to indicate environment, " +
                    "owner, cost center, or compliance requirements. Maximum 50 tags per key.",
            operationId = "tagResource"
    )
    ResponseEntity<?> tagResource(@PathVariable Long keyId, @RequestBody TagResourceRequestDto request);

    /**
     * Removes tags from a KMS key.
     *
     * @param keyId   The key identifier
     * @param request Contains tag keys to remove
     * @return ResponseEntity with empty body on success
     */
    @DeleteMapping("/keys/{keyId}/tags")
    @Operation(
            summary = "Untag Resource",
            description = "Removes tags from a KMS key. Only the tag keys are removed; the values are not needed. " +
                    "If a tag key doesn't exist, the operation continues silently.",
            operationId = "untagResource"
    )
    ResponseEntity<?> untagResource(@PathVariable Long keyId, @RequestBody UntagResourceRequestDto request);

    /**
     * Lists all tags on a KMS key.
     *
     * @param keyId The key identifier
     * @return ResponseEntity containing list of tags
     */
    @GetMapping("/keys/{keyId}/tags")
    @Operation(
            summary = "List Resource Tags",
            description = "Lists all tags on a KMS key. Returns tag keys and values for cost tracking and audits.",
            operationId = "listResourceTags"
    )
    ResponseEntity<ListTagsResponseDto> listResourceTags(@PathVariable Long keyId);

    /**
     * Gets parameters for importing key material (BYOK).
     *
     * @param keyId The key identifier
     * @return ResponseEntity containing wrapping key and import token
     */
    @PostMapping("/keys/{keyId}/import-parameters")
    @Operation(
            summary = "Get Parameters for Import",
            description = "Gets parameters needed to import your own key material (BYOK - Bring Your Own Key). " +
                    "Returns a public key for encrypting your key material and an import token that " +
                    "associates the material with the specified key. The public key is valid for a limited time. " +
                    "Use for importing keys from external HSMs or on-premises key management systems.",
            operationId = "getParametersForImport",
            hidden = true
    )
    ResponseEntity<ImportParametersResponseDto> getParametersForImport(@PathVariable Long keyId);

    /**
     * Imports key material into a KMS key (BYOK).
     *
     * @param keyId   The key identifier
     * @param request Contains encrypted key material and import token
     * @return ResponseEntity containing updated key metadata
     */
    @PostMapping("/keys/{keyId}/import-key-material")
    @Operation(
            summary = "Import Key Material",
            description = "Imports your own key material into a KMS key that was created with a null key origin. " +
                    "The key material must be encrypted using the public key from getParametersForImport. " +
                    "After import, the key becomes usable immediately. Keys with imported material " +
                    "must be re-imported after expiration or deletion.",
            operationId = "importKeyMaterial",
            hidden = true
    )
    ResponseEntity<KeyMetadataResponseDto> importKeyMaterial(
            @PathVariable Long keyId,
            @RequestBody ImportKeyMaterialRequestDto request);

    /**
     * Deletes imported key material, rendering the key unusable.
     *
     * @param keyId The key identifier
     * @return ResponseEntity containing key metadata (now disabled)
     */
    @DeleteMapping("/keys/{keyId}/key-material")
    @Operation(
            summary = "Delete Imported Key Material",
            description = "Deletes imported key material from a KMS key, rendering the key unusable. " +
                    "This effectively disables the key but preserves metadata and aliases. " +
                    "You can re-import new key material later using the same key. " +
                    "Useful for key rotation or security incidents with imported keys.",
            operationId = "deleteImportedKeyMaterial",
            hidden = true
    )
    ResponseEntity<KeyMetadataResponseDto> deleteImportedKeyMaterial(@PathVariable Long keyId);

    // ============================================================================
    // AUDIT & UTILITY
    // ============================================================================

    /**
     * Retrieves audit logs of cryptographic operations.
     *
     * @param keyId    Optional filter by key identifier
     * @param fromDate Optional start date filter (ISO 8601 format)
     * @param toDate   Optional end date filter (ISO 8601 format)
     * @param limit    Maximum number of log entries to return
     * @return ResponseEntity containing audit log entries
     */
    @GetMapping("/audit/logs")
    @Operation(
            summary = "Get Audit Logs",
            description = "Retrieves audit history of cryptographic operations (encrypt, decrypt, sign, verify, generate). " +
                    "Logs include operation type, timestamp, key ID, client IP, user identity, and success/failure status. " +
                    "Retain logs for 90 days by default. Use for security audits, incident investigation, and compliance reporting.",
            operationId = "getAuditLogs"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Audit logs retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid date range"),
            @ApiResponse(responseCode = "403", description = "Access denied - need kms:List* permissions")
    })
    ResponseEntity<AuditLogResponseDto> getAuditLogs(
            @Parameter(description = "Filter logs by specific key ID", example = "1234abcd-12ab-34cd-56ef-1234567890ab")
            @RequestParam(required = false) Long keyId,

            @Parameter(description = "Start date for audit logs (ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ)",
                    example = "2024-01-01T00:00:00Z")
            @RequestParam(required = false) String fromDate,

            @Parameter(description = "End date for audit logs (ISO 8601 format)",
                    example = "2024-01-31T23:59:59Z")
            @RequestParam(required = false) String toDate,

            @Parameter(description = "Maximum number of log entries (1-1000, default: 100)",
                    example = "100")
            @RequestParam(required = false) Integer limit);

    /**
     * Retrieves usage statistics for a KMS key.
     *
     * @param keyId The key identifier
     * @return ResponseEntity containing usage metrics (operation counts, last used date, etc.)
     */
    @GetMapping("/keys/{keyId}/usage-stats")
    @Operation(
            summary = "Get Key Usage Stats",
            description = "Returns usage statistics for a KMS key including total operation counts, " +
                    "last used timestamp, operation breakdown (encrypt/decrypt/sign/etc.), " +
                    "and average request latency. Useful for identifying unused keys, " +
                    "optimizing performance, and capacity planning.",
            operationId = "getKeyUsageStats"
    )
    ResponseEntity<KeyUsageStatsDto> getKeyUsageStats(@PathVariable Long keyId);

    /**
     * Generates cryptographically secure random data.
     *
     * @param length      Number of random bytes to generate (1-1024)
     * @param charSetType Character set for output (ALPHANUMERIC, NUMERIC, HEX, BASE64)
     * @return ResponseEntity containing random data as string
     */
    @GetMapping("/random")
    @Operation(
            summary = "Generate Random",
            description = "Generates cryptographically secure random data using a FIPS-validated random number generator. " +
                    "Use for generating tokens, passwords, nonces, initialization vectors, or keys. " +
                    "The randomness is suitable for cryptographic applications. Maximum length: 1024 bytes per request.",
            operationId = "generateRandomData"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Random data generated"),
            @ApiResponse(responseCode = "400", description = "Invalid length (must be 1-1024)"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    ResponseEntity<String> generateRandomData(
            @Parameter(description = "Number of random bytes to generate (1-1024)",
                    required = true, example = "32")
            @RequestParam Integer length,

            @Parameter(description = "Character set encoding for the output",
                    required = true,
                    schema = @Schema(implementation = IEnumCharSet.Types.class))
            @RequestParam IEnumCharSet.Types charSetType);

    /**
     * Validates that a key exists and is usable.
     *
     * @param keyId The key identifier to validate
     * @return ResponseEntity with empty body if key is valid
     */
    @PostMapping("/keys/{keyId}/validate")
    @Operation(
            summary = "Validate Key",
            description = "Validates that a key exists and is usable for cryptographic operations. " +
                    "Checks that the key exists, is not pending deletion, and is not disabled. " +
                    "Returns 200 OK if valid, appropriate error code otherwise. " +
                    "Use before attempting operations to avoid runtime failures.",
            operationId = "validateKey"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Key is valid and usable"),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "409", description = "Key is disabled or pending deletion"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    ResponseEntity<?> validateKey(@PathVariable Long keyId);

    // ============================================================================
// CUSTOM KEY STORE APIs
// ============================================================================

    /**
     * Creates a new custom key store.
     *
     * <p><b>Usage Example - CloudHSM Custom Key Store:</b></p>
     * <pre>
     * POST /custom-key-stores
     * {
     *   "customKeyStoreName": "Production-CloudHSM-Store",
     *   "cloudHsmClusterId": "cluster-12345678",
     *   "keyStorePassword": "******",
     *   "trustAnchorCertificate": "-----BEGIN CERTIFICATE-----..."
     * }
     * </pre>
     *
     * <p><b>Usage Example - External Key Store (XKS):</b></p>
     * <pre>
     * POST /custom-key-stores
     * {
     *   "customKeyStoreName": "External-XKS-Store",
     *   "customKeyStoreType": "EXTERNAL_KEY_STORE",
     *   "xksProxyUriEndpoint": "https://xks.example.com:8080",
     *   "xksProxyUriPath": "/api/v1/kms",
     *   "xksProxyAuthenticationCredential": "arn:aws:secretsmanager:region:account:secret:xks-proxy-auth"
     * }
     * </pre>
     *
     * @param request Custom key store configuration
     * @return ResponseEntity containing the created custom key store metadata
     */
    @PostMapping("/custom-key-stores")
    @Operation(
            summary = "Create Custom Key Store",
            description = "Creates a custom key store (CloudHSM cluster or external key store) that can host KMS keys. " +
                    "Custom key stores allow you to use keys stored in your own CloudHSM cluster or external " +
                    "key management system under your control. Keys created in custom key stores never leave " +
                    "your hardware. Two types supported: CLOUDHSM and EXTERNAL_KEY_STORE (XKS).",
            operationId = "createCustomKeyStore"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Custom key store created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request - missing required parameters"),
            @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions"),
            @ApiResponse(responseCode = "409", description = "Conflict - custom key store name already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<CustomKeyStoreResponseDto> createCustomKeyStore(@RequestBody CreateCustomKeyStoreRequestDto request);

    /**
     * Retrieves metadata about a custom key store.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * GET /custom-key-stores/cks-1234567890abcdef0
     * </pre>
     *
     * @param customKeyStoreId The unique identifier of the custom key store
     * @return ResponseEntity containing custom key store metadata including connection status
     */
    @GetMapping("/custom-key-stores/{customKeyStoreId}")
    @Operation(
            summary = "Describe Custom Key Store",
            description = "Returns detailed metadata about the specified custom key store including " +
                    "connection status (CONNECTED, DISCONNECTED, FAILED), creation date, " +
                    "and configuration details (CloudHSM cluster ID or XKS proxy endpoint). " +
                    "Use this to monitor the health and status of your custom key store.",
            operationId = "describeCustomKeyStore"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Custom key store metadata retrieved"),
            @ApiResponse(responseCode = "404", description = "Custom key store not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<CustomKeyStoreResponseDto> describeCustomKeyStore(@PathVariable String customKeyStoreId);

    /**
     * Updates a custom key store configuration.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * PATCH /custom-key-stores/cks-1234567890abcdef0
     * {
     *   "newCustomKeyStoreName": "Updated-KeyStore-Name",
     *   "keyStorePassword": "new-password-****",
     *   "xksProxyUriEndpoint": "https://new-xks.example.com:8080"
     * }
     * </pre>
     *
     * @param customKeyStoreId The unique identifier of the custom key store
     * @param request          Updates to apply to the custom key store
     * @return ResponseEntity containing updated custom key store metadata
     */
    @PatchMapping("/custom-key-stores/{customKeyStoreId}")
    @Operation(
            summary = "Update Custom Key Store",
            description = "Updates configuration properties of a custom key store. " +
                    "For CloudHSM stores: can update name and password. For external key stores: " +
                    "can update name, proxy endpoint, and authentication credentials. " +
                    "Changes may require reconnecting the custom key store to take effect.",
            operationId = "updateCustomKeyStore"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Custom key store updated successfully"),
            @ApiResponse(responseCode = "404", description = "Custom key store not found"),
            @ApiResponse(responseCode = "400", description = "Invalid update parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<CustomKeyStoreResponseDto> updateCustomKeyStore(@PathVariable String customKeyStoreId,
                                                                   @RequestBody UpdateCustomKeyStoreRequestDto request);

    /**
     * Deletes a custom key store.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * DELETE /custom-key-stores/cks-1234567890abcdef0
     * </pre>
     *
     * <p><b>Important:</b> The custom key store must be disconnected and contain no KMS keys before deletion.</p>
     *
     * @param customKeyStoreId The unique identifier of the custom key store
     * @return ResponseEntity with empty body on success
     */
    @DeleteMapping("/custom-key-stores/{customKeyStoreId}")
    @Operation(
            summary = "Delete Custom Key Store",
            description = "Deletes a custom key store. The custom key store must be disconnected " +
                    "and must not contain any KMS keys. After deletion, keys from this store " +
                    "become unusable and cannot be recovered. This operation is irreversible.",
            operationId = "deleteCustomKeyStore"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Custom key store deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Custom key store has keys or is still connected"),
            @ApiResponse(responseCode = "404", description = "Custom key store not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<?> deleteCustomKeyStore(@PathVariable String customKeyStoreId);

    /**
     * Lists all custom key stores in the account with pagination.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * GET /custom-key-stores?limit=50&nextToken=abc123
     * </pre>
     *
     * @param limit     Maximum number of custom key stores to return (default: 100, max: 1000)
     * @param nextToken Pagination token for retrieving the next page of results
     * @return ResponseEntity containing list of custom key stores
     */
    @GetMapping("/custom-key-stores")
    @Operation(
            summary = "List Custom Key Stores",
            description = "Returns a paginated list of all custom key stores in the account. " +
                    "Results include basic metadata (ID, name, type, connection status). " +
                    "Results are sorted by creation date descending.",
            operationId = "listCustomKeyStores"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Custom key stores listed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<ListCustomKeyStoresResponseDto> listCustomKeyStores(@RequestParam(required = false) Integer limit,
                                                                       @RequestParam(required = false) String nextToken);

    /**
     * Connects a custom key store to its underlying hardware (CloudHSM or external proxy).
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * POST /custom-key-stores/cks-1234567890abcdef0/connect
     * </pre>
     *
     * @param customKeyStoreId The unique identifier of the custom key store
     * @return ResponseEntity indicating success or failure
     */
    @PostMapping("/custom-key-stores/{customKeyStoreId}/connect")
    @Operation(
            summary = "Connect Custom Key Store",
            description = "Establishes a connection between KMS and the custom key store's " +
                    "underlying hardware (CloudHSM cluster or external proxy). The custom key store " +
                    "must be in DISCONNECTED state to connect. After successful connection, " +
                    "the custom key store can be used to create and manage KMS keys. " +
                    "Connection may take several minutes.",
            operationId = "connectCustomKeyStore"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Connection initiated successfully"),
            @ApiResponse(responseCode = "400", description = "Custom key store is already connected or invalid state"),
            @ApiResponse(responseCode = "404", description = "Custom key store not found"),
            @ApiResponse(responseCode = "500", description = "Connection failed - check CloudHSM or proxy availability")
    })
    ResponseEntity<?> connectCustomKeyStore(@PathVariable String customKeyStoreId);

    /**
     * Disconnects a custom key store from its underlying hardware.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>
     * POST /custom-key-stores/cks-1234567890abcdef0/disconnect
     * </pre>
     *
     * @param customKeyStoreId The unique identifier of the custom key store
     * @return ResponseEntity indicating success or failure
     */
    @PostMapping("/custom-key-stores/{customKeyStoreId}/disconnect")
    @Operation(
            summary = "Disconnect Custom Key Store",
            description = "Disconnects KMS from the custom key store's underlying hardware. " +
                    "When disconnected, keys in the custom key store become unusable for " +
                    "cryptographic operations. The custom key store must be in CONNECTED state. " +
                    "Use this for maintenance or before deleting the custom key store.",
            operationId = "disconnectCustomKeyStore"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Disconnection initiated successfully"),
            @ApiResponse(responseCode = "400", description = "Custom key store is already disconnected or invalid state"),
            @ApiResponse(responseCode = "404", description = "Custom key store not found"),
            @ApiResponse(responseCode = "500", description = "Disconnection failed")
    })
    ResponseEntity<?> disconnectCustomKeyStore(@PathVariable String customKeyStoreId);
}