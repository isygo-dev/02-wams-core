package eu.isygoit.model;

import eu.isygoit.constants.TenantConstants;
import eu.isygoit.enums.IEnumKeyExpirationModel;
import eu.isygoit.enums.IEnumKeyOrigin;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.model.jakarta.AuditableEntity;
import eu.isygoit.model.schema.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * The type Kms Key Version.
 * Represents a version of a cryptographic key in the KMS system
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = SchemaTableConstantName.T_KMS_KEY_VERSION,
        uniqueConstraints = {
                @UniqueConstraint(name = SchemaUcConstantName.UC_KMS_KEY_VERSION_ID,
                        columnNames = {SchemaColumnConstantName.C_TENANT, SchemaColumnConstantName.C_VERSION_ID})
        },
        indexes = {
                @Index(name = SchemaIndexConstantName.IDX_KMS_KEY_VERSION_TENANT_KEY, columnList = SchemaColumnConstantName.C_TENANT + "," + SchemaColumnConstantName.C_KEY_ID),
                @Index(name = SchemaIndexConstantName.IDX_KMS_KEY_VERSION_STATUS, columnList = SchemaColumnConstantName.C_STATUS)
        })
public class KmsKeyVersion extends AuditableEntity<Long> implements ITenantAssignable {

    @Id
    @SequenceGenerator(name = "kms_key_version_seq", sequenceName = "kms_key_version_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kms_key_version_seq")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    //@Convert(converter = LowerCaseConverter.class)
    @ColumnDefault("'" + TenantConstants.DEFAULT_TENANT_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_TENANT, length = SchemaConstantSize.TENANT, updatable = false, nullable = false)
    private String tenant;

    @Column(name = SchemaColumnConstantName.C_KEY_ID, nullable = false)
    private String keyId;

    @Column(name = SchemaColumnConstantName.C_VERSION_ID, length = 255, nullable = false)
    private String versionId;

    @Builder.Default
    @ColumnDefault("'ENABLED'")
    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_STATUS, length = 50, nullable = false)
    private IEnumKeyStatus.Types keyStatus = IEnumKeyStatus.Types.ENABLED;

    @Lob
    @Column(name = SchemaColumnConstantName.C_KEY_MATERIAL, nullable = false)
    private byte[] keyMaterial;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = SchemaColumnConstantName.C_PUBLIC_KEY)
    private byte[] publicKey; // X.509 encoded public key (null for symmetric/HMAC keys)

    @Column(name = SchemaColumnConstantName.C_DEACTIVATION_DATE)
    private LocalDateTime deactivationDate;

    @Column(name = SchemaColumnConstantName.C_EXPIRY_DATE)
    private LocalDateTime validTo;

    @Column(name = SchemaColumnConstantName.C_SIGNING_ALGORITHM, length = 50)
    private String signingAlgorithm;

    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_EXPIRATION_MODEL, length = 50)
    private IEnumKeyExpirationModel.Types expirationModel;

    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_ORIGIN, length = 50)
    private IEnumKeyOrigin.Types origin;

    // =========================================================================
    // Helper methods (derived fields)
    // =========================================================================

    /**
     * Returns true if this key version has expired.
     * <p>
     * A version is expired when {@code validTo} is set and lies in the past —
     * after this date the version's key material is no longer usable,
     * regardless of the stored {@code keyStatus}.
     * </p>
     */
    public boolean isExpired() {
        return validTo != null && validTo.isBefore(LocalDateTime.now());
    }

    /**
     * Returns true if this key version has been deactivated.
     * <p>
     * A version is deactivated when {@code deactivationDate} is set and lies
     * in the past. Deactivated versions must not be used for new cryptographic
     * operations (they may still be needed for decryption/verification of data
     * produced while active).
     * </p>
     */
    public boolean isDeactivated() {
        return deactivationDate != null && deactivationDate.isBefore(LocalDateTime.now());
    }

    /**
     * Effective lifecycle status that takes expiration and deactivation into account.
     * <p>
     * A version that is expired or deactivated is always reported as
     * {@code DISABLED}, regardless of its stored {@code keyStatus}.
     * All other states are returned unchanged.
     * </p>
     *
     * @return the effective (usable) status
     */
    public IEnumKeyStatus.Types getKeyStatus() {
        if (isExpired() || isDeactivated()) {
            return IEnumKeyStatus.Types.DISABLED;
        }
        return keyStatus;
    }

    public void setKeyStatus(IEnumKeyStatus.Types keyStatus) {
        if (isExpired() || isDeactivated()) {
            this.keyStatus = IEnumKeyStatus.Types.DISABLED;
        } else {
            this.keyStatus = keyStatus;
        }
    }

    /**
     * Returns true if the version is in {@code PENDING_DELETION} state.
     * <p>
     * Returns {@code false} when the version is expired or deactivated — an
     * expired/deactivated version is treated as {@code DISABLED} for all
     * status checks.
     * </p>
     */
    public boolean isPendingDeletion() {
        return IEnumKeyStatus.Types.PENDING_DELETION.equals(getKeyStatus());
    }

    /**
     * Returns true if the version is effectively enabled — i.e. stored status is
     * {@code ENABLED} and the version is neither expired nor deactivated.
     */
    public boolean isEnabled() {
        return IEnumKeyStatus.Types.ENABLED.equals(getKeyStatus());
    }

    /**
     * Returns true if the version is effectively disabled — i.e. either stored
     * status is {@code DISABLED}, or the version is expired or deactivated.
     */
    public boolean isDisabled() {
        return IEnumKeyStatus.Types.DISABLED.equals(getKeyStatus());
    }

    /**
     * Returns true if the version is in {@code PENDING_IMPORT} state (awaiting BYOK material).
     * <p>
     * Returns {@code false} when the version is expired or deactivated — an
     * expired/deactivated version is treated as {@code DISABLED} for all
     * status checks.
     * </p>
     */
    public boolean isPendingImport() {
        return IEnumKeyStatus.Types.PENDING_IMPORT.equals(getKeyStatus());
    }

    /**
     * Returns true if the version can currently be used for cryptographic operations:
     * it is {@code ENABLED}, not expired, and not deactivated.
     */
    @Transient
    public boolean isUsable() {
        return isEnabled();
    }
}