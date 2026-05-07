package eu.isygoit.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * The type Kms tag.
 * Represents a tag for a KMS key (AWS KMS-compliant)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "KMS_TAG",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_TAG_KEY_TENANT_KEY", columnNames = {"TENANT", "KEY_ID", "TAG_KEY"})
        },
        indexes = {
                @Index(name = "IDX_TAG_TENANT", columnList = "TENANT"),
                @Index(name = "IDX_TAG_KEY_ID", columnList = "KEY_ID"),
                @Index(name = "IDX_TAG_TAG_KEY", columnList = "TAG_KEY"),
                @Index(name = "IDX_TAG_TENANT_KEY", columnList = "TENANT, KEY_ID")
        })
public class KmsTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TENANT", nullable = false, length = 100)
    private String tenant;

    @Column(name = "KEY_ID", nullable = false)
    private Long keyId;

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