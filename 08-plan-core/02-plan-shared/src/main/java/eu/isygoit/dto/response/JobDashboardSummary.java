package eu.isygoit.dto.response;

import eu.isygoit.dto.data.PlanJobMinDto;

import java.util.List;

/**
 * Dashboard counters of the caller tenant.
 *
 * @param averageDurationMillis mean (completedAt - startedAt) of the finished jobs; 0 when none
 * @param successRate           SUCCEEDED / (SUCCEEDED + INFEASIBLE + FAILED + CANCELLED), range 0..1; 0 when none
 * @param queueDepth            number of CREATED jobs (waiting for a worker)
 * @param recentJobs            most recent jobs, newest first
 */
public record JobDashboardSummary(
        long total,
        long created,
        long accepted,
        long validating,
        long normalizing,
        long solving,
        long stopping,
        long succeeded,
        long infeasible,
        long failed,
        long cancelled,
        long averageDurationMillis,
        double successRate,
        long queueDepth,
        List<PlanJobMinDto> recentJobs) {

    public JobDashboardSummary {
        recentJobs = recentJobs == null ? List.of() : List.copyOf(recentJobs);
    }
}
