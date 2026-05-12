package eu.isygoit.model;

import eu.isygoit.constants.TenantConstants;
import eu.isygoit.enums.IKmsActionType;
import eu.isygoit.model.jakarta.AuditableEntity;
import eu.isygoit.model.schema.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * The type Kms Audit Log.
 * Represents cryptographic operations audit trail
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = SchemaTableConstantName.T_KMS_AUDIT_LOG,
        indexes = {
                @Index(name = "IDX_KMS_AUDIT_LOG_KEY_ID", columnList = "KEY_ID"),
                @Index(name = "IDX_KMS_AUDIT_LOG_TENANT_ACTION", columnList = "TENANT,ACTION"),
                @Index(name = "IDX_KMS_AUDIT_LOG_TIMESTAMP", columnList = "TIMESTAMP"),
                @Index(name = "IDX_KMS_AUDIT_LOG_PRINCIPAL", columnList = "PRINCIPAL")
        })
public class KmsAuditLog extends AuditableEntity<Long> implements ITenantAssignable {

    @Id
    @SequenceGenerator(name = "kms_audit_log_seq", sequenceName = "kms_audit_log_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kms_audit_log_seq")
    @Column(name = "ID", updatable = false, nullable = false)
    private Long id;

    //@Convert(converter = LowerCaseConverter.class)
    @ColumnDefault("'" + TenantConstants.DEFAULT_TENANT_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_TENANT, length = SchemaConstantSize.TENANT, updatable = false, nullable = false)
    private String tenant;

    @Column(name = "KEY_ID", length = 255)
    private String keyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ACTION", length = 100, nullable = false)
    private IKmsActionType.Types action;

    @Column(name = "PRINCIPAL", length = 255)
    private String principal; // User/Service that performed the action

    @Column(name = "IP_ADDRESS", length = 50)
    private String ipAddress; // Client IP address

    @Column(name = "TIMESTAMP", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(name = "STATUS", length = 50)
    private String status; // SUCCESS, FAILURE

    @Column(name = "ERROR_MESSAGE", length = 1024)
    private String errorMessage;

    @Column(name = "REQUEST_DETAILS", length = 2000)
    private String requestDetails; // JSON format request metadata

    @Column(name = "RESPONSE_DETAILS", length = 2000)
    private String responseDetails; // JSON format response metadata

    @Column(name = "EXECUTION_TIME_MS")
    private Long executionTimeMs; // Time taken in milliseconds
}

