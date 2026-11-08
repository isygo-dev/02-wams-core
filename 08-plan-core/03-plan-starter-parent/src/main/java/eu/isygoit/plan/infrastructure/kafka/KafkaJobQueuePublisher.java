package eu.isygoit.plan.infrastructure.kafka;

import eu.isygoit.async.camel.CamelRepository;
import eu.isygoit.plan.api.PlanningRequest;
import eu.isygoit.plan.application.port.JobQueuePublisher;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class KafkaJobQueuePublisher implements JobQueuePublisher {
    private static final long PUBLISH_TIMEOUT_SECONDS = 15;

    private final CamelRepository camelRepository;

    public KafkaJobQueuePublisher(CamelRepository camelRepository) {
        this.camelRepository = camelRepository;
    }

    @Override
    public void publish(PlanningRequest request) {
        try {
            camelRepository.asyncSendBody("plan-job-queue", request)
                    .get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing planning job through Camel", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("Could not publish planning job through Camel", exception);
        }
    }
}
