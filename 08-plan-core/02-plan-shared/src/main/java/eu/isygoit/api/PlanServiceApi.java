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
    JobDashboardSummary dashboardSummary(
            @RequestParam(name = "limit", defaultValue = "50") int limit);

    @GetMapping("/jobs/{jobId}")
    JobStatus status(@PathVariable("jobId") UUID jobId);

    @GetMapping("/jobs/{jobId}/request")
    ResponseEntity<PlanningRequest> request(@PathVariable("jobId") UUID jobId);

    @PutMapping("/jobs/{jobId}")
    ResponseEntity<JobStatus> update(
            @PathVariable("jobId") UUID jobId,
            @RequestBody PlanningRequest request);

    @GetMapping("/jobs/{jobId}/logs")
    List<LogEntry> logs(
            @PathVariable("jobId") UUID jobId,
            @RequestParam(name = "from", required = false) Instant from,
            @RequestParam(name = "to", required = false) Instant to,
            @RequestParam(name = "minLevel", required = false) IEnumLogLevel.Types minLevel,
            @RequestParam(name = "limit", defaultValue = "100") int limit);

    @GetMapping("/jobs/{jobId}/result")
    SolveResult result(@PathVariable("jobId") UUID jobId);

    @DeleteMapping("/jobs/{jobId}")
    ResponseEntity<Void> cancel(@PathVariable("jobId") UUID jobId);

    @PostMapping("/jobs/{jobId}/stop")
    ResponseEntity<Void> stop(@PathVariable("jobId") UUID jobId);

    @DeleteMapping("/jobs/{jobId}/record")
    ResponseEntity<Void> delete(@PathVariable("jobId") UUID jobId);
}