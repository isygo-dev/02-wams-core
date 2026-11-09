package eu.isygoit.plan.api;

import eu.isygoit.plan.domain.JobStatus;

import java.util.List;

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
        List<JobStatus> recentJobs) {

    public JobDashboardSummary {
        recentJobs = recentJobs == null ? List.of() : List.copyOf(recentJobs);
    }
}
