package eu.isygoit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Estimated size of the model a request would produce, reported by {@code POST /validate} so a UI can warn before
 * submitting (compare with the limits of {@link CapabilitiesDto.LimitsView}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelEstimateDto {

    /**
     * Surviving (task, requirement, resource) candidates.
     */
    private long candidates;
    /**
     * Estimated interval variables (candidate + master + fixed blocks).
     */
    private long intervals;
    /**
     * Estimated boolean + integer variables.
     */
    private long variables;
    /**
     * Estimated constraints.
     */
    private long constraints;
    /**
     * Distinct lexicographic tiers.
     */
    private int tiers;
    /**
     * Number of ticks of the horizon.
     */
    private long horizonTicks;
}
