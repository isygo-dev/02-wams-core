package eu.isygoit.service.impl;

import eu.isygoit.solver.SolverCancellationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class JobCancellationRegistry {
    private final ConcurrentMap<Long, SolverCancellationToken> tokens = new ConcurrentHashMap<>();
    private volatile boolean shuttingDown;

    public Registration register(Long jobId) {
        SolverCancellationToken token = new SolverCancellationToken();
        if (tokens.putIfAbsent(jobId, token) != null) {
            throw new IllegalStateException("A cancellation token is already registered for job " + jobId);
        }
        return new Registration(jobId, token);
    }

    public void cancel(Long jobId) {
        SolverCancellationToken token = tokens.get(jobId);
        if (token != null) {
            token.cancel();
        }
    }

    /**
     * True while at least one job of this instance is being solved.
     */
    public boolean hasActive() {
        return !tokens.isEmpty();
    }

    /**
     * Snapshot of the jobs currently solved by this instance.
     */
    public List<Long> activeJobIds() {
        return List.copyOf(tokens.keySet());
    }

    /**
     * Marks the instance as shutting down: no new job is started and workers release (instead of completing)
     * the jobs interrupted by {@link #cancelAll()}.
     */
    public void beginShutdown() {
        shuttingDown = true;
    }

    public boolean isShuttingDown() {
        return shuttingDown;
    }

    /**
     * Requests cancellation of every running job of this instance.
     */
    public void cancelAll() {
        tokens.values().forEach(SolverCancellationToken::cancel);
    }

    public final class Registration implements AutoCloseable {
        private final Long jobId;
        private final SolverCancellationToken token;

        private Registration(Long jobId, SolverCancellationToken token) {
            this.jobId = jobId;
            this.token = token;
        }

        public SolverCancellationToken token() {
            return token;
        }

        @Override
        public void close() {
            tokens.remove(jobId, token);
        }
    }
}
