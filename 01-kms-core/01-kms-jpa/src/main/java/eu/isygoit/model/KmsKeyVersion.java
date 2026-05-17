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
                @UniqueConstraint(name = SchemaUcConstantName.UC_KMS_KEY_VERSION_ID,
                        columnNames = {SchemaColumnConstantName.C_TENANT, SchemaColumnConstantName.C_VERSION_ID})
        },
        indexes = {
                @Index(name = SchemaIndexConstantName.IDX_KMS_KEY_VERSION_TENANT_KEY, columnList = SchemaColumnConstantName.C_TENANT + "," + SchemaColumnConstantName.C_KEY_ID),
                @Index(name = SchemaIndexConstantName.IDX_KMS_KEY_VERSION_STATUS, columnList = SchemaColumnConstantName.C_STATUS)
        })
public class KmsKeyVersion extends AuditableEntity<Long> implements ITenantAssignable {

    @Id
    @SequenceGenerator(name = "kms_key_version_seq", sequenceName = "kms_key_version_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kms_key_version_seq")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    //@Convert(converter = LowerCaseConverter.class)
    @ColumnDefault("'" + TenantConstants.DEFAULT_TENANT_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_TENANT, length = SchemaConstantSize.TENANT, updatable = false, nullable = false)
    private String tenant;

    @Column(name = SchemaColumnConstantName.C_KEY_ID, nullable = false)
    private String keyId;

    @Column(name = SchemaColumnConstantName.C_VERSION_ID, length = 255, nullable = false)
    private String versionId;

    @Column(name = SchemaColumnConstantName.C_STATUS, length = 50, nullable = false)
    private IEnumKeyStatus.Types keyStatus;

    @Lob
    @Column(name = SchemaColumnConstantName.C_KEY_MATERIAL, nullable = false)
    private byte[] keyMaterial;

    @Column(name = SchemaColumnConstantName.C_DEACTIVATION_DATE)
    private LocalDateTime deactivationDate;

    @Column(name = SchemaColumnConstantName.C_EXPIRY_DATE)
    private LocalDateTime validTo;

    @Column(name = SchemaColumnConstantName.C_SIGNING_ALGORITHM, length = 50)
    private String signingAlgorithm;

    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_EXPIRATION_MODEL, length = 50)
    private IEnumKeyExpirationModel.Types expirationModel;

    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_ORIGIN, length = 50)
    private IEnumKeyOrigin.Types origin;
}