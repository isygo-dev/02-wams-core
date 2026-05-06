package eu.isygoit.service;

import eu.isygoit.dto.request.*;
import eu.isygoit.dto.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * The interface Key service api.
 */
public interface IKmsServiceApi {

    // Key Management APIs
    /**
     * Create key response entity.
     *
     * @param request the request
     * @return the response entity
     */
    @PostMapping("/keys")
    @Operation(summary = "Create Key", description = "Creates a new cryptographic key with metadata and initial active version.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Key created successfully",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = CreateKeyResponseDto.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<CreateKeyResponseDto> createKey(@RequestBody CreateKeyRequestDto request);

    /**
     * Get key metadata response entity.
     *
     * @param keyId the key id
     * @return the response entity
     */
    @GetMapping("/keys/{keyId}")
    @Operation(summary = "Get Key Metadata", description = "Retrieves metadata about a specific key.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Key metadata retrieved",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = KeyMetadataResponseDto.class))}),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<KeyMetadataResponseDto> getKeyMetadata(@PathVariable String keyId);

    /**
     * List keys response entity.
     *
     * @param limit     the limit
     * @param nextToken the next token
     * @return the response entity
     */
    @GetMapping("/keys")
    @Operation(summary = "List Keys", description = "Returns paginated list of keys.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Keys listed successfully",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ListKeysResponseDto.class))}),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<ListKeysResponseDto> listKeys(@RequestParam(required = false) Integer limit,
                                                 @RequestParam(required = false) String nextToken);

    /**
     * Enable key response entity.
     *
     * @param keyId the key id
     * @return the response entity
     */
    @PatchMapping("/keys/{keyId}/enable")
    @Operation(summary = "Enable Key", description = "Enables a disabled key for cryptographic operations.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Key enabled",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = KeyMetadataResponseDto.class))}),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<KeyMetadataResponseDto> enableKey(@PathVariable String keyId);

    /**
     * Disable key response entity.
     *
     * @param keyId the key id
     * @return the response entity
     */
    @PatchMapping("/keys/{keyId}/disable")
    @Operation(summary = "Disable Key", description = "Disables key usage immediately.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Key disabled",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = KeyMetadataResponseDto.class))}),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<KeyMetadataResponseDto> disableKey(@PathVariable String keyId);

    /**
     * Schedule key deletion response entity.
     *
     * @param keyId               the key id
     * @param pendingWindowInDays the pending window in days
     * @return the response entity
     */
    @DeleteMapping("/keys/{keyId}")
    @Operation(summary = "Schedule Key Deletion", description = "Schedules key deletion (soft delete with grace period).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deletion scheduled",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = KeyMetadataResponseDto.class))}),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<KeyMetadataResponseDto> scheduleKeyDeletion(@PathVariable String keyId,
                                                               @RequestParam(required = false) Integer pendingWindowInDays);

    /**
     * Rotate key response entity.
     *
     * @param keyId the key id
     * @return the response entity
     */
    @PostMapping("/keys/{keyId}/rotate")
    @Operation(summary = "Rotate Key", description = "Creates a new cryptographic version of the key.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Key rotated",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = RotateKeyResponseDto.class))}),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<RotateKeyResponseDto> rotateKey(@PathVariable String keyId);

    // Cryptographic Operations
    /**
     * Encrypt response entity.
     *
     * @param request the request
     * @return the response entity
     */
    @PostMapping("/encrypt")
    @Operation(summary = "Encrypt", description = "Encrypts plaintext using a KMS-managed key.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Encryption successful",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EncryptResponseDto.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<EncryptResponseDto> encrypt(@RequestBody EncryptRequestDto request);

    /**
     * Decrypt response entity.
     *
     * @param request the request
     * @return the response entity
     */
    @PostMapping("/decrypt")
    @Operation(summary = "Decrypt", description = "Decrypts ciphertext using correct key version automatically.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Decryption successful",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = DecryptResponseDto.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<DecryptResponseDto> decrypt(@RequestBody DecryptRequestDto request);

    /**
     * Reencrypt response entity.
     *
     * @param request the request
     * @return the response entity
     */
    @PostMapping("/reencrypt")
    @Operation(summary = "Re-encrypt", description = "Rewraps ciphertext from one key to another without exposing plaintext.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Re-encryption successful",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = EncryptResponseDto.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<EncryptResponseDto> reencrypt(@RequestBody ReencryptRequestDto request);

    // Signing APIs
    /**
     * Sign response entity.
     *
     * @param request the request
     * @return the response entity
     */
    @PostMapping("/sign")
    @Operation(summary = "Sign", description = "Generates a digital signature for a message.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Signature generated",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = SignResponseDto.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<SignResponseDto> sign(@RequestBody SignRequestDto request);

    /**
     * Verify response entity.
     *
     * @param request the request
     * @return the response entity
     */
    @PostMapping("/verify")
    @Operation(summary = "Verify", description = "Verifies a digital signature.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification complete",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = VerifyResponseDto.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<VerifyResponseDto> verify(@RequestBody VerifyRequestDto request);

    // Key Policy & Access Control
    /**
     * Set key policy response entity.
     *
     * @param keyId   the key id
     * @param request the request
     * @return the response entity
     */
    @PutMapping("/keys/{keyId}/policy")
    @Operation(summary = "Set Key Policy", description = "Defines IAM-like access rules for key usage.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Policy updated"),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<?> setKeyPolicy(@PathVariable String keyId, @RequestBody SetKeyPolicyRequestDto request);

    /**
     * Get key policy response entity.
     *
     * @param keyId the key id
     * @return the response entity
     */
    @GetMapping("/keys/{keyId}/policy")
    @Operation(summary = "Get Key Policy", description = "Retrieves current key policy.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Policy retrieved"),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<?> getKeyPolicy(@PathVariable String keyId);

    /**
     * Create grant response entity.
     *
     * @param keyId   the key id
     * @param request the request
     * @return the response entity
     */
    @PostMapping("/keys/{keyId}/grants")
    @Operation(summary = "Create Grant", description = "Delegates limited access to a key.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Grant created",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = GrantResponseDto.class))}),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<GrantResponseDto> createGrant(@PathVariable String keyId, @RequestBody CreateGrantRequestDto request);

    /**
     * Revoke grant response entity.
     *
     * @param keyId   the key id
     * @param grantId the grant id
     * @return the response entity
     */
    @DeleteMapping("/keys/{keyId}/grants/{grantId}")
    @Operation(summary = "Revoke Grant", description = "Removes previously granted permissions.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Grant revoked"),
            @ApiResponse(responseCode = "404", description = "Key or grant not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<?> revokeGrant(@PathVariable String keyId, @PathVariable String grantId);

    // Key Versioning APIs
    /**
     * List key versions response entity.
     *
     * @param keyId the key id
     * @return the response entity
     */
    @GetMapping("/keys/{keyId}/versions")
    @Operation(summary = "List Key Versions", description = "Lists all versions of a key.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Versions listed",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = KeyVersionListResponseDto.class))}),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<KeyVersionListResponseDto> listKeyVersions(@PathVariable String keyId);

    /**
     * Get active version response entity.
     *
     * @param keyId the key id
     * @return the response entity
     */
    @GetMapping("/keys/{keyId}/active-version")
    @Operation(summary = "Get Active Version", description = "Returns currently active key version.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Active version retrieved",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ActiveVersionResponseDto.class))}),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<ActiveVersionResponseDto> getActiveVersion(@PathVariable String keyId);

    // Data Key API
    /**
     * Generate data key response entity.
     *
     * @param request the request
     * @return the response entity
     */
    @PostMapping("/datakey/generate")
    @Operation(summary = "Generate Data Key", description = "Generates a data encryption key (DEK) for client-side encryption.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Data key generated",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = DataKeyResponseDto.class))}),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<DataKeyResponseDto> generateDataKey(@RequestBody GenerateDataKeyRequestDto request);

    // Audit APIs
    /**
     * Get audit logs response entity.
     *
     * @param keyId    the key id
     * @param fromDate the from date
     * @param toDate   the to date
     * @param limit    the limit
     * @return the response entity
     */
    @GetMapping("/audit/logs")
    @Operation(summary = "Get Audit Logs", description = "Returns cryptographic usage history for compliance and monitoring.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Audit logs retrieved",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = AuditLogResponseDto.class))}),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    ResponseEntity<AuditLogResponseDto> getAuditLogs(@RequestParam(required = false) String keyId,
                                                     @RequestParam(required = false) String fromDate,
                                                     @RequestParam(required = false) String toDate,
                                                     @RequestParam(required = false) Integer limit);
}

