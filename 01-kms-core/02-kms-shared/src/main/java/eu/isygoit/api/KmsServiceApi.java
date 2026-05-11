package eu.isygoit.api;

import eu.isygoit.dto.KmsDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Key Management Service (KMS) REST API.
 * <p>
 * Provides a full set of operations for managing cryptographic keys,
 * encryption/decryption, digital signatures, envelope encryption,
 * key rotation, aliases, tagging, grants, policies, BYOK (import key material),
 * and custom key stores (CloudHSM / external).
 * </p>
 *
 * @author Isygoit Team
 * @see <a href="https://docs.aws.amazon.com/kms/latest/APIReference/Welcome.html">WAMS KMS API Reference</a>
 */
@Tag(name = "KMS Service", description = "Key Management Service – manage keys and perform cryptographic operations")
@RequestMapping("/api/v1/kms")
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
    ResponseEntity<CreateKeyResponse> createKey(@RequestBody CreateKeyRequest request);

    @GetMapping("/keys/describe")
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
    ResponseEntity<DescribeKeyResponse> describeKey(@RequestBody DescribeKeyRequest request);

    @GetMapping("/keys/list")
    @Operation(
            summary = "List Keys",
            description = "Returns a paginated list of all KMS keys in the account. Includes basic metadata (KeyId, Arn).",
            operationId = "listKeys"
    )
    ResponseEntity<ListKeysResponse> listKeys(@RequestBody ListKeysRequest request);

    @DeleteMapping("/keys/schedule-deletion")
    @Operation(
            summary = "Schedule Key Deletion",
            description = "Schedules a KMS key for deletion with a configurable waiting period (7–30 days). " +
                    "During the waiting period the key cannot be used and its state becomes 'PendingDeletion'.",
            operationId = "scheduleKeyDeletion"
    )
    ResponseEntity<ScheduleKeyDeletionResponse> scheduleKeyDeletion(@RequestBody ScheduleKeyDeletionRequest request);

    @PostMapping("/keys/cancel-deletion")
    @Operation(
            summary = "Cancel Key Deletion",
            description = "Cancels a previously scheduled key deletion, restoring the key to its previous state (Enabled or Disabled).",
            operationId = "cancelKeyDeletion"
    )
    ResponseEntity<CancelKeyDeletionResponse> cancelKeyDeletion(@RequestBody CancelKeyDeletionRequest request);

    @DeleteMapping("/keys/delete")
    @Operation(
            summary = "Delete Key (Permanent)",
            description = "Permanently deletes a KMS key. This operation is irreversible and should only be used " +
                    "after the key has been scheduled for deletion and the waiting period has expired. " +
                    "This is an extension beyond the WAMS KMS API."
    )
    ResponseEntity<DeleteKeyResponse> deleteKey(@RequestBody DeleteKeyRequest request);

    @PutMapping("/keys/update-rotation")
    @Operation(summary = "Update Key Rotation (Custom)", description = "Enables/disables automatic key rotation with optional custom period (90-3650 days).")
    ResponseEntity<KeyRotationStatusResponseDto> updateKeyRotation(@RequestBody UpdateKeyRotationRequestDto request);

    @PostMapping("/keys/enable")
    @Operation(summary = "Enable Key", description = "Enables a disabled KMS key, making it available for cryptographic operations.")
    ResponseEntity<EnableKeyResponse> enableKey(@RequestBody EnableKeyRequest request);

    @PostMapping("/keys/disable")
    @Operation(summary = "Disable Key", description = "Disables a KMS key, preventing all cryptographic operations.")
    ResponseEntity<DisableKeyResponse> disableKey(@RequestBody DisableKeyRequest request);

    @PatchMapping("/keys/update-description")
    @Operation(summary = "Update Key Description", description = "Updates the description of a KMS key.")
    ResponseEntity<UpdateKeyDescriptionResponse> updateKeyDescription(@RequestBody UpdateKeyDescriptionRequest request);

    // =========================================================================
    // Key Rotation
    // =========================================================================

    @GetMapping("/keys/rotation-status")
    @Operation(summary = "Get Key Rotation Status", description = "Returns whether automatic key rotation is enabled for the key.")
    ResponseEntity<GetKeyRotationStatusResponse> getKeyRotationStatus(@RequestBody GetKeyRotationStatusRequest request);

    @GetMapping("/keys/rotations")
    @Operation(
            summary = "List Key Rotations",
            description = "Returns a paginated list of all key rotations (both automatic and manual) " +
                    "for the specified KMS key. Each rotation record includes the rotation timestamp " +
                    "and key version ID.",
            operationId = "listKeyRotations"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rotations listed successfully",
                    content = @Content(schema = @Schema(implementation = ListKeyRotationsResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
    })
    ResponseEntity<ListKeyRotationsResponseDto> listKeyRotations(@RequestBody ListKeyRotationsRequestDto request);

    @PostMapping("/keys/rotate/enable")
    @Operation(summary = "Enable Key Rotation", description = "Enables automatic annual rotation of the key material (symmetric keys only).")
    ResponseEntity<EnableKeyRotationResponse> enableKeyRotation(@RequestBody EnableKeyRotationRequest request);

    @PostMapping("/keys/rotate/disable")
    @Operation(summary = "Disable Key Rotation", description = "Disables automatic key rotation.")
    ResponseEntity<DisableKeyRotationResponse> disableKeyRotation(@RequestBody DisableKeyRotationRequest request);

    @PostMapping("/keys/rotate")
    @Operation(summary = "Rotate Key (On‑Demand)", description = "Immediately rotates a KMS key, creating a new key version.")
    ResponseEntity<RotateKeyResponse> rotateKey(@RequestBody RotateKeyRequest request);

    @GetMapping("/keys/versions")
    @Operation(summary = "List Key Versions", description = "Lists all versions of a KMS key (created by rotation or replication).")
    ResponseEntity<ListKeyVersionsResponse> listKeyVersions(@RequestBody ListKeyVersionsRequest request);

    @GetMapping("/keys/active-version")
    @Operation(hidden = true)
        // hide from OpenAPI if you want
    ResponseEntity<ActiveVersionResponseDto> getActiveVersion(@RequestBody ActiveVersionRequestDto request);

    @PostMapping("/keys/primary-region")
    @Operation(
            summary = "Update Primary Region",
            description = "Updates the primary region of a multi‑region key. This operation is part of the WAMS KMS API " +
                    "and returns an empty response on success.",
            operationId = "updatePrimaryRegion"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Primary region updated successfully"),
            @ApiResponse(responseCode = "404", description = "Key not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request – key is not multi‑region or region invalid")
    })
    ResponseEntity<UpdatePrimaryRegionResponse> updatePrimaryRegion(@RequestBody UpdatePrimaryRegionRequestDto request);

    @PostMapping("/keys/replicate")
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
    ResponseEntity<ReplicateKeyResponse> replicateKey(@RequestBody ReplicateKeyRequestDto request);

    @PostMapping("/keys/synchronize")
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
    ResponseEntity<SynchronizeMultiRegionKeyResponse> synchronizeMultiRegionKey(@RequestBody SynchronizeMultiRegionKeyRequest request);

    // =========================================================================
    // Cryptographic Operations
    // =========================================================================

    @PostMapping("/encrypt")
    @Operation(summary = "Encrypt", description = "Encrypts plaintext using a symmetric or asymmetric KMS key. Maximum 4096 bytes.")
    ResponseEntity<EncryptResponse> encrypt(@RequestBody EncryptRequest request);

    @PostMapping("/decrypt")
    @Operation(summary = "Decrypt", description = "Decrypts ciphertext that was encrypted under a KMS key.")
    ResponseEntity<DecryptResponse> decrypt(@RequestBody DecryptRequest request);

    @PostMapping("/reencrypt")
    @Operation(summary = "Re‑Encrypt", description = "Decrypts ciphertext under one KMS key and re‑encrypts under another, without exposing plaintext.")
    ResponseEntity<ReEncryptResponse> reEncrypt(@RequestBody ReEncryptRequest request);

    @PostMapping("/datakey/generate")
    @Operation(summary = "Generate Data Key", description = "Generates a symmetric data key for envelope encryption. Returns plaintext and encrypted copies.")
    ResponseEntity<GenerateDataKeyResponse> generateDataKey(@RequestBody GenerateDataKeyRequest request);

    @PostMapping("/datakey/generate-without-plaintext")
    @Operation(summary = "Generate Data Key (No Plaintext)", description = "Generates a data key and returns only the encrypted copy.")
    ResponseEntity<GenerateDataKeyWithoutPlaintextResponse> generateDataKeyWithoutPlaintext(@RequestBody GenerateDataKeyWithoutPlaintextRequest request);

    @PostMapping("/datakey/generate-pair")
    @Operation(summary = "Generate Data Key Pair", description = "Generates an asymmetric data key pair (public + private).")
    ResponseEntity<GenerateDataKeyPairResponse> generateDataKeyPair(@RequestBody GenerateDataKeyPairRequest request);

    @PostMapping("/datakey/generate-pair-without-plaintext")
    @Operation(summary = "Generate Data Key Pair (No Plaintext)", description = "Generates an asymmetric key pair and returns only the public key and encrypted private key.")
    ResponseEntity<GenerateDataKeyPairWithoutPlaintextResponse> generateDataKeyPairWithoutPlaintext(@RequestBody GenerateDataKeyPairWithoutPlaintextRequest request);

    @PostMapping("/sign")
    @Operation(summary = "Sign", description = "Generates a digital signature using an asymmetric KMS key.")
    ResponseEntity<SignResponse> sign(@RequestBody SignRequest request);

    @PostMapping("/verify")
    @Operation(summary = "Verify Signature", description = "Verifies a digital signature using an asymmetric KMS key.")
    ResponseEntity<VerifyResponse> verify(@RequestBody VerifyRequest request);

    @PostMapping("/mac/generate")
    @Operation(summary = "Generate MAC", description = "Generates a Message Authentication Code (MAC) using a symmetric HMAC KMS key.")
    ResponseEntity<GenerateMacResponse> generateMac(@RequestBody GenerateMacRequest request);

    @PostMapping("/mac/verify")
    @Operation(summary = "Verify MAC", description = "Verifies a Message Authentication Code (MAC).")
    ResponseEntity<VerifyMacResponse> verifyMac(@RequestBody VerifyMacRequest request);

    @GetMapping("/keys/public-key")
    @Operation(summary = "Get Public Key", description = "Returns the public key of an asymmetric KMS key (RSA or ECC).")
    ResponseEntity<GetPublicKeyResponse> getPublicKey(@RequestBody GetPublicKeyRequest request);

    @GetMapping("/keys/audit")
    ResponseEntity<AuditLogResponse> getAuditLogs(@RequestBody AuditLogRequest request);

    @GetMapping("/keys/usage-stats")
    ResponseEntity<KeyUsageStatsResponse> getKeyUsageStats(@RequestBody KeyUsageStatsRequest request);

    @PostMapping("/random")
    @Operation(summary = "Generate Random", description = "Generates cryptographically secure random bytes (up to 1024).")
    ResponseEntity<GenerateRandomResponse> generateRandom(@RequestBody GenerateRandomRequest request);

    // =========================================================================
    // Aliases
    // =========================================================================

    @GetMapping("/aliases")
    @Operation(summary = "List Aliases", description = "Lists all aliases in the account, including those for WAMS managed keys.")
    ResponseEntity<ListAliasesResponse> listAliases(@RequestBody ListAliasesRequest request);

    @GetMapping("/aliases/key")
    @Operation(summary = "List Aliases", description = "Lists all aliases in the account, including those for WAMS managed keys.")
    ResponseEntity<ListAliasesResponse> listAliasesForKey(@RequestBody ListAliasesForKeyRequest request);

    @PostMapping("/aliases")
    @Operation(summary = "Create Alias", description = "Creates a friendly name alias for a KMS key (must start with 'alias/').")
    ResponseEntity<CreateAliasResponse> createAlias(@RequestBody CreateAliasRequest request);

    @PutMapping("/aliases")
    @Operation(summary = "Update Alias", description = "Associates an existing alias with a different KMS key.")
    ResponseEntity<UpdateAliasResponse> updateAlias(@RequestBody UpdateAliasRequest request);

    @DeleteMapping("/aliases")
    @Operation(summary = "Delete Alias", description = "Deletes an alias. The underlying key is unaffected.")
    ResponseEntity<DeleteAliasResponse> deleteAlias(@RequestBody DeleteAliasRequest request);
    // =========================================================================
    // Tags
    // =========================================================================

    @GetMapping("/keys/tags")
    @Operation(summary = "List Resource Tags", description = "Lists all tags attached to a KMS key.")
    ResponseEntity<ListResourceTagsResponse> listResourceTags(@RequestBody ListResourceTagsRequest request);

    @PostMapping("/keys/tags")
    @Operation(summary = "Tag Resource", description = "Adds or updates tags on a KMS key (max 50 tags per key).")
    ResponseEntity<TagResourceResponse> tagResource(@RequestBody TagResourceRequest request);

    @DeleteMapping("/keys/tags")
    @Operation(summary = "Untag Resource", description = "Removes tags from a KMS key.")
    ResponseEntity<UntagResourceResponse> untagResource(@RequestBody UntagResourceRequest request);

    // =========================================================================
    // Key Policies & Grants
    // =========================================================================

    @PutMapping("/keys/policy")
    @Operation(summary = "Put Key Policy", description = "Attaches or updates a key policy document to a KMS key.")
    ResponseEntity<PutKeyPolicyResponse> putKeyPolicy(@RequestBody PutKeyPolicyRequest request);

    @GetMapping("/keys/policy")
    @Operation(summary = "Get Key Policy", description = "Returns the key policy document attached to a KMS key.")
    ResponseEntity<GetKeyPolicyResponse> getKeyPolicy(@RequestBody GetKeyPolicyRequest request);

    @GetMapping("/keys/policies")
    @Operation(summary = "List Key Policies", description = "Lists the names of all key policies attached to a KMS key.")
    ResponseEntity<ListKeyPoliciesResponse> listKeyPolicies(@RequestBody ListKeyPoliciesRequest request);

    @PostMapping("/keys/grants")
    @Operation(summary = "Create Grant", description = "Creates a grant that allows a principal to perform specific operations on a key.")
    ResponseEntity<CreateGrantResponse> createGrant(@RequestBody CreateGrantRequest request);

    @GetMapping("/keys/grants")
    @Operation(summary = "List Grants", description = "Lists all grants for a KMS key.")
    ResponseEntity<ListGrantsResponse> listGrants(@RequestBody ListGrantsRequest request);

    @DeleteMapping("/keys/grants")
    @Operation(summary = "Revoke Grant", description = "Revokes a grant, immediately removing its permissions.")
    ResponseEntity<RevokeGrantResponse> revokeGrant(@RequestBody RevokeGrantRequest request);

    @PostMapping("/grants/retire")
    @Operation(summary = "Retire Grant", description = "Retires a grant (can be called by grantee or key administrator).")
    ResponseEntity<RetireGrantResponse> retireGrant(@RequestBody RetireGrantRequest request);

    @GetMapping("/grants/retirable")
    @Operation(summary = "List Retirable Grants", description = "Lists grants that can be retired by a given principal.")
    ResponseEntity<ListRetirableGrantsResponse> listRetirableGrants(@RequestBody ListRetirableGrantsRequest request);

    // =========================================================================
    // BYOK (Import Key Material)
    // =========================================================================

    @PostMapping("/keys/import-parameters")
    @Operation(summary = "Get Parameters for Import", description = "Returns a public key and import token needed to import your own key material (BYOK).")
    ResponseEntity<GetParametersForImportResponse> getParametersForImport(@RequestBody GetParametersForImportRequest request);

    @PostMapping("/keys/import")
    @Operation(summary = "Import Key Material", description = "Imports your own key material into a KMS key created with origin = EXTERNAL.")
    ResponseEntity<ImportKeyMaterialResponse> importKeyMaterial(@RequestBody ImportKeyMaterialRequest request);

    @DeleteMapping("/keys/key-material")
    @Operation(summary = "Delete Imported Key Material", description = "Deletes imported key material, rendering the key unusable.")
    ResponseEntity<DeleteImportedKeyMaterialResponse> deleteImportedKeyMaterial(@RequestBody DeleteImportedKeyMaterialRequest request);

    // =========================================================================
    // Custom Key Stores
    // =========================================================================

    @PostMapping("/custom-key-stores")
    @Operation(summary = "Create Custom Key Store", description = "Creates a custom key store backed by a CloudHSM cluster or external proxy.")
    ResponseEntity<CreateCustomKeyStoreResponse> createCustomKeyStore(@RequestBody CreateCustomKeyStoreRequest request);

    @GetMapping("/custom-key-stores")
    @Operation(summary = "Describe Custom Key Store", description = "Returns metadata about a custom key store, including connection status.")
    ResponseEntity<DescribeCustomKeyStoreResponse> describeCustomKeyStore(@RequestBody DescribeCustomKeyStoreRequest request);

    @PatchMapping("/custom-key-stores")
    @Operation(summary = "Update Custom Key Store", description = "Updates configuration properties of a custom key store.")
    ResponseEntity<UpdateCustomKeyStoreResponse> updateCustomKeyStore(@RequestBody UpdateCustomKeyStoreRequest request);

    @DeleteMapping("/custom-key-stores")
    @Operation(summary = "Delete Custom Key Store", description = "Deletes a custom key store (must be disconnected and contain no keys).")
    ResponseEntity<DeleteCustomKeyStoreResponse> deleteCustomKeyStore(@RequestBody DeleteCustomKeyStoreRequest request);

    @PostMapping("/custom-key-stores/connect")
    @Operation(summary = "Connect Custom Key Store", description = "Connects a custom key store to its underlying hardware.")
    ResponseEntity<ConnectCustomKeyStoreResponse> connectCustomKeyStore(@RequestBody ConnectCustomKeyStoreRequest request);

    @PostMapping("/custom-key-stores/disconnect")
    @Operation(summary = "Disconnect Custom Key Store", description = "Disconnects a custom key store from its underlying hardware.")
    ResponseEntity<DisconnectCustomKeyStoreResponse> disconnectCustomKeyStore(@RequestBody DisconnectCustomKeyStoreRequest request);

    @GetMapping("/custom-key-stores/list")
    @Operation(summary = "List Custom Key Stores", description = "Lists all custom key stores in the account (paginated).")
    ResponseEntity<ListCustomKeyStoresResponse> listCustomKeyStores(@RequestBody ListCustomKeyStoresRequest request);

    @PostMapping("/keys/validate")
    @Operation(summary = "Validate Key", description = "Validates the specified key.")
    ResponseEntity<ValidateKeyResponse> validateKey(@RequestBody ValidateKeyRequest request);
}