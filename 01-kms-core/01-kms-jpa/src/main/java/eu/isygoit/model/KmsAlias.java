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
                @UniqueConstraint(name = "UK_ALIAS_NAME_TENANT", columnNames = {"TENANT", "ALIAS_NAME"})
        },
        indexes = {
                @Index(name = "IDX_ALIAS_TENANT", columnList = "TENANT"),
                @Index(name = "IDX_ALIAS_KEY_ID", columnList = "KEY_ID"),
                @Index(name = "IDX_ALIAS_NAME", columnList = "ALIAS_NAME")
        })
public class KmsAlias extends AuditableEntity<Long> implements ITenantAssignable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    //@Convert(converter = LowerCaseConverter.class)
    @ColumnDefault("'" + TenantConstants.DEFAULT_TENANT_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_TENANT, length = SchemaConstantSize.TENANT, updatable = false, nullable = false)
    private String tenant;

    @Column(name = "ALIAS_NAME", nullable = false, length = 256)
    private String aliasName;

    @Column(name = "KEY_ID", nullable = false)
    private String keyId;

    @Column(name = "DESCRIPTION", length = 1024)
    private String description;

    @CreationTimestamp
    @Column(name = "CREATION_DATE", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @UpdateTimestamp
    @Column(name = "LAST_UPDATED_DATE")
    private LocalDateTime lastUpdatedDate;

    @Version
    @Column(name = "VERSION")
    private Long version;
}