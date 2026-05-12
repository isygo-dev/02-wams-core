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
 * The type Kms tag.
 * Represents a tag for a KMS key (WAMS KMS-compliant)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.KMS_TAG,
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_TAG_KEY_TENANT_KEY", columnNames = {"TENANT", "KEY_ID", "TAG_KEY"})
        },
        indexes = {
                @Index(name = "IDX_TAG_TENANT", columnList = "TENANT"),
                @Index(name = "IDX_TAG_KEY_ID", columnList = "KEY_ID"),
                @Index(name = "IDX_TAG_TAG_KEY", columnList = "TAG_KEY"),
                @Index(name = "IDX_TAG_TENANT_KEY", columnList = "TENANT, KEY_ID")
        })
public class KmsTag extends AuditableEntity<Long> implements ITenantAssignable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    //@Convert(converter = LowerCaseConverter.class)
    @ColumnDefault("'" + TenantConstants.DEFAULT_TENANT_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_TENANT, length = SchemaConstantSize.TENANT, updatable = false, nullable = false)
    private String tenant;

    @Column(name = "KEY_ID", nullable = false)
    private String keyId;

    @Column(name = "TAG_KEY", nullable = false, length = 128)
    private String tagKey;

    @Column(name = "TAG_VALUE", length = 256)
    private String tagValue;

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