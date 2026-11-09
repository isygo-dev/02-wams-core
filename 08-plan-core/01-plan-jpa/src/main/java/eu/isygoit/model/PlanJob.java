package eu.isygoit.model;

import eu.isygoit.constants.TenantConstants;
import eu.isygoit.enums.IEnumJobStatusCode;
import eu.isygoit.model.jakarta.AuditableEntity;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.model.schema.SchemaConstantSize;
import eu.isygoit.model.schema.SchemaTableConstantName;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = SchemaTableConstantName.T_PLAN_JOB)
@ToString(callSuper = true)
public class PlanJob extends AuditableEntity<UUID>
        implements ITenantAssignable, IStatusAssignable<IEnumJobStatusCode.Types> {
    @Id
    @Column(name = SchemaColumnConstantName.C_JOB_ID, nullable = false, updatable = false)
    private UUID id;

    @ColumnDefault("'" + TenantConstants.DEFAULT_TENANT_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_TENANT, length = SchemaConstantSize.TENANT, updatable = false, nullable = false)
    private String tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_STATUS, nullable = false, length = SchemaConstantSize.PLAN_JOB_STATUS)
    private IEnumJobStatusCode.Types status;

    @Column(name = SchemaColumnConstantName.C_SUBMITTED_AT, nullable = false)
    private Instant submittedAt;

    @Column(name = SchemaColumnConstantName.C_STARTED_AT)
    private Instant startedAt;

    @Column(name = SchemaColumnConstantName.C_COMPLETED_AT)
    private Instant completedAt;

    @Column(name = SchemaColumnConstantName.C_ERROR_CODE, length = SchemaConstantSize.PLAN_ERROR_CODE)
    private String errorCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = SchemaColumnConstantName.C_MESSAGE)
    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = SchemaColumnConstantName.C_REQUEST_JSON)
    private String requestJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = SchemaColumnConstantName.C_RESULT_JSON)
    private String resultJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = SchemaColumnConstantName.C_DIAGNOSTICS_JSON)
    private String diagnosticsJson;

    @Version
    @Column(name = SchemaColumnConstantName.C_VERSION, nullable = false)
    private long version;
}
