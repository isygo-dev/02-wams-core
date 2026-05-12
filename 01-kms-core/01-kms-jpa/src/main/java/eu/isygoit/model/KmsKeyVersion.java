package eu.isygoit.model;

import eu.isygoit.constants.TenantConstants;
import eu.isygoit.enums.IEnumKeyExpirationModel;
import eu.isygoit.enums.IEnumKeyOrigin;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.model.jakarta.AuditableEntity;
import eu.isygoit.model.schema.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

/**
 * The type Kms Key Version.
 * Represents a version of a cryptographic key in the KMS system
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.T_KMS_KEY_VERSION,
        uniqueConstraints = {
                @UniqueConstraint(name = "UC_KMS_KEY_VERSION_ID", columnNames = {"TENANT", "VERSION_ID"}),
                @UniqueConstraint(name = "UC_KMS_KEY_VERSION_NUMBER", columnNames = {"TENANT", "KEY_ID", "VERSION_NUMBER"})
        },
        indexes = {
                @Index(name = "IDX_KMS_KEY_VERSION_TENANT_KEY", columnList = "TENANT,KEY_ID"),
                @Index(name = "IDX_KMS_KEY_VERSION_STATUS", columnList = "STATUS"),
                @Index(name = "IDX_KMS_KEY_VERSION_ROTATION_DATE", columnList = "ROTATION_DATE")
        })
public class KmsKeyVersion extends AuditableEntity<Long> implements ITenantAssignable {

    @Id
    @SequenceGenerator(name = "kms_key_version_seq", sequenceName = "kms_key_version_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kms_key_version_seq")
    @Column(name = "ID", updatable = false, nullable = false)
    private Long id;

    //@Convert(converter = LowerCaseConverter.class)
    @ColumnDefault("'" + TenantConstants.DEFAULT_TENANT_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_TENANT, length = SchemaConstantSize.TENANT, updatable = false, nullable = false)
    private String tenant;

    @Column(name = "KEY_ID", nullable = false)
    private String keyId;

    @Column(name = "VERSION_ID", length = 255, nullable = false)
    private String versionId;

    @Column(name = "VERSION_NUMBER")
    private Integer versionNumber;

    @Column(name = "STATUS", length = 50, nullable = false)
    private IEnumKeyStatus.Types keyStatus; // ACTIVE, DEPRECATED, PENDING_DELETION

    @Lob
    @Column(name = "KEY_MATERIAL", nullable = false)
    private byte[] keyMaterial;

    @Column(name = "CREATION_DATE", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @Column(name = "ACTIVATION_DATE")
    private LocalDateTime activationDate;

    @Column(name = "DEACTIVATION_DATE")
    private LocalDateTime deactivationDate;

    @Column(name = "ROTATION_DATE")
    private LocalDateTime rotationDate;

    @Column(name = "EXPIRY_DATE")
    private LocalDateTime expiryDate;

    @Column(name = "DESCRIPTION", length = 1024)
    private String description;

    @Column(name = "SIGNING_ALGORITHM", length = 50)
    private String signingAlgorithm;

    @Column(name = "HASH_ALGORITHM", length = 50)
    private String hashAlgorithm;

    @Enumerated(EnumType.STRING)
    @Column(name = "EXPIRATION_MODEL", length = 50)
    private IEnumKeyExpirationModel.Types expirationModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "ORIGIN", length = 50)
    private IEnumKeyOrigin.Types origin;
}