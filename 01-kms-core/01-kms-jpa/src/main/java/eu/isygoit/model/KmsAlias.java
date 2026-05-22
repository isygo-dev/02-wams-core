package eu.isygoit.model;

import eu.isygoit.constants.TenantConstants;
import eu.isygoit.model.jakarta.AuditableEntity;
import eu.isygoit.model.schema.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/**
 * The type Kms alias.
 * Represents an alias for a KMS key (WAMS KMS-compliant)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.T_KMS_ALIAS,
        uniqueConstraints = {
                @UniqueConstraint(name = SchemaUcConstantName.UC_UK_ALIAS_NAME_TENANT,
                        columnNames = {SchemaColumnConstantName.C_TENANT, SchemaColumnConstantName.C_ALIAS_NAME})
        },
        indexes = {
                @Index(name = SchemaIndexConstantName.IDX_ALIAS_TENANT, columnList = SchemaColumnConstantName.C_TENANT),
                @Index(name = SchemaIndexConstantName.IDX_ALIAS_KEY_ID, columnList = SchemaColumnConstantName.C_KEY_ID),
                @Index(name = SchemaIndexConstantName.IDX_ALIAS_NAME, columnList = SchemaColumnConstantName.C_ALIAS_NAME)
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

    @Pattern(regexp = "^alias:.*", message = "alias.name.must.start.with.alias")
    @Column(name = SchemaColumnConstantName.C_ALIAS_NAME, nullable = false, length = 256)
    private String aliasName;

    @Column(name = SchemaColumnConstantName.C_KEY_ID, nullable = false)
    private String targetKeyId;

    @Builder.Default
    @ColumnDefault("false")
    @Column(name = SchemaColumnConstantName.C_IS_PRIMARY, nullable = false)
    private Boolean primaryKey = Boolean.FALSE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(
                    name = SchemaColumnConstantName.C_TENANT,
                    referencedColumnName = SchemaColumnConstantName.C_TENANT,
                    insertable = false,
                    updatable = false
            ),
            @JoinColumn(
                    name = SchemaColumnConstantName.C_KEY_ID,
                    referencedColumnName = SchemaColumnConstantName.C_KEY_ID,
                    insertable = false,
                    updatable = false
            )
    })
    private KmsKey key;
}