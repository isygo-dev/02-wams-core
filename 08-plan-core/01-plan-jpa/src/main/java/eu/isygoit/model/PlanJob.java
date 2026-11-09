package eu.isygoit.model;

import eu.isygoit.annotation.Criteria;
import eu.isygoit.constants.TenantConstants;
import eu.isygoit.enums.IEnumJobStatusCode;
import eu.isygoit.model.jakarta.AuditableEntity;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.model.schema.SchemaConstantSize;
import eu.isygoit.model.schema.SchemaIndexConstantName;
import eu.isygoit.model.schema.SchemaTableConstantName;
import eu.isygoit.model.schema.SchemaUcConstantName;
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

/**
 * A planning job: one submitted {@code PlanningRequest} and its lifecycle (status, timings, result, diagnostics).
 * <p>
 * Scoped per tenant. Primary key is a surrogate {@code Long} from {@code plan_job_sequence}; the business
 * {@code code} is unique per tenant; the optional {@code clientRef} is the client-chosen idempotency reference
 * (unique per tenant when present).
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = SchemaTableConstantName.T_PLAN_JOB,
        uniqueConstraints = {
                @UniqueConstraint(
                        name = SchemaUcConstantName.UC_PLAN_JOB_TENANT_CODE,
                        columnNames = {SchemaColumnConstantName.C_TENANT, SchemaColumnConstantName.C_CODE}),
                @UniqueConstraint(
                        name = SchemaUcConstantName.UC_PLAN_JOB_TENANT_CLIENT_REF,
                        columnNames = {SchemaColumnConstantName.C_TENANT, SchemaColumnConstantName.C_CLIENT_REF})
        },
        indexes = {
                @Index(name = SchemaIndexConstantName.IDX_PLAN_JOB_STATUS_SUBMITTED,
                        columnList = SchemaColumnConstantName.C_STATUS + ", " + SchemaColumnConstantName.C_SUBMITTED_AT),
                @Index(name = SchemaIndexConstantName.IDX_PLAN_JOB_TENANT_SUBMITTED,
                        columnList = SchemaColumnConstantName.C_TENANT + ", " + SchemaColumnConstantName.C_SUBMITTED_AT + " DESC"),
                @Index(name = SchemaIndexConstantName.IDX_PLAN_JOB_STATUS_DISPATCHED_SUBMITTED,
                        columnList = SchemaColumnConstantName.C_STATUS + ", " + SchemaColumnConstantName.C_DISPATCHED_AT
                                + ", " + SchemaColumnConstantName.C_SUBMITTED_AT)
        })
@ToString(callSuper = true)
public class PlanJob extends AuditableEntity<Long>
        implements ITenantAssignable, ICodeAssignable, IStatusAssignable<IEnumJobStatusCode.Types> {

    /**
     * Sequence name, also used by the native insert of the repository ({@code nextval}).
     */
    public static final String SEQUENCE_NAME = "plan_job_sequence";

    @Id
    @SequenceGenerator(name = "plan_job_seq_generator", sequenceName = SEQUENCE_NAME, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "plan_job_seq_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    @ColumnDefault("'" + TenantConstants.DEFAULT_TENANT_NAME + "'")
    @Column(name = SchemaColumnConstantName.C_TENANT, length = SchemaConstantSize.TENANT, updatable = false, nullable = false)
    private String tenant;

    /**
     * Business code, unique per tenant, generated through the next-code service.
     */
    @Criteria
    @Column(name = SchemaColumnConstantName.C_CODE, length = SchemaConstantSize.CODE, updatable = false, nullable = false)
    private String code;

    /**
     * Optional client-chosen idempotency reference (nullable: several jobs without reference are allowed).
     */
    @Criteria
    @Column(name = SchemaColumnConstantName.C_CLIENT_REF, length = SchemaConstantSize.PLAN_CLIENT_REF, updatable = false)
    private String clientRef;

    /**
     * The solver chosen at submit time (list filter).
     */
    @Criteria
    @Column(name = SchemaColumnConstantName.C_ENGINE, length = SchemaConstantSize.PLAN_ENGINE)
    private String engine;

    @Criteria
    @Enumerated(EnumType.STRING)
    @Column(name = SchemaColumnConstantName.C_STATUS, nullable = false, length = SchemaConstantSize.PLAN_JOB_STATUS)
    private IEnumJobStatusCode.Types status;

    @Column(name = SchemaColumnConstantName.C_SUBMITTED_AT, nullable = false)
    private Instant submittedAt;

    @Column(name = SchemaColumnConstantName.C_STARTED_AT)
    private Instant startedAt;

    /**
     * Last time the job was handed to the queue / executor; null when never dispatched or released.
     */
    @Column(name = SchemaColumnConstantName.C_DISPATCHED_AT)
    private Instant dispatchedAt;

    @ColumnDefault("0")
    @Column(name = SchemaColumnConstantName.C_DISPATCH_ATTEMPTS, nullable = false)
    private int dispatchAttempts;

    @Column(name = SchemaColumnConstantName.C_COMPLETED_AT)
    private Instant completedAt;

    @Column(name = SchemaColumnConstantName.C_ERROR_CODE, length = SchemaConstantSize.PLAN_ERROR_CODE)
    private String errorCode;

    @Column(name = SchemaColumnConstantName.C_MESSAGE, length = SchemaConstantSize.PLAN_MESSAGE)
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
