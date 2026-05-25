package eu.isygoit.model;

import eu.isygoit.constants.TenantConstants;
import eu.isygoit.enums.*;
import eu.isygoit.model.jakarta.AuditableEntity;
import eu.isygoit.model.schema.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * KMS Key entity representing a Customer Master Key (CMK) in the Key Management Service.
 * <p>
 * This entity maps to the WAMS KMS key model and stores all cryptographic key metadata,
 * key material (encrypted), rotation settings, multi‑region configuration, and import details.
 * </p>
 *
 * @see <a href="https://docs.wams.amazon.com/kms/latest/APIReference/API_KeyMetadata.html">WAMS KeyMetadata</a>
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = SchemaTableConstantName.T_KMS_KEY,
        uniqueConstraints = {
                @UniqueConstraint(name = SchemaUcConstantName.UC_KMS_KEY_TENANT_ID,
                        columnNames = {SchemaColumnConstantName.C_TENANT, SchemaColumnConstantName.C_KEY_ID})
        },
        indexes = {
                @Index(name = SchemaIndexConstantName.IDX_KMS_KEY_TENANT_STATUS, columnList = SchemaColumnConstantName.C_TENANT + "," + SchemaColumnConstantName.C_STATUS),
                @Index(name = SchemaIndexConstantName.IDX_KMS_KEY_EXPIRATION, columnList = SchemaColumnConstantName.C_EXPIRATION_DATE),
                @Index(name = SchemaIndexConstantName.IDX_KMS_KEY_KEY_STORE, columnList = SchemaColumnConstantName.C_TENANT + "," + SchemaColumnConstantName.C_KEY_STORE_ID),
                @Index(name = SchemaIndexConstantName.IDX_KMS_KEY_PRIMARY_KEY_ID, columnList = SchemaColumnConstantName.C_PRIMARY_KEY_ID),
                @Index(name = SchemaIndexConstantName.IDX_KMS_KEY_REGION, columnList = SchemaColumnConstantName.C_REGION)
        })
public class KmsKey extends AuditableEntity<Long> implements ITenantAssignable {

    // =========================================================================
    // Primary identifiers
    // =========================================================================

    /**
     * Internal primary key (technical ID). Not exposed to customers.
     */
    @Id
    @SequenceGenerator(name = "kms_key_seq", sequenceName = "kms_key_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kms_key_seq")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    /**
     * Tenant (account/isolated namespace) that owns this key.
     * Defaults to "default" tenant.
     * <p>
     * Maps to WAMS: AccountId (via tenant abstraction).
     * </p>
     */
    @Builder.Default
    @ColumnDefault("'" + TenantConstants.DEFAULT_TENANT_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_TENANT, length = SchemaConstantSize.TENANT, updatable = false, nullable = false)
    private String tenant = TenantConstants.DEFAULT_TENANT_NAME;

    /**
     * Unique identifier of the KMS key (UUID format).
     * Immutable after creation.
     * <p>
     * WAMS equivalent: KeyId
     * </p>
     */
    @Column(name = SchemaColumnConstantName.C_KEY_ID, updatable = false, nullable = false, length = 255)
    private String keyId;

    /**
     * WAMS Resource Name (similar to WAMS WRN).
     * Format example: "wrn:wams:kms:us-east-1:123456789012:key/1234abcd-..."
     * <p>
     * WAMS equivalent: WRN
     * </p>
     */
    @Column(name = SchemaColumnConstantName.C_KEY_WRN, length = 255, nullable = false)
    private String keyWrn;

    /**
     * WAMS region where this key resides (e.g., "us-east-1", "eu-west-3").
     * For multi‑region keys, this is the region of this specific replica or the primary.
     * <p>
     * WAMS equivalent: region
     * </p>
     */
    @Column(name = SchemaColumnConstantName.C_REGION, length = 100)
    private String region;

    // =========================================================================
    // Key specification and usage
    // =========================================================================

    /**
     * Cryptographic key specification (type and length).
     * <p>
     * Possible values:
     * <ul>
     *   <li>SYMMETRIC_DEFAULT – AES‑256‑GCM (symmetric encryption)</li>
     *   <li>RSA_2048, RSA_3072, RSA_4096 – asymmetric RSA (encryption or signing)</li>
     *   <li>ECC_NIST_P256, ECC_NIST_P384, ECC_NIST_P521 – elliptic curve (signing)</li>
     *   <li>ECC_SECG_P256K1 – Bitcoin curve (signing)</li>
     *   <li>HMAC_224, HMAC_256, HMAC_384, HMAC_512 – symmetric HMAC</li>
     *   <li>SM2 – Chinese national standard</li>
     * </ul>
     * </p>
     * <p>
     * WAMS equivalent: KeySpec (in WAMS API) or CustomerMasterKeySpec.
     * </p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_KEY_SPEC, length = 50, nullable = false)
    private IEnumKeySpec.Types keySpec;

    /**
     * Intended usage of the key.
     * <p>
     * Possible values:
     * <ul>
     *   <li>ENCRYPT_DECRYPT – for symmetric encryption/decryption</li>
     *   <li>SIGN_VERIFY – for digital signatures (asymmetric keys)</li>
     *   <li>GENERATE_VERIFY_MAC – for HMAC operations</li>
     * </ul>
     * </p>
     * <p>
     * WAMS equivalent: KeyUsage.
     * </p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_KEY_PURPOSE, length = 50, nullable = false)
    private IEnumKeyUsage.Types keyUsage;

    // =========================================================================
    // Identification and description
    // =========================================================================

    /**
     * Friendly alias for the key (e.g., "alias:my-key").
     * Aliases are separate resources in WAMS but denormalized here for quick access.
     * <p>
     * Note: A key can have multiple aliases; this field stores the primary/default alias.
     * </p>
     * <p>
     * WAMS equivalent: part of Alias API, not part of KeyMetadata.
     * </p>
     */
    @Pattern(regexp = "^alias:.*", message = "alias.name.must.start.with.alias")
    @Column(name = SchemaColumnConstantName.C_KEY_ALIAS, length = 255)
    private String primaryKeyAlias;

    /**
     * Human‑readable description of the key (max 1024 characters).
     * <p>
     * WAMS equivalent: Description.
     * </p>
     */
    @Column(name = SchemaColumnConstantName.C_DESCRIPTION, length = 1024)
    private String description;

    // =========================================================================
    // State and lifecycle
    // =========================================================================

    /**
     * Current state (status) of the key.
     * <p>
     * Possible values:
     * <ul>
     *   <li>ENABLED – normal operational state</li>
     *   <li>DISABLED – cannot be used for cryptographic operations</li>
     *   <li>PENDING_DELETION – scheduled for deletion (waiting period)</li>
     *   <li>PENDING_IMPORT – awaiting key material import (for BYOK)</li>
     * </ul>
     * </p>
     * <p>
     * WAMS equivalent: KeyState.
     * </p>
     */
    @Builder.Default
    @ColumnDefault("'ENABLED'")
    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_STATUS, length = 50, nullable = false)
    private IEnumKeyStatus.Types keyStatus = IEnumKeyStatus.Types.ENABLED;

    /**
     * Version ID of the current active key material.
     * Rotations create new versions; the primary version is stored here.
     * <p>
     * WAMS equivalent: CurrentVersion (from ListKeyVersions).
     * </p>
     */
    @Column(name = SchemaColumnConstantName.C_CURRENT_VERSION_ID, length = 255)
    private String currentVersionId;

    // =========================================================================
    // Rotation settings
    // =========================================================================

    /**
     * Whether automatic key rotation is enabled (symmetric keys only).
     * When true, KMS rotates the key material every `rotationPeriodInDays`.
     * <p>
     * WAMS equivalent: RotationEnabled (from GetKeyRotationStatus).
     * </p>
     */
    @Builder.Default
    @ColumnDefault("false")
    @Column(name = SchemaColumnConstantName.C_ROTATION_ENABLED, nullable = false)
    private Boolean rotationEnabled = Boolean.FALSE;

    /**
     * Rotation period in days (default 365). Only meaningful if `rotationEnabled` is true.
     * Minimum 90 days, maximum 3650 days (10 years).
     * <p>
     * WAMS equivalent: RotationPeriodInDays.
     * </p>
     */
    @Column(name = SchemaColumnConstantName.C_ROTATION_PERIOD_DAYS)
    private Integer rotationPeriodInDays;

    /**
     * Date and time of the last automatic or manual rotation.
     * <p>
     * WAMS equivalent: LastRotationDate.
     * </p>
     */
    @Column(name = SchemaColumnConstantName.C_LAST_ROTATION_DATE)
    private LocalDateTime lastRotationDate;

    // =========================================================================
    // Deletion (schedule and pending)
    // =========================================================================

    /**
     * Date and time when the key is scheduled to be permanently deleted.
     * Only set when `keyStatus = PENDING_DELETION`.
     * <p>
     * WAMS equivalent: DeletionDate.
     * </p>
     */
    @Column(name = SchemaColumnConstantName.C_DELETION_DATE)
    private LocalDateTime deletionDate;

    /**
     * Waiting period in days before deletion (7‑30 days). Set during `scheduleKeyDeletion`.
     * <p>
     * WAMS equivalent: PendingWindowInDays.
     * </p>
     */
    @Column(name = SchemaColumnConstantName.C_PENDING_DELETION_WINDOW_DAYS)
    private Integer pendingDeletionWindowDays;

    // =========================================================================
    // Key material (encrypted at rest)
    // =========================================================================

    /**
     * Encrypted key material (the actual cryptographic key).
     * Always encrypted using envelope encryption with a master key.
     * Never stored in plaintext.
     * <p>
     * WAMS equivalent: key material (not directly exposed by API).
     * </p>
     */
    @Lob
    @Column(name = SchemaColumnConstantName.C_KEY_MATERIAL, nullable = false)
    private byte[] keyMaterial;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = SchemaColumnConstantName.C_PUBLIC_KEY)
    private byte[] publicKey; // X.509 encoded public key (null for symmetric/HMAC keys)

    /**
     * Date when the key material was imported (for BYOK). Null for keys generated by KMS.
     * <p>
     * WAMS equivalent: not directly exposed, but used internally.
     * </p>
     */
    @Column(name = SchemaColumnConstantName.C_IMPORT_DATE)
    private LocalDateTime importDate;

    /**
     * Expiration date for imported key material. After this date the key becomes unusable.
     * Only applies to keys with origin = EXTERNAL (BYOK).
     * <p>
     * WAMS equivalent: ValidTo.
     * </p>
     */
    @Column(name = SchemaColumnConstantName.C_EXPIRATION_DATE)
    private LocalDateTime validTo;

    // =========================================================================
    // Origin and custom key stores
    // =========================================================================

    /**
     * Origin of the key material.
     * <p>
     * Possible values:
     * <ul>
     *   <li>WAMS_KMS – generated by KMS (default)</li>
     *   <li>EXTERNAL – imported by customer (BYOK)</li>
     *   <li>CLOUDHSM – generated in a CloudHSM cluster</li>
     *   <li>EXTERNAL_KEY_STORE – backed by an external key store proxy</li>
     * </ul>
     * </p>
     * <p>
     * WAMS equivalent: Origin.
     * </p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_ORIGIN, length = 50)
    @ColumnDefault("'WAMS_KMS'")
    private IEnumKeyOrigin.Types origin = IEnumKeyOrigin.Types.WAMS_KMS;

    /**
     * Expiration model for imported key material.
     * <p>
     * Possible values:
     * <ul>
     *   <li>KEY_MATERIAL_EXPIRES – material expires at `validTo`</li>
     *   <li>KEY_MATERIAL_DOES_NOT_EXPIRE – no expiration</li>
     * </ul>
     * </p>
     * <p>
     * WAMS equivalent: ExpirationModel (from GetParametersForImport).
     * </p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_EXPIRATION_MODEL, length = 50)
    private IEnumKeyExpirationModel.Types expirationModel;

    /**
     * Custom key store backing this key (CloudHSM or XKS). Null for standard KMS keys.
     * <p>
     * WAMS equivalent: CustomKeyStoreId.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns(foreignKey = @ForeignKey(name = SchemaFkConstantName.FK_KMS_KEY_REF_KMS_KEY_STORE),
            value = {
                    @JoinColumn(
                            name = SchemaColumnConstantName.C_TENANT,
                            referencedColumnName = SchemaColumnConstantName.C_TENANT,
                            insertable = false,
                            updatable = false
                    ),
                    @JoinColumn(
                            name = SchemaColumnConstantName.C_KEY_STORE_ID,
                            referencedColumnName = SchemaColumnConstantName.C_ID,
                            insertable = false,
                            updatable = false
                    )
            })
    private KmsCustomKeyStore customKeyStore;

    /**
     * Foreign key column for `customKeyStore` (denormalized).
     */
    @Column(name = SchemaColumnConstantName.C_KEY_STORE_ID, insertable = false, updatable = false, length = 255)
    private Long keyStoreId;

    // =========================================================================
    // Multi‑region key configuration
    // =========================================================================

    /**
     * Whether this key is part of a multi‑region setup.
     * <p>
     * WAMS equivalent: MultiRegion (boolean).
     * </p>
     */
    @Builder.Default
    @ColumnDefault("false")
    @Column(name = SchemaColumnConstantName.C_MULTI_REGION, nullable = false)
    private Boolean multiRegion = Boolean.FALSE;

    /**
     * For replica keys: points to the primary key's `keyId`.
     * For primary keys: null.
     * <p>
     * WAMS equivalent: part of MultiRegionConfiguration (PrimaryKeyId).
     * </p>
     */
    @Column(name = SchemaColumnConstantName.C_PRIMARY_KEY_ID, length = 255)
    private String primaryKeyId;

    /**
     * For primary keys: the region where the primary key resides.
     * For replica keys: not used (the primary region is the region of the primary key).
     * <p>
     * WAMS equivalent: part of MultiRegionConfiguration (PrimaryRegion).
     * </p>
     */
    @Column(name = SchemaColumnConstantName.C_PRIMARY_REGION, length = 100)
    private String primaryRegion;

    /**
     * For primary keys: comma‑separated list of replica region names.
     * Example: "us-west-2,eu-west-1,ap-southeast-1"
     * <p>
     * WAMS equivalent: part of MultiRegionConfiguration (ReplicaRegions).
     * </p>
     */
    @Column(name = SchemaColumnConstantName.C_REPLICA_REGIONS, length = 500)
    private String replicaRegions;

    // =========================================================================
    // Helper methods (derived fields)
    // =========================================================================

    /**
     * Returns true if the key is expired (only applicable for imported keys with `validTo`).
     */
    public boolean isExpired() {
        return validTo != null && validTo.isBefore(LocalDateTime.now());
    }

    /**
     * Returns true if the key is in `PENDING_DELETION` state.
     */
    public boolean isPendingDeletion() {
        return IEnumKeyStatus.Types.PENDING_DELETION.equals(keyStatus);
    }

    /**
     * Returns true if the key is in `ENABLED` state.
     */
    public boolean isEnabled() {
        return IEnumKeyStatus.Types.ENABLED.equals(keyStatus);
    }

    /**
     * Returns true if the key is in `DISABLED` state.
     */
    public boolean isDisabled() {
        return IEnumKeyStatus.Types.DISABLED.equals(keyStatus);
    }

    /**
     * Returns true if this is a multi‑region primary key.
     */
    public boolean isPrimaryKey() {
        return Boolean.TRUE.equals(multiRegion) && primaryKeyId == null;
    }

    /**
     * Returns true if this is a multi‑region replica key.
     */
    public boolean isReplicaKey() {
        return Boolean.TRUE.equals(multiRegion) && primaryKeyId != null;
    }

    // =========================================================================
    // Calculated fields (transient – not stored in database)
    // =========================================================================

    /**
     * List of encryption algorithms supported by this key.
     * Derived from `keySpec` and `keyUsage`.
     * <p>
     * For SYMMETRIC_DEFAULT: ["SYMMETRIC_DEFAULT"]
     * For RSA keys: ["RSAES_OAEP_SHA_1", "RSAES_OAEP_SHA_256", "RSAES_PKCS1_V1_5"]
     * For others: empty list.
     * </p>
     * <p>
     * WAMS equivalent: EncryptionAlgorithmSpecs.
     * </p>
     */
    @Transient
    public List<String> getEncryptionAlgorithmSpecs() {
        List<String> specs = new ArrayList<>();
        if (keySpec == null) return specs;
        if (keySpec == IEnumKeySpec.Types.SYMMETRIC_DEFAULT
                && keyUsage == IEnumKeyUsage.Types.ENCRYPT_DECRYPT) {
            specs.add("SYMMETRIC_DEFAULT");
        } else if (keySpec.name().startsWith("RSA") && keyUsage == IEnumKeyUsage.Types.ENCRYPT_DECRYPT) {
            specs.add("RSAES_OAEP_SHA_1");
            specs.add("RSAES_OAEP_SHA_256");
            specs.add("RSAES_PKCS1_V1_5");
        }
        return specs;
    }

    /**
     * Key manager type: "WAMS" (or "WAMS") or "CUSTOMER".
     * Derived from `origin` and custom key store presence.
     * <p>
     * WAMS equivalent: KeyManager.
     * </p>
     */
    @Transient
    public String getKeyManager() {
        if (origin == IEnumKeyOrigin.Types.EXTERNAL
                || origin == IEnumKeyOrigin.Types.WAMS_CLOUDHSM
                || origin == IEnumKeyOrigin.Types.EXTERNAL_KEY_STORE) {
            return "CUSTOMER";
        }
        return "WAMS";
    }

    /**
     * Structured multi‑region configuration object.
     * Returns a map with fields: multiRegion, primaryKeyId, primaryRegion, replicaRegions (list).
     * <p>
     * WAMS equivalent: MultiRegionConfiguration.
     * </p>
     */
    @Transient
    public Map<String, Object> getMultiRegionConfiguration() {
        if (Boolean.FALSE.equals(multiRegion)) return null;
        return Map.of(
                "multiRegion", multiRegion,
                "primaryKeyId", primaryKeyId != null ? primaryKeyId : keyId,
                "primaryRegion", primaryRegion,
                "replicaRegions", replicaRegions != null ? List.of(replicaRegions.split(",")) : List.of()
        );
    }

    /**
     * Next scheduled rotation date (if rotation enabled and lastRotationDate known).
     * <p>
     * WAMS equivalent: NextRotationDate (from GetKeyRotationStatus).
     * </p>
     */
    @Transient
    public LocalDateTime getNextRotationDate() {
        if (Boolean.TRUE.equals(rotationEnabled) && lastRotationDate != null && rotationPeriodInDays != null) {
            return lastRotationDate.plusDays(rotationPeriodInDays);
        }
        return null;
    }

    /**
     * Returns true if the key material was imported (BYOK).
     * <p>
     * WAMS equivalent: origin == EXTERNAL.
     * </p>
     */
    @Transient
    public Boolean getImported() {
        return IEnumKeyOrigin.Types.EXTERNAL.equals(origin);
    }
}