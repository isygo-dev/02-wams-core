package eu.isygoit.plan.api;

import java.util.UUID;

public record PlanningMeta(
        UUID jobId,
        String version,
        String domainName,
        String locale,
        String timezone) {
}
