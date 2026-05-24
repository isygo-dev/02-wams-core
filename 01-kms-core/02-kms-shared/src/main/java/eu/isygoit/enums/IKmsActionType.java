package eu.isygoit.enums;

/**
 * WAMS KMS action types aligned with WAMS KMS API operations.
 * <p>
 * Reference:
 * https://docs.wams.amazon.com/kms/latest/APIReference/Welcome.html
 */
public interface IKmsActionType {

    /**
     * The constant STR_ENUM_SIZE.
     */
    int STR_ENUM_SIZE = 64;

    /**
     * WAMS KMS action types.
     */
    enum Types implements IEnum {

        // =========================================================================
        // Cryptographic operations
        // =========================================================================

        ENCRYPT("Encrypt"),
        DECRYPT("Decrypt"),
        RE_ENCRYPT("ReEncrypt"),
        SIGN("Sign"),
        VERIFY("Verify"),
        GENERATE_MAC("GenerateMac"),
        VERIFY_MAC("VerifyMac"),

        GENERATE_DATA_KEY("GenerateDataKey"),
        GENERATE_DATA_KEY_WITHOUT_PLAINTEXT("GenerateDataKeyWithoutPlaintext"),
        GENERATE_DATA_KEY_PAIR("GenerateDataKeyPair"),
        GENERATE_DATA_KEY_PAIR_WITHOUT_PLAINTEXT("GenerateDataKeyPairWithoutPlaintext"),
        GENERATE_RANDOM("GenerateRandom"),
        VALIDATE_KEY("ValidateKey"),

        // =========================================================================
        // Key management
        // =========================================================================

        CREATE_KEY("CreateKey"),
        DESCRIBE_KEY("DescribeKey"),

        ENABLE_KEY("EnableKey"),
        DISABLE_KEY("DisableKey"),

        ENABLE_KEY_ROTATION("EnableKeyRotation"),
        DISABLE_KEY_ROTATION("DisableKeyRotation"),
        GET_KEY_ROTATION_STATUS("GetKeyRotationStatus"),
        ROTATE_KEY_ON_DEMAND("RotateKeyOnDemand"),

        SCHEDULE_KEY_DELETION("ScheduleKeyDeletion"),
        CANCEL_KEY_DELETION("CancelKeyDeletion"),

        UPDATE_KEY_DESCRIPTION("UpdateKeyDescription"),

        // =========================================================================
        // Aliases
        // =========================================================================

        CREATE_ALIAS("CreateAlias"),
        UPDATE_ALIAS("UpdateAlias"),
        DELETE_ALIAS("DeleteAlias"),
        LIST_ALIASES("ListAliases"),
        LIST_ALIASES_FOR_KEY("ListAliasesForKey"),

        // =========================================================================
        // Tags
        // =========================================================================

        TAG_RESOURCE("TagResource"),
        UNTAG_RESOURCE("UntagResource"),
        LIST_RESOURCE_TAGS("ListResourceTags"),

        // =========================================================================
        // Policies
        // =========================================================================

        GET_KEY_POLICY("GetKeyPolicy"),
        PUT_KEY_POLICY("PutKeyPolicy"),
        LIST_KEY_POLICIES("ListKeyPolicies"),

        // =========================================================================
        // Grants
        // =========================================================================

        CREATE_GRANT("CreateGrant"),
        LIST_GRANTS("ListGrants"),
        LIST_RETIRED_GRANTS("ListRetiredGrants"),
        REVOKE_GRANT("RevokeGrant"),
        RETIRE_GRANT("RetireGrant"),

        // =========================================================================
        // Public keys
        // =========================================================================

        GET_PUBLIC_KEY("GetPublicKey"),

        // =========================================================================
        // Key material import
        // =========================================================================

        GET_PARAMETERS_FOR_IMPORT("GetParametersForImport"),
        IMPORT_KEY_MATERIAL("ImportKeyMaterial"),
        DELETE_IMPORTED_KEY_MATERIAL("DeleteImportedKeyMaterial"),

        // =========================================================================
        // Custom key stores
        // =========================================================================

        CREATE_CUSTOM_KEY_STORE("CreateCustomKeyStore"),
        UPDATE_CUSTOM_KEY_STORE("UpdateCustomKeyStore"),
        DELETE_CUSTOM_KEY_STORE("DeleteCustomKeyStore"),

        CONNECT_CUSTOM_KEY_STORE("ConnectCustomKeyStore"),
        DISCONNECT_CUSTOM_KEY_STORE("DisconnectCustomKeyStore"),

        DESCRIBE_CUSTOM_KEY_STORES("DescribeCustomKeyStores"),
        LIST_CUSTOM_KEY_STORE("ListCustomKeyStores"),

        // =========================================================================
        // Multi-region keys
        // =========================================================================

        REPLICATE_KEY("ReplicateKey"),
        UPDATE_PRIMARY_REGION("UpdatePrimaryRegion"),

        // =========================================================================
        // Key discovery / metadata
        // =========================================================================

        LIST_KEY_ROTATIONS("ListKeyRotations"),
        LIST_KEYS("ListKeys"),
        DESCRIBE_CUSTOM_KEY_STORE("DescribeCustomKeyStore"),
        SYNCHRONIZE_MULTI_REGION_KEY("SynchronizeMultiRegionKey"),
        GET_ACTIVE_VERSION("GetActiveVersion"),
        UPDATE_KEY_ROTATION("UpdateKeyRotation"),
        LIST_KEY_VERSIONS("ListKeyVersions"),
        GET_KEY_VERSION("GetKeyVersion"),
        DELETE_KEY("DeleteKey"),
        DISABLE_KEY_VERSION("DisableKeyVersion"),
        ENABLE_KEY_VERSION("EnableKeyVersion");

        private final String meaning;

        Types(String meaning) {
            this.meaning = meaning;
        }

        @Override
        public String meaning() {
            return meaning;
        }
    }
}