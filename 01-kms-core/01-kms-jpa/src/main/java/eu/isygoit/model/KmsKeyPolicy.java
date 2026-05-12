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

/**
 * The type Kms Key Policy.
 * Represents access control policies for a specific key
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = SchemaTableConstantName.T_KMS_KEY_POLICY,
        uniqueConstraints = {
                @UniqueConstraint(name = "UC_KMS_KEY_POLICY_KEY_ID",
                        columnNames = {"TENANT", "KEY_ID"})
        },
        indexes = {
                @Index(name = "IDX_KMS_KEY_POLICY_KEY_ID", columnList = "KEY_ID")
        })
public class KmsKeyPolicy extends AuditableEntity<Long> implements ITenantAssignable {

    @Id
    @SequenceGenerator(name = "kms_key_policy_seq", sequenceName = "kms_key_policy_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kms_key_policy_seq")
    @Column(name = "ID", updatable = false, nullable = false)
    private Long id;

    //@Convert(converter = LowerCaseConverter.class)
    @ColumnDefault("'" + TenantConstants.DEFAULT_TENANT_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_TENANT, length = SchemaConstantSize.TENANT, updatable = false, nullable = false)
    private String tenant;

    @Column(name = "KEY_ID", length = 255, updatable = false, nullable = false)
    private String keyId;

    @Lob
    @Column(name = "POLICY_DOCUMENT", nullable = false)
    private String policyDocument; // JSON format IAM policy

    @Column(name = "POLICY_VERSION", length = 50)
    private String policyVersion; // Version of policy format (2012-10-17, etc.)

    @Column(name = "DESCRIPTION", length = 1024)
    private String description;

}

