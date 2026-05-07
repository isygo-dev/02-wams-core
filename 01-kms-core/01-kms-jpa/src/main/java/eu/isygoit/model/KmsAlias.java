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
 * The type Kms alias.
 * Represents an alias for a KMS key (AWS KMS-compliant)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "KMS_ALIAS",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_ALIAS_NAME_TENANT", columnNames = {"TENANT", "ALIAS_NAME"})
        },
        indexes = {
                @Index(name = "IDX_ALIAS_TENANT", columnList = "TENANT"),
                @Index(name = "IDX_ALIAS_KEY_ID", columnList = "KEY_ID"),
                @Index(name = "IDX_ALIAS_NAME", columnList = "ALIAS_NAME")
        })
public class KmsAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TENANT", nullable = false, length = 100)
    private String tenant;

    @Column(name = "ALIAS_NAME", nullable = false, length = 256)
    private String aliasName;

    @Column(name = "KEY_ID", nullable = false)
    private Long keyId;

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