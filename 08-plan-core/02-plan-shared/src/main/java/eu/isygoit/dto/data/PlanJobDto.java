package eu.isygoit.dto.data;

import eu.isygoit.dto.request.PlanningRequest;
import eu.isygoit.dto.response.Diagnostics;
import eu.isygoit.dto.response.SolveResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Planning job full representation: the minimal fields plus the typed request, result and diagnostics.
 * <p>
 * Mirrors {@link eu.isygoit.model.PlanJob} (the entity stores the three payloads as jsonb strings; the mapper
 * converts them).
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PlanJobDto extends PlanJobMinDto {

    /**
     * The submitted planning request; {@code null} once purged.
     */
    private PlanningRequest request;

    /**
     * The solve result; {@code null} until the job ends with a solution.
     */
    private SolveResult result;

    /**
     * Infeasibility diagnostics; {@code null} unless the job ended INFEASIBLE.
     */
    private Diagnostics diagnostics;
}
