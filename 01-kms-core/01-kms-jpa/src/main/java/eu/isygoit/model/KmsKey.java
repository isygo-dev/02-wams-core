package eu.isygoit.model;

import eu.isygoit.constants.TenantConstants;
import eu.isygoit.enums.IEnumKeyPurpose;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.model.jakarta.AuditableEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * The type Kms Key.
 * Represents a cryptographic key in the KMS system (AWS KMS compliant)
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = "T_KMS_KEY",
        uniqueConstraints = {
                @UniqueConstraint(name = "UC_KMS_KEY_TENANT_ID",
                        columnNames = {"TENANT", "KEY_ID"})
        },
        indexes = {
                @Index(name = "IDX_KMS_KEY_TENANT_STATUS", columnList = "TENANT,STATUS"),
                @Index(name = "IDX_KMS_KEY_ALIAS", columnList = "TENANT,ALIAS"),
                @Index(name = "IDX_KMS_KEY_IMPORTED", columnList = "IMPORTED"),
                @Index(name = "IDX_KMS_KEY_EXPIRATION", columnList = "EXPIRATION_DATE"),
                @Index(name = "IDX_KMS_KEY_KEY_STORE", columnList = "TENANT,KEY_STORE_ID")
        })
public class KmsKey extends AuditableEntity<Long> implements ITenantAssignable {

    @Id
    @SequenceGenerator(name = "kms_key_seq", sequenceName = "kms_key_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kms_key_seq")
    @Column(name = "ID", updatable = false, nullable = false)
    private Long id;

    @ColumnDefault("'" + TenantConstants.DEFAULT_TENANT_NAME + "'")
    @Column(name = "TENANT", length = 100, updatable = false, nullable = false)
    private String tenant;

    @Column(name = "KEY_ID", updatable = false, nullable = false)
    private Long keyId; // UUID format

    @Column(name = "KEY_ARN", length = 255, nullable = false)
    private String keyArn; // Amazon Resource Name

    @Column(name = "KEY_STORE_ID", length = 255)
    private String keyStoreId; // Custom key store ID

    @Enumerated(EnumType.STRING)
    @Column(name = "KEY_SPEC", length = 50, nullable = false)
    private IEnumKeySpec.Types keySpec; // AES_256, RSA_2048, EC_P256

    @Enumerated(EnumType.STRING)
    @Column(name = "KEY_PURPOSE", length = 50, nullable = false)
    private IEnumKeyPurpose.Types keyPurpose; // ENCRYPT_DECRYPT, SIGN_VERIFY

    @Column(name = "KEY_ALIAS", length = 255)
    private String keyAlias;

    @Column(name = "DESCRIPTION", length = 1024)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 50, nullable = false)
    private IEnumKeyStatus.Types status; // ENABLED, DISABLED, PENDING_DELETION

    @Column(name = "CURRENT_VERSION_ID", length = 255)
    private String currentVersionId;

    @Column(name = "ROTATION_ENABLED", nullable = false)
    @ColumnDefault("false")
    private Boolean rotationEnabled = false;

    @Column(name = "ROTATION_PERIOD_DAYS")
    private Integer rotationPeriodDays;

    @Column(name = "LAST_ROTATION_DATE")
    private LocalDateTime lastRotationDate;

    @Column(name = "DELETION_DATE")
    private LocalDateTime deletionDate;

    @Column(name = "PENDING_DELETION_WINDOW_DAYS")
    private Integer pendingDeletionWindowDays;

    @Lob
    @Column(name = "KEY_MATERIAL", nullable = false)
    private byte[] keyMaterial; // Encrypted key material

    @Column(name = "KEY_MATERIAL_ENCRYPTED", nullable = false)
    @ColumnDefault("true")
    private Boolean keyMaterialEncrypted = true;

    @Column(name = "IMPORTED", nullable = false)
    @ColumnDefault("false")
    private Boolean imported = false; // Whether key material was imported

    @Column(name = "IMPORT_DATE")
    private LocalDateTime importDate; // Date when key material was imported

    @Column(name = "EXPIRATION_DATE")
    private LocalDateTime expirationDate; // Expiration date for imported key material

    @Column(name = "CREATION_DATE", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @Column(name = "ORIGIN", length = 50)
    @ColumnDefault("'AWS_KMS'")
    private String origin; // AWS_KMS, EXTERNAL, CLOUDHSM

    @Column(name = "TAGS")
    private String tags; // JSON format for metadata tags

    @Column(name = "ENABLED", nullable = false)
    @ColumnDefault("false")
    private Boolean enabled = false; // Whether the key is enabled for use

    @Column(name = "MULTI_REGION", nullable = false)
    @ColumnDefault("false")
    private Boolean multiRegion = false; // Whether this is a multi-region key

    @Column(name = "PRIMARY_REGION", length = 100)
    private String primaryRegion; // Primary region for multi-region key

    @Column(name = "REPLICA_REGIONS", length = 500)
    private String replicaRegions; // Comma-separated list of replica regions (JSON format)

    // Helper methods
    public boolean isExpired() {
        return expirationDate != null && expirationDate.isBefore(LocalDateTime.now());
    }

    public boolean isPendingDeletion() {
        return IEnumKeyStatus.Types.PENDING_DELETION.equals(status);
    }

    public boolean isEnabled() {
        return IEnumKeyStatus.Types.ENABLED.equals(status);
    }

    public boolean isDisabled() {
        return IEnumKeyStatus.Types.DISABLED.equals(status);
    }
}