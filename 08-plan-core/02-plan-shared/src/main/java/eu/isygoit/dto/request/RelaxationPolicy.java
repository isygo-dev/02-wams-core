package eu.isygoit.dto.request;

public record RelaxationPolicy(boolean authorized, int priority, double maxRelaxation) {
    public RelaxationPolicy {
        if (priority < 0) {
            throw new IllegalArgumentException("Relaxation priority must not be negative");
        }
        if (!Double.isFinite(maxRelaxation) || (maxRelaxation != 0.0 && maxRelaxation != 1.0)) {
            throw new IllegalArgumentException("Maximum relaxation must be either 0 or 1");
        }
    }
}
