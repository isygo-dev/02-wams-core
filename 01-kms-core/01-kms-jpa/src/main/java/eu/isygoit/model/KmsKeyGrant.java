package eu.isygoit.model;

import eu.isygoit.constants.TenantConstants;
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

/**
 * The type Kms Key Grant.
 * Represents access grants for a key to specific principals
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = SchemaTableConstantName.T_KMS_KEY_GRANT,
        uniqueConstraints = {
                @UniqueConstraint(name = SchemaUcConstantName.UC_KMS_KEY_GRANT_ID,
                        columnNames = {SchemaColumnConstantName.C_TENANT, SchemaColumnConstantName.C_GRANT_ID})
        },
        indexes = {
                @Index(name = SchemaIndexConstantName.IDX_KMS_KEY_GRANT_KEY_ID, columnList = SchemaColumnConstantName.C_KEY_ID),
                @Index(name = SchemaIndexConstantName.IDX_KMS_KEY_GRANT_PRINCIPAL, columnList = SchemaColumnConstantName.C_PRINCIPAL),
                @Index(name = SchemaIndexConstantName.IDX_KMS_KEY_GRANT_STATUS, columnList = SchemaColumnConstantName.C_STATUS)
        })
public class KmsKeyGrant extends AuditableEntity<Long> implements ITenantAssignable {

    @Id
    @SequenceGenerator(name = "kms_key_grant_seq", sequenceName = "kms_key_grant_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kms_key_grant_seq")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    //@Convert(converter = LowerCaseConverter.class)
    @ColumnDefault("'" + TenantConstants.DEFAULT_TENANT_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_TENANT, length = SchemaConstantSize.TENANT, updatable = false, nullable = false)
    private String tenant;

    @Column(name = SchemaColumnConstantName.C_KEY_ID, length = 255, updatable = false, nullable = false)
    private String keyId;

    @Column(name = SchemaColumnConstantName.C_GRANT_ID, length = 255, updatable = false, nullable = false)
    private String grantId;

    @Column(name = SchemaColumnConstantName.C_PRINCIPAL, length = 255, nullable = false)
    private String principal; // WRN or account ID

    @Column(name = SchemaColumnConstantName.C_OPERATIONS, length = 1000, nullable = false)
    private String operations; // JSON array of allowed operations: encrypt, decrypt, sign, verify, etc.

    @Column(name = SchemaColumnConstantName.C_CONSTRAINTS)
    private String constraints; // JSON format for encryption context constraints

    @Column(name = SchemaColumnConstantName.C_NAME, length = 255)
    private String name;

    @Column(name = SchemaColumnConstantName.C_STATUS, length = 50, nullable = false)
    @ColumnDefault("'ACTIVE'")
    private String status; // ACTIVE, REVOKED

    @Column(name = SchemaColumnConstantName.C_CREATION_DATE, nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @Column(name = SchemaColumnConstantName.C_REVOCATION_DATE)
    private LocalDateTime revocationDate;

}

