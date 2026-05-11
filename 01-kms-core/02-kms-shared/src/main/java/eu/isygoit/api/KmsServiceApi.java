package eu.isygoit.api;

import eu.isygoit.dto.KmsDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Key Management Service (KMS) REST API.
 * <p>
 * Provides a full set of operations for managing cryptographic keys,
 * encryption/decryption, digital signatures, envelope encryption,
 * key rotation, aliases, tagging, grants, policies, BYOK (import key material),
 * and custom key stores (CloudHSM / external).
 * </p>
 * <p>
 * This API follows RESTful best practices:
 * <ul>
 *   <li>GET requests never contain a request body – parameters are passed as path variables or query parameters.</li>
 *   <li>Resource identifiers are embedded in the URL path (e.g., {@code /keys/{keyId}}).</li>
 *   <li>Safe operations (GET, HEAD) are idempotent and read-only.</li>
 *   <li>POST is used for non-idempotent creation or actions, PUT for full replacement, PATCH for partial update, DELETE for removal.</li>
 * </ul>
 * </p>
 *
 * @author Isygoit Team
 * @see <a href="https://docs.aws.amazon.com/kms/latest/APIReference/Welcome.html">AWS KMS API Reference</a>
 */
@Tag(name = "KMS Service", description = "Key Management Service – manage keys and perform cryptographic operations")
public interface KmsServiceApi {

    // =========================================================================
    // Key Management
    // =========================================================================

    @PostMapping("/keys")
    @Operation(
            summary = "Create Key",
            description = "Creates a new customer managed key (CMK). Supports symmetric (AES‑256) and asymmetric (RSA, ECC) keys. " +
                    "You can set a description, key spec, usage, origin (WAMS_KMS or EXTERNAL for BYOK), tags, multi‑region flag, and an optional key policy.",
            operationId = "createKey"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Key created successfully",
                    content = @Content(schema = @Schema(implementation = CreateKeyResponse.class),
                            examples = @ExampleObject(value = "{\"KeyMetadata\":{\"KeyId\":\"1234abcd-...\",\"Arn\":\"arn:...\",\"Enabled\":true}}"))),
            @ApiResponse(responseCode = "400", description = "Invalid request – unsupported key spec or missing required field"),
            @ApiResponse(responseCode = "403", description = "Access denied – insufficient permissions"),
            @ApiResponse(responseCode = "409", description = "Conflict – alias already exists")
    })
    ResponseEntity<CreateKeyResponse> createKey(@Valid @RequestBody CreateKeyRequest request);

    @GetMapping("/keys/{keyId}")
    @Operation(
            summary = "Describe Key",
            description = "Returns detailed metadata about a KMS key, including its ARN, state, creation date, rotation status, and multi‑region configuration.",
            operationId = "describeKey"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Key metadata retrieved",
                    content = @Content(schema = @Schema(implementation = DescribeKeyResponse.class))),
            @ApiResponse(responseCode = "404", description = "Key not found")
    })
    ResponseEntity<DescribeKeyResponse> describeKey(
            @Parameter(description = "Unique identifier of the KMS key (KeyId or ARN)", required = true, example = "1234abcd-12ab-34cd-56ef-1234567890ab")
            @PathVariable("keyId") String keyId);

    @GetMapping("/keys")
    @Operation(
            summary = "List Keys",
            description = "Returns a paginated list of all KMS keys in the account. Includes basic metadata (KeyId, Arn).",
            operationId = "listKeys"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Keys listed successfully",
                    content = @Content(schema = @Schema(implementation = ListKeysResponse.class)))
    })
    ResponseEntity<ListKeysResponse> listKeys(
            @Parameter(description = "Maximum number of keys to return (1..1000)", example = "50")
            @RequestParam(value = "limit", required = false) Integer limit,
            @Parameter(description = "Pagination marker from previous response")
            @RequestParam(value = "marker", required = false) String marker);

    @DeleteMapping("/keys/{keyId}/schedule-deletion")
    @Operation(
            summary = "Schedule Key Deletion",
            description = "Schedules a KMS key for deletion with a configurable waiting period (7–30 days). " +
                    "During the waiting period the key cannot be used and its state becomes 'PendingDeletion'.",
            operationId = "scheduleKeyDeletion"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deletion scheduled successfully"),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "400", description = "Invalid waiting period")
    })
    ResponseEntity<ScheduleKeyDeletionResponse> scheduleKeyDeletion(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId,
            @Parameter(description = "Waiting period in days (7-30)", example = "30")
            @RequestParam(value = "pendingWindowInDays", defaultValue = "30") Integer pendingWindowInDays);

    @PostMapping("/keys/{keyId}/cancel-deletion")
    @Operation(
            summary = "Cancel Key Deletion",
            description = "Cancels a previously scheduled key deletion, restoring the key to its previous state (Enabled or Disabled).",
            operationId = "cancelKeyDeletion"
    )
    ResponseEntity<CancelKeyDeletionResponse> cancelKeyDeletion(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId);

    @DeleteMapping("/keys/{keyId}")
    @Operation(
            summary = "Delete Key (Permanent)",
            description = "Permanently deletes a KMS key. This operation is irreversible and should only be used " +
                    "after the key has been scheduled for deletion and the waiting period has expired. " +
                    "This is an extension beyond the AWS KMS API.",
            operationId = "deleteKey"
    )
    ResponseEntity<DeleteKeyResponse> deleteKey(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId);

    @PatchMapping("/keys/{keyId}/rotation")
    @Operation(
            summary = "Update Key Rotation",
            description = "Enables/disables automatic key rotation with optional custom period (90-3650 days).",
            operationId = "updateKeyRotation"
    )
    ResponseEntity<KeyRotationStatusResponseDto> updateKeyRotation(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId,
            @Valid @RequestBody UpdateKeyRotationRequestDto request);

    @PostMapping("/keys/{keyId}/enable")
    @Operation(
            summary = "Enable Key",
            description = "Enables a disabled KMS key, making it available for cryptographic operations.",
            operationId = "enableKey"
    )
    ResponseEntity<EnableKeyResponse> enableKey(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId);

    @PostMapping("/keys/{keyId}/disable")
    @Operation(
            summary = "Disable Key",
            description = "Disables a KMS key, preventing all cryptographic operations.",
            operationId = "disableKey"
    )
    ResponseEntity<DisableKeyResponse> disableKey(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId);

    @PatchMapping("/keys/{keyId}/description")
    @Operation(
            summary = "Update Key Description",
            description = "Updates the description of a KMS key.",
            operationId = "updateKeyDescription"
    )
    ResponseEntity<UpdateKeyDescriptionResponse> updateKeyDescription(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId,
            @Valid @RequestBody UpdateKeyDescriptionRequest request);

    // =========================================================================
    // Key Rotation
    // =========================================================================

    @GetMapping("/keys/{keyId}/rotation-status")
    @Operation(
            summary = "Get Key Rotation Status",
            description = "Returns whether automatic key rotation is enabled for the key, along with rotation period and last rotation date.",
            operationId = "getKeyRotationStatus"
    )
    ResponseEntity<GetKeyRotationStatusResponse> getKeyRotationStatus(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId);

    @GetMapping("/keys/{keyId}/rotations")
    @Operation(
            summary = "List Key Rotations",
            description = "Returns a paginated list of all key rotations (both automatic and manual) " +
                    "for the specified KMS key. Each rotation record includes the rotation timestamp and key version ID.",
            operationId = "listKeyRotations"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rotations listed successfully",
                    content = @Content(schema = @Schema(implementation = ListKeyRotationsResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
    })
    ResponseEntity<ListKeyRotationsResponseDto> listKeyRotations(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId,
            @Parameter(description = "Maximum number of rotations to return (1..1000)", example = "50")
            @RequestParam(value = "limit", required = false) Integer limit,
            @Parameter(description = "Pagination token from previous response")
            @RequestParam(value = "nextToken", required = false) String nextToken);

    @PostMapping("/keys/{keyId}/rotate/enable")
    @Operation(
            summary = "Enable Key Rotation",
            description = "Enables automatic annual rotation of the key material (symmetric keys only).",
            operationId = "enableKeyRotation"
    )
    ResponseEntity<EnableKeyRotationResponse> enableKeyRotation(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId);

    @PostMapping("/keys/{keyId}/rotate/disable")
    @Operation(
            summary = "Disable Key Rotation",
            description = "Disables automatic key rotation.",
            operationId = "disableKeyRotation"
    )
    ResponseEntity<DisableKeyRotationResponse> disableKeyRotation(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId);

    @PostMapping("/keys/{keyId}/rotate")
    @Operation(
            summary = "Rotate Key (On‑Demand)",
            description = "Immediately rotates a KMS key, creating a new key version.",
            operationId = "rotateKey"
    )
    ResponseEntity<RotateKeyResponse> rotateKey(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId);

    @GetMapping("/keys/{keyId}/versions")
    @Operation(
            summary = "List Key Versions",
            description = "Lists all versions of a KMS key (created by rotation or replication).",
            operationId = "listKeyVersions"
    )
    ResponseEntity<ListKeyVersionsResponse> listKeyVersions(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "marker", required = false) String marker);

    @GetMapping("/keys/{keyId}/active-version")
    @Operation(
            summary = "Get Active Key Version",
            description = "Returns the current active version ID of the specified KMS key.",
            operationId = "getActiveVersion",
            hidden = true
    )
    ResponseEntity<ActiveVersionResponseDto> getActiveVersion(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId);

    @PostMapping("/keys/{keyId}/primary-region")
    @Operation(
            summary = "Update Primary Region",
            description = "Updates the primary region of a multi‑region key. This operation is part of the AWS KMS API " +
                    "and returns an empty response on success.",
            operationId = "updatePrimaryRegion"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Primary region updated successfully"),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request – key is not multi‑region or region invalid")
    })
    ResponseEntity<UpdatePrimaryRegionResponse> updatePrimaryRegion(
            @Parameter(description = "Unique identifier of the multi-region primary key", required = true)
            @PathVariable("keyId") String keyId,
            @Valid @RequestBody UpdatePrimaryRegionRequestDto request);

    @PostMapping("/keys/{keyId}/replicate")
    @Operation(
            summary = "Replicate Key",
            description = "Replicates a multi‑region key to another region. Returns the replica key metadata.",
            operationId = "replicateKey"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Key replicated successfully",
                    content = @Content(schema = @Schema(implementation = ReplicateKeyResponse.class))),
            @ApiResponse(responseCode = "404", description = "Source key not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request – key not multi‑region or region invalid")
    })
    ResponseEntity<ReplicateKeyResponse> replicateKey(
            @Parameter(description = "Unique identifier of the multi-region primary key", required = true)
            @PathVariable("keyId") String keyId,
            @Valid @RequestBody ReplicateKeyRequestDto request);

    @PostMapping("/keys/{keyId}/synchronize")
    @Operation(
            summary = "Synchronize Multi-Region Key",
            description = "Synchronizes a multi‑region key replica with its primary region. " +
                    "This operation updates the replica's key material and metadata to match the primary. " +
                    "Required after rotating the primary key or changing its configuration.",
            operationId = "synchronizeMultiRegionKey"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Synchronization completed successfully"),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "400", description = "Key is not a multi‑region replica")
    })
    ResponseEntity<SynchronizeMultiRegionKeyResponse> synchronizeMultiRegionKey(
            @Parameter(description = "Unique identifier of the multi-region replica key", required = true)
            @PathVariable("keyId") String keyId);

    // =========================================================================
    // Cryptographic Operations
    // =========================================================================

    @PostMapping("/encrypt")
    @Operation(
            summary = "Encrypt",
            description = "Encrypts plaintext using a symmetric or asymmetric KMS key. Maximum 4096 bytes. " +
                    "Provide plaintext as base64-encoded string."
    )
    ResponseEntity<EncryptResponse> encrypt(@Valid @RequestBody EncryptRequest request);

    @PostMapping("/decrypt")
    @Operation(
            summary = "Decrypt",
            description = "Decrypts ciphertext that was encrypted under a KMS key. Ciphertext must be base64-encoded."
    )
    ResponseEntity<DecryptResponse> decrypt(@Valid @RequestBody DecryptRequest request);

    @PostMapping("/reencrypt")
    @Operation(
            summary = "Re‑Encrypt",
            description = "Decrypts ciphertext under one KMS key and re‑encrypts under another, without exposing plaintext."
    )
    ResponseEntity<ReEncryptResponse> reEncrypt(@Valid @RequestBody ReEncryptRequest request);

    @PostMapping("/datakey/generate")
    @Operation(
            summary = "Generate Data Key",
            description = "Generates a symmetric data key for envelope encryption. Returns plaintext and encrypted copies."
    )
    ResponseEntity<GenerateDataKeyResponse> generateDataKey(@Valid @RequestBody GenerateDataKeyRequest request);

    @PostMapping("/datakey/generate-without-plaintext")
    @Operation(
            summary = "Generate Data Key (No Plaintext)",
            description = "Generates a data key and returns only the encrypted copy (plaintext never leaves KMS)."
    )
    ResponseEntity<GenerateDataKeyWithoutPlaintextResponse> generateDataKeyWithoutPlaintext(@Valid @RequestBody GenerateDataKeyWithoutPlaintextRequest request);

    @PostMapping("/datakey/generate-pair")
    @Operation(
            summary = "Generate Data Key Pair",
            description = "Generates an asymmetric data key pair (public + encrypted private key)."
    )
    ResponseEntity<GenerateDataKeyPairResponse> generateDataKeyPair(@Valid @RequestBody GenerateDataKeyPairRequest request);

    @PostMapping("/datakey/generate-pair-without-plaintext")
    @Operation(
            summary = "Generate Data Key Pair (No Plaintext)",
            description = "Generates an asymmetric key pair and returns only the public key and encrypted private key."
    )
    ResponseEntity<GenerateDataKeyPairWithoutPlaintextResponse> generateDataKeyPairWithoutPlaintext(@Valid @RequestBody GenerateDataKeyPairWithoutPlaintextRequest request);

    @PostMapping("/sign")
    @Operation(
            summary = "Sign",
            description = "Generates a digital signature using an asymmetric KMS key. Message must be base64-encoded."
    )
    ResponseEntity<SignResponse> sign(@Valid @RequestBody SignRequest request);

    @PostMapping("/verify")
    @Operation(
            summary = "Verify Signature",
            description = "Verifies a digital signature using an asymmetric KMS key."
    )
    ResponseEntity<VerifyResponse> verify(@Valid @RequestBody VerifyRequest request);

    @PostMapping("/mac/generate")
    @Operation(
            summary = "Generate MAC",
            description = "Generates a Message Authentication Code (MAC) using a symmetric HMAC KMS key."
    )
    ResponseEntity<GenerateMacResponse> generateMac(@Valid @RequestBody GenerateMacRequest request);

    @PostMapping("/mac/verify")
    @Operation(
            summary = "Verify MAC",
            description = "Verifies a Message Authentication Code (MAC)."
    )
    ResponseEntity<VerifyMacResponse> verifyMac(@Valid @RequestBody VerifyMacRequest request);

    @GetMapping("/keys/{keyId}/public-key")
    @Operation(
            summary = "Get Public Key",
            description = "Returns the public key of an asymmetric KMS key (RSA or ECC) in base64-encoded DER format."
    )
    ResponseEntity<GetPublicKeyResponse> getPublicKey(
            @Parameter(description = "Unique identifier of the asymmetric KMS key", required = true)
            @PathVariable("keyId") String keyId);

    @GetMapping("/keys/{keyId}/audit-logs")
    @Operation(
            summary = "Get Audit Logs",
            description = "Returns audit logs (key usage events) for the specified key, optionally filtered by date range."
    )
    ResponseEntity<AuditLogResponse> getAuditLogs(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId,
            @Parameter(description = "Start date (ISO 8601)", example = "2024-01-01T00:00:00Z")
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @Parameter(description = "End date (ISO 8601)", example = "2024-12-31T23:59:59Z")
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @Parameter(description = "Maximum number of log entries to return", example = "100")
            @RequestParam(value = "limit", required = false) Integer limit);

    @GetMapping("/keys/{keyId}/usage-stats")
    @Operation(
            summary = "Get Key Usage Statistics",
            description = "Returns aggregated usage statistics for a KMS key, including operation counts and last used date."
    )
    ResponseEntity<KeyUsageStatsResponse> getKeyUsageStats(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId);

    @PostMapping("/random")
    @Operation(
            summary = "Generate Random",
            description = "Generates cryptographically secure random bytes (up to 1024). Returns base64-encoded result."
    )
    ResponseEntity<GenerateRandomResponse> generateRandom(@Valid @RequestBody GenerateRandomRequest request);

    // =========================================================================
    // Aliases
    // =========================================================================

    @GetMapping("/aliases")
    @Operation(
            summary = "List Aliases",
            description = "Lists all aliases in the account, including those for AWS managed keys.",
            operationId = "listAliases"
    )
    ResponseEntity<ListAliasesResponse> listAliases(
            @Parameter(description = "Maximum number of aliases to return (1..1000)", example = "50")
            @RequestParam(value = "limit", required = false) Integer limit,
            @Parameter(description = "Pagination marker from previous response")
            @RequestParam(value = "marker", required = false) String marker);

    @GetMapping("/keys/{keyId}/aliases")
    @Operation(
            summary = "List Aliases for Key",
            description = "Lists all aliases associated with a specific KMS key.",
            operationId = "listAliasesForKey"
    )
    ResponseEntity<ListAliasesResponse> listAliasesForKey(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "marker", required = false) String marker);

    @PostMapping("/aliases")
    @Operation(
            summary = "Create Alias",
            description = "Creates a friendly name alias for a KMS key (must start with 'alias/').",
            operationId = "createAlias"
    )
    ResponseEntity<CreateAliasResponse> createAlias(@Valid @RequestBody CreateAliasRequest request);

    @PatchMapping("/aliases/{aliasName}")
    @Operation(
            summary = "Update Alias",
            description = "Associates an existing alias with a different KMS key.",
            operationId = "updateAlias"
    )
    ResponseEntity<UpdateAliasResponse> updateAlias(
            @Parameter(description = "Alias name (e.g., alias/MyKey)", required = true)
            @PathVariable("aliasName") String aliasName,
            @Valid @RequestBody UpdateAliasRequest request);

    @DeleteMapping("/aliases/{aliasName}")
    @Operation(
            summary = "Delete Alias",
            description = "Deletes an alias. The underlying key is unaffected.",
            operationId = "deleteAlias"
    )
    ResponseEntity<DeleteAliasResponse> deleteAlias(
            @Parameter(description = "Alias name (e.g., alias/MyKey)", required = true)
            @PathVariable("aliasName") String aliasName);

    // =========================================================================
    // Tags
    // =========================================================================

    @GetMapping("/keys/{keyId}/tags")
    @Operation(
            summary = "List Resource Tags",
            description = "Lists all tags attached to a KMS key.",
            operationId = "listResourceTags"
    )
    ResponseEntity<ListResourceTagsResponse> listResourceTags(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "marker", required = false) String marker);

    @PostMapping("/keys/{keyId}/tags")
    @Operation(
            summary = "Tag Resource",
            description = "Adds or updates tags on a KMS key (max 50 tags per key).",
            operationId = "tagResource"
    )
    ResponseEntity<TagResourceResponse> tagResource(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId,
            @Valid @RequestBody TagResourceRequest request);

    @DeleteMapping("/keys/{keyId}/tags")
    @Operation(
            summary = "Untag Resource",
            description = "Removes tags from a KMS key.",
            operationId = "untagResource"
    )
    ResponseEntity<UntagResourceResponse> untagResource(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId,
            @Valid @RequestBody UntagResourceRequest request);

    // =========================================================================
    // Key Policies & Grants
    // =========================================================================

    @PutMapping("/keys/{keyId}/policy")
    @Operation(
            summary = "Put Key Policy",
            description = "Attaches or updates a key policy document to a KMS key. The policy is a JSON object.",
            operationId = "putKeyPolicy"
    )
    ResponseEntity<PutKeyPolicyResponse> putKeyPolicy(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId,
            @Valid @RequestBody PutKeyPolicyRequest request);

    @GetMapping("/keys/{keyId}/policy")
    @Operation(
            summary = "Get Key Policy",
            description = "Returns the key policy document attached to a KMS key. Default policy name is 'default'.",
            operationId = "getKeyPolicy"
    )
    ResponseEntity<GetKeyPolicyResponse> getKeyPolicy(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId,
            @Parameter(description = "Name of the policy (usually 'default')", example = "default")
            @RequestParam(value = "policyName", defaultValue = "default") String policyName);

    @GetMapping("/keys/{keyId}/policies")
    @Operation(
            summary = "List Key Policies",
            description = "Lists the names of all key policies attached to a KMS key.",
            operationId = "listKeyPolicies"
    )
    ResponseEntity<ListKeyPoliciesResponse> listKeyPolicies(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "marker", required = false) String marker);

    @PostMapping("/keys/{keyId}/grants")
    @Operation(
            summary = "Create Grant",
            description = "Creates a grant that allows a principal to perform specific operations on a key.",
            operationId = "createGrant"
    )
    ResponseEntity<CreateGrantResponse> createGrant(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId,
            @Valid @RequestBody CreateGrantRequest request);

    @GetMapping("/keys/{keyId}/grants")
    @Operation(
            summary = "List Grants",
            description = "Lists all grants for a KMS key.",
            operationId = "listGrants"
    )
    ResponseEntity<ListGrantsResponse> listGrants(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "marker", required = false) String marker,
            @RequestParam(value = "grantId", required = false) String grantId,
            @RequestParam(value = "granteePrincipal", required = false) String granteePrincipal);

    @DeleteMapping("/keys/{keyId}/grants/{grantId}")
    @Operation(
            summary = "Revoke Grant",
            description = "Revokes a grant, immediately removing its permissions.",
            operationId = "revokeGrant"
    )
    ResponseEntity<RevokeGrantResponse> revokeGrant(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId,
            @Parameter(description = "Unique identifier of the grant", required = true)
            @PathVariable("grantId") String grantId);

    @PostMapping("/grants/retire")
    @Operation(
            summary = "Retire Grant",
            description = "Retires a grant (can be called by grantee or key administrator).",
            operationId = "retireGrant"
    )
    ResponseEntity<RetireGrantResponse> retireGrant(@Valid @RequestBody RetireGrantRequest request);

    @GetMapping("/grants/retirable")
    @Operation(
            summary = "List Retirable Grants",
            description = "Lists grants that can be retired by a given principal.",
            operationId = "listRetirableGrants"
    )
    ResponseEntity<ListRetirableGrantsResponse> listRetirableGrants(
            @Parameter(description = "Principal that can retire the grants (ARN or account ID)", required = true)
            @RequestParam("retiringPrincipal") String retiringPrincipal,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "marker", required = false) String marker);

    // =========================================================================
    // BYOK (Import Key Material)
    // =========================================================================

    @PostMapping("/keys/{keyId}/import-parameters")
    @Operation(
            summary = "Get Parameters for Import",
            description = "Returns a public key and import token needed to import your own key material (BYOK). " +
                    "The public key is used to encrypt your key material, and the token binds it to the KMS key.",
            operationId = "getParametersForImport"
    )
    ResponseEntity<GetParametersForImportResponse> getParametersForImport(
            @Parameter(description = "Unique identifier of the KMS key (origin must be EXTERNAL)", required = true)
            @PathVariable("keyId") String keyId,
            @Valid @RequestBody GetParametersForImportRequest request);

    @PostMapping("/keys/{keyId}/import")
    @Operation(
            summary = "Import Key Material",
            description = "Imports your own key material into a KMS key created with origin = EXTERNAL.",
            operationId = "importKeyMaterial"
    )
    ResponseEntity<ImportKeyMaterialResponse> importKeyMaterial(
            @Parameter(description = "Unique identifier of the KMS key (origin EXTERNAL)", required = true)
            @PathVariable("keyId") String keyId,
            @Valid @RequestBody ImportKeyMaterialRequest request);

    @DeleteMapping("/keys/{keyId}/key-material")
    @Operation(
            summary = "Delete Imported Key Material",
            description = "Deletes imported key material, rendering the key unusable. The key becomes 'PendingImport'.",
            operationId = "deleteImportedKeyMaterial"
    )
    ResponseEntity<DeleteImportedKeyMaterialResponse> deleteImportedKeyMaterial(
            @Parameter(description = "Unique identifier of the KMS key with imported material", required = true)
            @PathVariable("keyId") String keyId);

    // =========================================================================
    // Custom Key Stores
    // =========================================================================

    @PostMapping("/custom-key-stores")
    @Operation(
            summary = "Create Custom Key Store",
            description = "Creates a custom key store backed by a CloudHSM cluster or external proxy (XKS).",
            operationId = "createCustomKeyStore"
    )
    ResponseEntity<CreateCustomKeyStoreResponse> createCustomKeyStore(@Valid @RequestBody CreateCustomKeyStoreRequest request);

    @GetMapping("/custom-key-stores/{customKeyStoreId}")
    @Operation(
            summary = "Describe Custom Key Store",
            description = "Returns metadata about a custom key store, including connection status and configuration.",
            operationId = "describeCustomKeyStore"
    )
    ResponseEntity<DescribeCustomKeyStoreResponse> describeCustomKeyStore(
            @Parameter(description = "Numeric ID of the custom key store", required = true)
            @PathVariable("customKeyStoreId") Long customKeyStoreId);

    @PatchMapping("/custom-key-stores/{customKeyStoreId}")
    @Operation(
            summary = "Update Custom Key Store",
            description = "Updates configuration properties of a custom key store (e.g., name, connection parameters).",
            operationId = "updateCustomKeyStore"
    )
    ResponseEntity<UpdateCustomKeyStoreResponse> updateCustomKeyStore(
            @Parameter(description = "Numeric ID of the custom key store", required = true)
            @PathVariable("customKeyStoreId") Long customKeyStoreId,
            @Valid @RequestBody UpdateCustomKeyStoreRequest request);

    @DeleteMapping("/custom-key-stores/{customKeyStoreId}")
    @Operation(
            summary = "Delete Custom Key Store",
            description = "Deletes a custom key store (must be disconnected and contain no keys).",
            operationId = "deleteCustomKeyStore"
    )
    ResponseEntity<DeleteCustomKeyStoreResponse> deleteCustomKeyStore(
            @Parameter(description = "Numeric ID of the custom key store", required = true)
            @PathVariable("customKeyStoreId") Long customKeyStoreId);

    @PostMapping("/custom-key-stores/{customKeyStoreId}/connect")
    @Operation(
            summary = "Connect Custom Key Store",
            description = "Connects a custom key store to its underlying hardware (CloudHSM or external proxy).",
            operationId = "connectCustomKeyStore"
    )
    ResponseEntity<ConnectCustomKeyStoreResponse> connectCustomKeyStore(
            @Parameter(description = "Numeric ID of the custom key store", required = true)
            @PathVariable("customKeyStoreId") Long customKeyStoreId);

    @PostMapping("/custom-key-stores/{customKeyStoreId}/disconnect")
    @Operation(
            summary = "Disconnect Custom Key Store",
            description = "Disconnects a custom key store from its underlying hardware.",
            operationId = "disconnectCustomKeyStore"
    )
    ResponseEntity<DisconnectCustomKeyStoreResponse> disconnectCustomKeyStore(
            @Parameter(description = "Numeric ID of the custom key store", required = true)
            @PathVariable("customKeyStoreId") Long customKeyStoreId);

    @GetMapping("/custom-key-stores")
    @Operation(
            summary = "List Custom Key Stores",
            description = "Lists all custom key stores in the account (paginated).",
            operationId = "listCustomKeyStores"
    )
    ResponseEntity<ListCustomKeyStoresResponse> listCustomKeyStores(
            @Parameter(description = "Maximum number of stores to return (1..1000)", example = "50")
            @RequestParam(value = "limit", required = false) Integer limit,
            @Parameter(description = "Pagination marker from previous response")
            @RequestParam(value = "marker", required = false) String marker,
            @Parameter(description = "Filter by custom key store ID")
            @RequestParam(value = "customKeyStoreId", required = false) Long customKeyStoreId,
            @Parameter(description = "Filter by custom key store name (exact match)")
            @RequestParam(value = "customKeyStoreName", required = false) String customKeyStoreName);

    @PostMapping("/keys/{keyId}/validate")
    @Operation(
            summary = "Validate Key",
            description = "Validates the specified key (checks key material integrity, state, and permissions).",
            operationId = "validateKey"
    )
    ResponseEntity<ValidateKeyResponse> validateKey(
            @Parameter(description = "Unique identifier of the KMS key", required = true)
            @PathVariable("keyId") String keyId);
}