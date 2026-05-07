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
 * The type Kms Key Version.
 * Represents a specific version of a cryptographic key
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = "T_KMS_KEY_VERSION",
        uniqueConstraints = {
                @UniqueConstraint(name = "UC_KMS_KEY_VERSION_ID",
                        columnNames = {"TENANT", "KEY_ID", "VERSION_ID"})
        },
        indexes = {
                @Index(name = "IDX_KMS_KEY_VERSION_KEY_ID", columnList = "TENANT,KEY_ID"),
                @Index(name = "IDX_KMS_KEY_VERSION_STATUS", columnList = "KEY_ID,STATUS")
        })
public class KmsKeyVersion extends AuditableEntity<Long> implements ITenantAssignable {

    @Id
    @SequenceGenerator(name = "kms_key_version_seq", sequenceName = "kms_key_version_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kms_key_version_seq")
    @Column(name = "ID", updatable = false, nullable = false)
    private Long id;

    @Column(name = "TENANT", length = 100, updatable = false, nullable = false)
    @ColumnDefault("'DEFAULT'")
    private String tenant;

    @Column(name = "KEY_ID", length = 255, updatable = false, nullable = false)
    private Long keyId;

    @Column(name = "VERSION_ID", length = 255, updatable = false, nullable = false)
    private String versionId;

    @Column(name = "STATUS", length = 50, nullable = false)
    private String status; // ACTIVE, INACTIVE

    @Lob
    @Column(name = "KEY_MATERIAL", nullable = false)
    private byte[] keyMaterial; // Encrypted key material for this version

    @Column(name = "CREATION_DATE", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @Column(name = "ACTIVATION_DATE")
    private LocalDateTime activationDate;

    @Column(name = "DEACTIVATION_DATE")
    private LocalDateTime deactivationDate;

    @Column(name = "ROTATION_DATE")
    private LocalDateTime rotationDate;

}

