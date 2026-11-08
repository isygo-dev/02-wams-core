package eu.isygoit.controller;

import eu.isygoit.api.PlanServiceApi;
import eu.isygoit.enums.IEnumLogLevel;
import eu.isygoit.plan.api.JobDashboardSummary;
import eu.isygoit.plan.api.PlanningRequest;
import eu.isygoit.plan.application.JobLogQueryService;
import eu.isygoit.plan.application.JobQueryService;
import eu.isygoit.plan.application.JobSubmissionService;
import eu.isygoit.plan.domain.JobStatus;
import eu.isygoit.plan.domain.LogEntry;
import eu.isygoit.plan.domain.SolveResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/private/plans")
public class PlanController implements PlanServiceApi {
    private final JobSubmissionService submissionService;
    private final JobQueryService queryService;
    private final JobLogQueryService logQueryService;

    public PlanController(
            JobSubmissionService submissionService,
            JobQueryService queryService,
            JobLogQueryService logQueryService) {
        this.submissionService = submissionService;
        this.queryService = queryService;
        this.logQueryService = logQueryService;
    }

    @Override
    public ResponseEntity<JobStatus> submit(PlanningRequest request) {
        return ResponseEntity.accepted().body(submissionService.submit(request));
    }

    @Override
    public JobDashboardSummary dashboardSummary(int limit) {
        return queryService.dashboardSummary(limit);
    }

    @Override
    public JobStatus status(UUID jobId) {
        return queryService.refreshStatus(jobId);
    }

    @Override
    public List<LogEntry> logs(
            UUID jobId,
            Instant from,
            Instant to,
            IEnumLogLevel.Types minLevel,
            int limit) {
        return logQueryService.logs(jobId, from, to, minLevel, limit);
    }

    @Override
    public SolveResult result(UUID jobId) {
        return queryService.result(jobId);
    }

    @Override
    public ResponseEntity<Void> cancel(UUID jobId) {
        return queryService.cancel(jobId)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
}
