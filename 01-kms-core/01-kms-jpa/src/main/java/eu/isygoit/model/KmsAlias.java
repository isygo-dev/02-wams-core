package eu.isygoit.model;

import eu.isygoit.constants.TenantConstants;
import eu.isygoit.model.jakarta.AuditableEntity;
import eu.isygoit.model.schema.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * The type Kms alias.
 * Represents an alias for a KMS key (WAMS KMS-compliant)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.KMS_ALIAS,
        uniqueConstraints = {
                @UniqueConstraint(name = SchemaUcConstantName.UC_UK_ALIAS_NAME_TENANT,
                        columnNames = {SchemaColumnConstantName.C_TENANT, SchemaColumnConstantName.C_ALIAS_NAME})
        },
        indexes = {
                @Index(name = "IDX_ALIAS_TENANT", columnList = SchemaColumnConstantName.C_TENANT),
                @Index(name = "IDX_ALIAS_KEY_ID", columnList = SchemaColumnConstantName.C_KEY_ID),
                @Index(name = "IDX_ALIAS_NAME", columnList = SchemaColumnConstantName.C_ALIAS_NAME)
        })
public class KmsAlias extends AuditableEntity<Long> implements ITenantAssignable {

    @Id
    @SequenceGenerator(name = "kms_alias_seq", sequenceName = "kms_alias_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kms_alias_seq")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    //@Convert(converter = LowerCaseConverter.class)
    @ColumnDefault("'" + TenantConstants.DEFAULT_TENANT_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_TENANT, length = SchemaConstantSize.TENANT, updatable = false, nullable = false)
    private String tenant;

    @Column(name = SchemaColumnConstantName.C_ALIAS_NAME, nullable = false, length = 256)
    private String aliasName;

    @Column(name = SchemaColumnConstantName.C_KEY_ID, nullable = false)
    private String keyId;

    @Column(name = SchemaColumnConstantName.C_DESCRIPTION, length = 1024)
    private String description;

    @CreationTimestamp
    @Column(name = SchemaColumnConstantName.C_CREATION_DATE, nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @UpdateTimestamp
    @Column(name = SchemaColumnConstantName.C_LAST_UPDATED_DATE)
    private LocalDateTime lastUpdatedDate;

    @Version
    @Column(name = SchemaColumnConstantName.C_VERSION)
    private Long version;
}