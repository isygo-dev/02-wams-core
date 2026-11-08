package eu.isygoit.plan.application;

import eu.isygoit.enums.IEnumLogLevel;
import eu.isygoit.plan.domain.LogEntry;
import eu.isygoit.plan.exception.InvalidRequestException;
import eu.isygoit.plan.port.LogEntryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JobLogQueryService {
    private final JobQueryService jobQueryService;
    private final LogEntryRepository logEntryRepository;

    public JobLogQueryService(
            JobQueryService jobQueryService,
            LogEntryRepository logEntryRepository) {
        this.jobQueryService = jobQueryService;
        this.logEntryRepository = logEntryRepository;
    }

    public List<LogEntry> logs(
            UUID jobId, Instant from, Instant to, IEnumLogLevel.Types minLevel, int limit) {
        if (limit < 1 || limit > 1000) {
            throw new InvalidRequestException("INVALID_LOG_LIMIT", "limit must be between 1 and 1000");
        }
        jobQueryService.status(jobId);
        if (from == null && to == null) {
            return logEntryRepository.findLatest(jobId, limit);
        }
        if (from == null || to == null || from.isAfter(to)) {
            throw new InvalidRequestException("INVALID_LOG_RANGE",
                    "from and to must both be provided and from must not be after to");
        }
        return logEntryRepository.findByJobId(jobId, from, to, minLevel, limit);
    }
}
