package eu.isygoit.model;

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
 * The type Kms Key Grant.
 * Represents access grants for a key to specific principals
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = "T_KMS_KEY_GRANT",
        uniqueConstraints = {
                @UniqueConstraint(name = "UC_KMS_KEY_GRANT_ID",
                        columnNames = {"TENANT", "GRANT_ID"})
        },
        indexes = {
                @Index(name = "IDX_KMS_KEY_GRANT_KEY_ID", columnList = "KEY_ID"),
                @Index(name = "IDX_KMS_KEY_GRANT_PRINCIPAL", columnList = "PRINCIPAL"),
                @Index(name = "IDX_KMS_KEY_GRANT_STATUS", columnList = "STATUS")
        })
public class KmsKeyGrant extends AuditableEntity<Long> implements ITenantAssignable {

    @Id
    @SequenceGenerator(name = "kms_key_grant_seq", sequenceName = "kms_key_grant_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kms_key_grant_seq")
    @Column(name = "ID", updatable = false, nullable = false)
    private Long id;

    @Column(name = "TENANT", length = 100, updatable = false, nullable = false)
    @ColumnDefault("'DEFAULT'")
    private String tenant;

    @Column(name = "KEY_ID", length = 255, updatable = false, nullable = false)
    private Long keyId;

    @Column(name = "GRANT_ID", length = 255, updatable = false, nullable = false)
    private String grantId;

    @Column(name = "PRINCIPAL", length = 255, nullable = false)
    private String principal; // ARN or account ID

    @Column(name = "OPERATIONS", length = 1000, nullable = false)
    private String operations; // JSON array of allowed operations: encrypt, decrypt, sign, verify, etc.

    @Column(name = "CONSTRAINTS")
    private String constraints; // JSON format for encryption context constraints

    @Column(name = "NAME", length = 255)
    private String name;

    @Column(name = "STATUS", length = 50, nullable = false)
    @ColumnDefault("'ACTIVE'")
    private String status; // ACTIVE, REVOKED

    @Column(name = "CREATION_DATE", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @Column(name = "REVOCATION_DATE")
    private LocalDateTime revocationDate;

}

