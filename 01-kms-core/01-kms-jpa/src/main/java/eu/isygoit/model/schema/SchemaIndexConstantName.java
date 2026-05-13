package eu.isygoit.model.schema;

/**
 * The interface Schema index constant name.
 * Declares all @Index constant names used in KMS entities.
 */
public interface SchemaIndexConstantName {

    // ============================================================================
    // KMS KEY VERSION INDEXES
    // ============================================================================

    /**
     * The constant IDX_KMS_KEY_VERSION_TENANT_KEY.
     * Index on tenant and key_id columns for KmsKeyVersion.
     */
    String IDX_KMS_KEY_VERSION_TENANT_KEY = "IDX_KMS_KEY_VERSION_TENANT_KEY";

    /**
     * The constant IDX_KMS_KEY_VERSION_STATUS.
     * Index on status column for KmsKeyVersion.
     */
    String IDX_KMS_KEY_VERSION_STATUS = "IDX_KMS_KEY_VERSION_STATUS";

    /**
     * The constant IDX_KMS_KEY_VERSION_ROTATION_DATE.
     * Index on rotation_date column for KmsKeyVersion.
     */
    String IDX_KMS_KEY_VERSION_ROTATION_DATE = "IDX_KMS_KEY_VERSION_ROTATION_DATE";

    // ============================================================================
    // KMS KEY INDEXES
    // ============================================================================

    /**
     * The constant IDX_KMS_KEY_TENANT_STATUS.
     * Composite index on tenant and status columns for KmsKey.
     */
    String IDX_KMS_KEY_TENANT_STATUS = "IDX_KMS_KEY_TENANT_STATUS";

    /**
     * The constant IDX_KMS_KEY_ALIAS.
     * Composite index on tenant and key_alias columns for KmsKey.
     */
    String IDX_KMS_KEY_ALIAS = "IDX_KMS_KEY_ALIAS";

    /**
     * The constant IDX_KMS_KEY_IMPORTED.
     * Index on imported column for KmsKey.
     */
    String IDX_KMS_KEY_IMPORTED = "IDX_KMS_KEY_IMPORTED";

    /**
     * The constant IDX_KMS_KEY_EXPIRATION.
     * Index on expiration_date column for KmsKey.
     */
    String IDX_KMS_KEY_EXPIRATION = "IDX_KMS_KEY_EXPIRATION";

    /**
     * The constant IDX_KMS_KEY_KEY_STORE.
     * Composite index on tenant and key_store_id columns for KmsKey.
     */
    String IDX_KMS_KEY_KEY_STORE = "IDX_KMS_KEY_KEY_STORE";

    /**
     * The constant IDX_KMS_KEY_PRIMARY_KEY_ID.
     * Index on primary_key_id column for KmsKey.
     */
    String IDX_KMS_KEY_PRIMARY_KEY_ID = "IDX_KMS_KEY_PRIMARY_KEY_ID";

    /**
     * The constant IDX_KMS_KEY_REGION.
     * Index on region column for KmsKey.
     */
    String IDX_KMS_KEY_REGION = "IDX_KMS_KEY_REGION";

    // ============================================================================
    // KMS KEY GRANT INDEXES
    // ============================================================================

    /**
     * The constant IDX_KMS_KEY_GRANT_KEY_ID.
     * Index on key_id column for KmsKeyGrant.
     */
    String IDX_KMS_KEY_GRANT_KEY_ID = "IDX_KMS_KEY_GRANT_KEY_ID";

    /**
     * The constant IDX_KMS_KEY_GRANT_PRINCIPAL.
     * Index on principal column for KmsKeyGrant.
     */
    String IDX_KMS_KEY_GRANT_PRINCIPAL = "IDX_KMS_KEY_GRANT_PRINCIPAL";

    /**
     * The constant IDX_KMS_KEY_GRANT_STATUS.
     * Index on status column for KmsKeyGrant.
     */
    String IDX_KMS_KEY_GRANT_STATUS = "IDX_KMS_KEY_GRANT_STATUS";

    // ============================================================================
    // KMS KEY POLICY INDEXES
    // ============================================================================

    /**
     * The constant IDX_KMS_KEY_POLICY_KEY_ID.
     * Index on key_id column for KmsKeyPolicy.
     */
    String IDX_KMS_KEY_POLICY_KEY_ID = "IDX_KMS_KEY_POLICY_KEY_ID";

    // ============================================================================
    // KMS AUDIT LOG INDEXES
    // ============================================================================

    /**
     * The constant IDX_KMS_AUDIT_LOG_KEY_ID.
     * Index on key_id column for KmsAuditLog.
     */
    String IDX_KMS_AUDIT_LOG_KEY_ID = "IDX_KMS_AUDIT_LOG_KEY_ID";

    /**
     * The constant IDX_KMS_AUDIT_LOG_TENANT_ACTION.
     * Composite index on tenant and action columns for KmsAuditLog.
     */
    String IDX_KMS_AUDIT_LOG_TENANT_ACTION = "IDX_KMS_AUDIT_LOG_TENANT_ACTION";

    /**
     * The constant IDX_KMS_AUDIT_LOG_TIMESTAMP.
     * Index on timestamp column for KmsAuditLog.
     */
    String IDX_KMS_AUDIT_LOG_TIMESTAMP = "IDX_KMS_AUDIT_LOG_TIMESTAMP";

    /**
     * The constant IDX_KMS_AUDIT_LOG_PRINCIPAL.
     * Index on principal column for KmsAuditLog.
     */
    String IDX_KMS_AUDIT_LOG_PRINCIPAL = "IDX_KMS_AUDIT_LOG_PRINCIPAL";

    // ============================================================================
    // KMS ALIAS INDEXES
    // ============================================================================

    /**
     * The constant IDX_ALIAS_TENANT.
     * Index on tenant column for KmsAlias.
     */
    String IDX_ALIAS_TENANT = "IDX_ALIAS_TENANT";

    /**
     * The constant IDX_ALIAS_KEY_ID.
     * Index on key_id column for KmsAlias.
     */
    String IDX_ALIAS_KEY_ID = "IDX_ALIAS_KEY_ID";

    /**
     * The constant IDX_ALIAS_NAME.
     * Index on alias_name column for KmsAlias.
     */
    String IDX_ALIAS_NAME = "IDX_ALIAS_NAME";

    // ============================================================================
    // KMS TAG INDEXES
    // ============================================================================

    /**
     * The constant IDX_TAG_TENANT.
     * Index on tenant column for KmsTag.
     */
    String IDX_TAG_TENANT = "IDX_TAG_TENANT";

    /**
     * The constant IDX_TAG_KEY_ID.
     * Index on key_id column for KmsTag.
     */
    String IDX_TAG_KEY_ID = "IDX_TAG_KEY_ID";

    /**
     * The constant IDX_TAG_TAG_KEY.
     * Index on tag_key column for KmsTag.
     */
    String IDX_TAG_TAG_KEY = "IDX_TAG_TAG_KEY";

    /**
     * The constant IDX_TAG_TENANT_KEY.
     * Composite index on tenant and key_id columns for KmsTag.
     */
    String IDX_TAG_TENANT_KEY = "IDX_TAG_TENANT_KEY";

    // ============================================================================
    // CUSTOM KEY STORE INDEXES
    // ============================================================================

    /**
     * The constant IDX_CUSTOM_KEY_STORE_TENANT.
     * Index on tenant column for CustomKeyStore.
     */
    String IDX_CUSTOM_KEY_STORE_TENANT = "IDX_CUSTOM_KEY_STORE_TENANT";

    /**
     * The constant IDX_CUSTOM_KEY_STORE_STATUS.
     * Index on status column for CustomKeyStore.
     */
    String IDX_CUSTOM_KEY_STORE_STATUS = "IDX_CUSTOM_KEY_STORE_STATUS";

    /**
     * The constant IDX_CUSTOM_KEY_STORE_TYPE.
     * Index on store_type column for CustomKeyStore.
     */
    String IDX_CUSTOM_KEY_STORE_TYPE = "IDX_CUSTOM_KEY_STORE_TYPE";
}

