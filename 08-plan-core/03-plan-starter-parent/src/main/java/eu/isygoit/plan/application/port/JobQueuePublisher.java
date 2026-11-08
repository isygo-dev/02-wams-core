package eu.isygoit.plan.application.port;

import eu.isygoit.plan.api.PlanningRequest;

public interface JobQueuePublisher {
    void publish(PlanningRequest request);
}
