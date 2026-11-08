package eu.isygoit.plan.infrastructure.kafka;

import eu.isygoit.async.camel.CamelRepository;
import eu.isygoit.plan.application.port.LogProducer;
import eu.isygoit.plan.domain.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CamelLogProducer implements LogProducer {
    private static final Logger log = LoggerFactory.getLogger(CamelLogProducer.class);

    private final CamelRepository camelRepository;

    public CamelLogProducer(CamelRepository camelRepository) {
        this.camelRepository = camelRepository;
    }

    @Override
    public void publishAsync(LogEntry entry) {
        try {
            camelRepository.asyncSendBody("plan-log-queue", entry)
                    .whenComplete((ignored, failure) -> {
                        if (failure != null) {
                            log.warn("Failed to publish planning log for job {}", entry.jobId(), failure);
                        }
                    });
        } catch (RuntimeException exception) {
            log.warn("Failed to enqueue planning log for job {}", entry.jobId(), exception);
        }
    }
}
