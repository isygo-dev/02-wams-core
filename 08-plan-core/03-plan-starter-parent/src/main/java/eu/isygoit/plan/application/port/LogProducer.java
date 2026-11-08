package eu.isygoit.plan.application.port;

import eu.isygoit.plan.domain.LogEntry;

public interface LogProducer {
    void publishAsync(LogEntry entry);
}
