package eu.isygoit.dto.data;

import eu.isygoit.dto.extendable.AuditableIdAssignableDto;
import eu.isygoit.enums.IEnumJobStatusCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Planning job minimal representation (lists, lookups, dashboard): no request / result / diagnostics payload.
 * <p>
 * Mirrors {@link eu.isygoit.model.PlanJob}.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PlanJobMinDto extends AuditableIdAssignableDto<Long> {

    /**
     * Surrogate identifier. Example: {@code 1042L}.
     */
    private Long id;

    /**
     * Business code unique per tenant. Example: {@code "PLN-000042"}.
     */
    private String code;

    /**
     * Owning tenant (read-only for clients).
     */
    private String tenant;

    /**
     * Optional client-chosen idempotency reference. Example: {@code "timetable-2026-w14"}.
     */
    private String clientRef;

    /**
     * Solver chosen at submit time. Example: {@code "cpsat"}.
     */
    private String engine;

    /**
     * Lifecycle status of the job.
     */
    private IEnumJobStatusCode.Types status;

    private Instant submittedAt;

    private Instant startedAt;

    private Instant completedAt;

    /**
     * Failure / infeasibility code when the job ended abnormally, otherwise {@code null}.
     */
    private String errorCode;

    /**
     * Human readable progress or failure message.
     */
    private String message;
}
