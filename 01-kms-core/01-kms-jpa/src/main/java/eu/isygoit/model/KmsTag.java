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

/**
 * The type Kms tag.
 * Represents a tag for a KMS key (WAMS KMS-compliant)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.T_KMS_TAG,
        uniqueConstraints = {
                @UniqueConstraint(name = SchemaUcConstantName.UC_UK_TAG_KEY_TENANT_KEY, 
                        columnNames = {SchemaColumnConstantName.C_TENANT, SchemaColumnConstantName.C_KEY_ID, SchemaColumnConstantName.C_TAG_KEY})
        },
        indexes = {
                @Index(name = SchemaIndexConstantName.IDX_TAG_TENANT, columnList = SchemaColumnConstantName.C_TENANT),
                @Index(name = SchemaIndexConstantName.IDX_TAG_KEY_ID, columnList = SchemaColumnConstantName.C_KEY_ID),
                @Index(name = SchemaIndexConstantName.IDX_TAG_TAG_KEY, columnList = SchemaColumnConstantName.C_TAG_KEY),
                @Index(name = SchemaIndexConstantName.IDX_TAG_TENANT_KEY, columnList = SchemaColumnConstantName.C_TENANT + "," + SchemaColumnConstantName.C_KEY_ID)
        })
public class KmsTag extends AuditableEntity<Long> implements ITenantAssignable {

    @Id
    @SequenceGenerator(name = "kms_tag_seq", sequenceName = "kms_tag_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kms_tag_seq")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    //@Convert(converter = LowerCaseConverter.class)
    @ColumnDefault("'" + TenantConstants.DEFAULT_TENANT_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_TENANT, length = SchemaConstantSize.TENANT, updatable = false, nullable = false)
    private String tenant;

    @Column(name = SchemaColumnConstantName.C_KEY_ID, nullable = false)
    private String keyId;

    @Column(name = SchemaColumnConstantName.C_TAG_KEY, nullable = false, length = 128)
    private String tagKey;

    @Column(name = SchemaColumnConstantName.C_TAG_VALUE, length = 256)
    private String tagValue;
}