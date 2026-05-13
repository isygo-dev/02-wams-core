package eu.isygoit.model;

import eu.isygoit.constants.TenantConstants;
import eu.isygoit.enums.*;
import eu.isygoit.model.jakarta.AuditableEntity;
import eu.isygoit.model.schema.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = SchemaTableConstantName.T_KMS_KEY,
        uniqueConstraints = {
                @UniqueConstraint(name = SchemaUcConstantName.UC_UC_KMS_KEY_TENANT_ID,
                        columnNames = {SchemaColumnConstantName.C_TENANT, SchemaColumnConstantName.C_KEY_ID})
        },
        indexes = {
                @Index(name = SchemaIndexConstantName.IDX_KMS_KEY_TENANT_STATUS, columnList = SchemaColumnConstantName.C_TENANT + "," + SchemaColumnConstantName.C_STATUS),
                @Index(name = SchemaIndexConstantName.IDX_KMS_KEY_ALIAS, columnList = SchemaColumnConstantName.C_TENANT + "," + SchemaColumnConstantName.C_KEY_ALIAS),
                @Index(name = SchemaIndexConstantName.IDX_KMS_KEY_IMPORTED, columnList = SchemaColumnConstantName.C_IMPORTED),
                @Index(name = SchemaIndexConstantName.IDX_KMS_KEY_EXPIRATION, columnList = SchemaColumnConstantName.C_EXPIRATION_DATE),
                @Index(name = SchemaIndexConstantName.IDX_KMS_KEY_KEY_STORE, columnList = SchemaColumnConstantName.C_TENANT + "," + SchemaColumnConstantName.C_KEY_STORE_ID),
                @Index(name = SchemaIndexConstantName.IDX_KMS_KEY_PRIMARY_KEY_ID, columnList = SchemaColumnConstantName.C_PRIMARY_KEY_ID),
                @Index(name = SchemaIndexConstantName.IDX_KMS_KEY_REGION, columnList = SchemaColumnConstantName.C_REGION)
        })
public class KmsKey extends AuditableEntity<Long> implements ITenantAssignable {

    @Id
    @SequenceGenerator(name = "kms_key_seq", sequenceName = "kms_key_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kms_key_seq")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    //@Convert(converter = LowerCaseConverter.class)
    @ColumnDefault("'" + TenantConstants.DEFAULT_TENANT_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_TENANT, length = SchemaConstantSize.TENANT, updatable = false, nullable = false)
    private String tenant;

    @Column(name = SchemaColumnConstantName.C_KEY_ID, updatable = false, nullable = false, length = 255)
    private String keyId; // UUID format

    @Column(name = SchemaColumnConstantName.C_KEY_WRN, length = 255, nullable = false)
    private String keyWrn; // Amazon Resource Name

    @Column(name = SchemaColumnConstantName.C_REGION, length = 100)
    private String region; // WAMS region where this key resides

    @Column(name = SchemaColumnConstantName.C_KEY_STORE_ID, length = 255)
    private Long keyStoreId; // Custom key store ID

    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_KEY_SPEC, length = 50, nullable = false)
    private IEnumKeySpec.Types keySpec; // AES_256, RSA_2048, EC_P256

    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_KEY_PURPOSE, length = 50, nullable = false)
    private IEnumKeyUsage.Types keyUsage; // ENCRYPT_DECRYPT, SIGN_VERIFY

    @Column(name = SchemaColumnConstantName.C_KEY_ALIAS, length = 255)
    private String keyAlias;

    @Column(name = SchemaColumnConstantName.C_DESCRIPTION, length = 1024)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_STATUS, length = 50, nullable = false)
    private IEnumKeyStatus.Types keyStatus; // ENABLED, DISABLED, PENDING_DELETION

    @Column(name = SchemaColumnConstantName.C_CURRENT_VERSION_ID, length = 255)
    private String currentVersionId;

    @Column(name = SchemaColumnConstantName.C_ROTATION_ENABLED, nullable = false)
    @ColumnDefault("false")
    private Boolean rotationEnabled = false;

    @Column(name = SchemaColumnConstantName.C_ROTATION_PERIOD_DAYS)
    private Integer rotationPeriodDays;

    @Column(name = SchemaColumnConstantName.C_LAST_ROTATION_DATE)
    private LocalDateTime lastRotationDate;

    @Column(name = SchemaColumnConstantName.C_DELETION_DATE)
    private LocalDateTime deletionDate;

    @Column(name = SchemaColumnConstantName.C_PENDING_DELETION_WINDOW_DAYS)
    private Integer pendingDeletionWindowDays;

    @Lob
    @Column(name = SchemaColumnConstantName.C_KEY_MATERIAL, nullable = false)
    private byte[] keyMaterial; // Encrypted key material

    @Column(name = SchemaColumnConstantName.C_KEY_MATERIAL_ENCRYPTED, nullable = false)
    @ColumnDefault("true")
    private Boolean keyMaterialEncrypted = true;

    @Column(name = SchemaColumnConstantName.C_IMPORTED, nullable = false)
    @ColumnDefault("false")
    private Boolean imported = false; // Whether key material was imported

    @Column(name = SchemaColumnConstantName.C_IMPORT_DATE)
    private LocalDateTime importDate; // Date when key material was imported

    @Column(name = SchemaColumnConstantName.C_EXPIRATION_DATE)
    private LocalDateTime expirationDate; // Expiration date for imported key material

    @Column(name = SchemaColumnConstantName.C_CREATION_DATE, nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_ORIGIN, length = 50)
    @ColumnDefault("'WAMS_KMS'")
    private IEnumKeyOrigin.Types origin; // WAMS_KMS, EXTERNAL, CLOUDHSM

    @Column(name = SchemaColumnConstantName.C_TAGS, columnDefinition = "TEXT")
    private String tags; // JSON format for metadata tags

    @Column(name = SchemaColumnConstantName.C_MULTI_REGION, nullable = false)
    @ColumnDefault("false")
    private Boolean multiRegion = false; // Whether this key is part of a multi‑region setup

    @Column(name = SchemaColumnConstantName.C_PRIMARY_KEY_ID, length = 255)
    private String primaryKeyId; // For replica keys – points to the primary key's KEY_ID

    @Column(name = SchemaColumnConstantName.C_PRIMARY_REGION, length = 100)
    private String primaryRegion; // Primary region (only for the primary key)

    @Column(name = SchemaColumnConstantName.C_REPLICA_REGIONS, length = 500)
    private String replicaRegions; // Comma‑separated list of replica regions (only for the primary key)

    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_EXPIRATION_MODEL, length = 50)
    private IEnumKeyExpirationModel.Types expirationModel;

    // Helper methods
    public boolean isExpired() {
        return expirationDate != null && expirationDate.isBefore(LocalDateTime.now());
    }

    public boolean isPendingDeletion() {
        return IEnumKeyStatus.Types.PENDING_DELETION.equals(keyStatus);
    }

    public boolean isEnabled() {
        return IEnumKeyStatus.Types.ENABLED.equals(keyStatus);
    }

    public boolean isDisabled() {
        return IEnumKeyStatus.Types.DISABLED.equals(keyStatus);
    }

    public boolean isPrimaryKey() {
        return Boolean.TRUE.equals(multiRegion) && primaryKeyId == null;
    }

    public boolean isReplicaKey() {
        return Boolean.TRUE.equals(multiRegion) && primaryKeyId != null;
    }
}