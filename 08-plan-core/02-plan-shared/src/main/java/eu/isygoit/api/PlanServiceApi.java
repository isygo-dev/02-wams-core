package eu.isygoit.api;

import eu.isygoit.enums.IEnumLogLevel;
import eu.isygoit.plan.api.JobDashboardSummary;
import eu.isygoit.plan.api.PlanningRequest;
import eu.isygoit.plan.domain.JobStatus;
import eu.isygoit.plan.domain.LogEntry;
import eu.isygoit.plan.domain.SolveResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PlanServiceApi {
    @PostMapping("/jobs")
    ResponseEntity<JobStatus> submit(@RequestBody PlanningRequest request);

    @GetMapping("/jobs/summary")
    JobDashboardSummary dashboardSummary(@RequestParam(name = "limit", defaultValue = "50") int limit);

    @GetMapping("/jobs/{jobId}")
    JobStatus status(@PathVariable UUID jobId);

    @GetMapping("/jobs/{jobId}/logs")
    List<LogEntry> logs(
            @PathVariable UUID jobId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) IEnumLogLevel.Types minLevel,
            @RequestParam(defaultValue = "100") int limit);

    @GetMapping("/jobs/{jobId}/result")
    SolveResult result(@PathVariable UUID jobId);

    @DeleteMapping("/jobs/{jobId}")
    ResponseEntity<Void> cancel(@PathVariable UUID jobId);
}
