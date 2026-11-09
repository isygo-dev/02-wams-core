package eu.isygoit.async.camel.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.async.kafka.PlanDeadLetterProducer;
import eu.isygoit.repository.PlanJobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Dead-letter handler of the plan-job consumer route. After the dead-letter publish it tries to move the job (when its
 * id can be read from the {@code PlanJobMessage} body or the record key) to FAILED through
 * {@link PlanJobRepository#failIfNotTerminal(Long, String, String)} so a client does not poll a job that will never
 * run. The update runs in its own transaction (the repository query is a modifying one) and the post-processing step
 * never throws.
 */
@Component
public class PlanJobDeadLetterProcessor extends AbstractPlanDeadLetterProcessor {
    public static final String REJECTED = "JOB_MESSAGE_REJECTED";
    public static final String DISPATCH_FAILED = "DISPATCH_FAILED";

    private final PlanJobRepository jobRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public PlanJobDeadLetterProcessor(
            PlanDeadLetterProducer deadLetterProducer,
            PlanJobRepository jobRepository,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager,
            @Value("${spring.kafka.topics.plan-jobs-dlq:isygo.plan.jobs.dlq}") String deadLetterTopic) {
        super(deadLetterProducer, deadLetterTopic, false);
        this.jobRepository = jobRepository;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    protected void afterDeadLetter(byte[] payload, String key, Throwable failure) {
        Long jobId = extractJobId(payload, key);
        if (jobId == null) {
            log.warn("Dead-lettered plan job message has no readable job id; no job state was changed");
            return;
        }
        String code = PlanKafkaErrors.isNonRetryable(failure) ? REJECTED : DISPATCH_FAILED;
        String message = REJECTED.equals(code)
                ? "The planning job message was rejected as invalid"
                : "The planning job could not be processed after retries";
        try {
            Integer updated = transactionTemplate.execute(
                    status -> jobRepository.failIfNotTerminal(jobId, code, message));
            if (updated == null || updated <= 0) {
                log.warn("Dead-lettered plan job {} was not marked FAILED (already terminal or missing); "
                        + "no state change", jobId);
            }
        } catch (RuntimeException exception) {
            log.error("Could not mark dead-lettered plan job {} as FAILED: {}", jobId, exception.getMessage(), exception);
        }
    }

    Long extractJobId(byte[] payload, String key) {
        if (payload != null && payload.length > 0) {
            try {
                JsonNode jobId = objectMapper.readTree(payload).path("jobId");
                if (jobId.isIntegralNumber()) {
                    return jobId.asLong();
                }
                if (jobId.isTextual()) {
                    Long parsed = PlanKafkaKeys.jobIdOf(jobId.asText());
                    if (parsed != null) {
                        return parsed;
                    }
                }
            } catch (Exception ignored) {
                // fall back to the record key
            }
        }
        return PlanKafkaKeys.jobIdOf(key);
    }
}
