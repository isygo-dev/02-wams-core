package eu.isygoit.plan.domain;

public record Metrics(long durationMillis, long iterations, int workersUsed) {
    public Metrics {
        if (durationMillis < 0 || iterations < 0 || workersUsed < 0) {
            throw new IllegalArgumentException("Metrics must not be negative");
        }
    }
}
