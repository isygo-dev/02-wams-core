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
                @Index(name = "IDX_KMS_AUDIT_LOG_KEY_ID", columnList = SchemaColumnConstantName.C_KEY_ID),
                @Index(name = "IDX_KMS_AUDIT_LOG_TENANT_ACTION", columnList = SchemaColumnConstantName.C_TENANT + "," + SchemaColumnConstantName.C_ACTION),
                @Index(name = "IDX_KMS_AUDIT_LOG_TIMESTAMP", columnList = SchemaColumnConstantName.C_TIMESTAMP),
                @Index(name = "IDX_KMS_AUDIT_LOG_PRINCIPAL", columnList = SchemaColumnConstantName.C_PRINCIPAL)
        })
public class KmsAuditLog extends AuditableEntity<Long> implements ITenantAssignable {

    @Id
    @SequenceGenerator(name = "kms_audit_log_seq", sequenceName = "kms_audit_log_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kms_audit_log_seq")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    //@Convert(converter = LowerCaseConverter.class)
    @ColumnDefault("'" + TenantConstants.DEFAULT_TENANT_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_TENANT, length = SchemaConstantSize.TENANT, updatable = false, nullable = false)
    private String tenant;

    @Column(name = SchemaColumnConstantName.C_KEY_ID, length = 255)
    private String keyId;

    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_ACTION, length = 100, nullable = false)
    private IKmsActionType.Types action;

    @Column(name = SchemaColumnConstantName.C_PRINCIPAL, length = 255)
    private String principal; // User/Service that performed the action

    @Column(name = SchemaColumnConstantName.C_IP_ADDRESS, length = 50)
    private String ipAddress; // Client IP address

    @Column(name = SchemaColumnConstantName.C_TIMESTAMP, nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(name = SchemaColumnConstantName.C_STATUS, length = 50)
    private String status; // SUCCESS, FAILURE

    @Column(name = SchemaColumnConstantName.C_ERROR_MESSAGE, length = 1024)
    private String errorMessage;

    @Column(name = SchemaColumnConstantName.C_REQUEST_DETAILS, length = 2000)
    private String requestDetails; // JSON format request metadata

    @Column(name = SchemaColumnConstantName.C_RESPONSE_DETAILS, length = 2000)
    private String responseDetails; // JSON format response metadata

    @Column(name = SchemaColumnConstantName.C_EXECUTION_TIME_MS)
    private Long executionTimeMs; // Time taken in milliseconds
}

