package eu.isygoit.dto.request.params;

/**
 * Params of {@code minimize_plan_changes}: deviation from the {@code initialSolution}. Both weights default to 1.
 * <pre>{@code
 * { "startDeviationWeight": 1, "resourceChangeWeight": 20 }
 * }</pre>
 *
 * @param startDeviationWeight penalty per tick a start moved
 * @param resourceChangeWeight penalty per resource that changed
 */
public record PlanChangeParams(Integer startDeviationWeight, Integer resourceChangeWeight) {

    public PlanChangeParams {
        startDeviationWeight = startDeviationWeight == null ? Integer.valueOf(1) : startDeviationWeight;
        resourceChangeWeight = resourceChangeWeight == null ? Integer.valueOf(1) : resourceChangeWeight;
    }
}
