package eu.isygoit.plan.application;

import eu.isygoit.plan.engine.SolverCancellationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class JobCancellationRegistry {
    private final ConcurrentMap<UUID, SolverCancellationToken> tokens = new ConcurrentHashMap<>();

    public Registration register(UUID jobId) {
        SolverCancellationToken token = new SolverCancellationToken();
        if (tokens.putIfAbsent(jobId, token) != null) {
            throw new IllegalStateException("A cancellation token is already registered for job " + jobId);
        }
        return new Registration(jobId, token);
    }

    public void cancel(UUID jobId) {
        SolverCancellationToken token = tokens.get(jobId);
        if (token != null) {
            token.cancel();
        }
    }

    public final class Registration implements AutoCloseable {
        private final UUID jobId;
        private final SolverCancellationToken token;

        private Registration(UUID jobId, SolverCancellationToken token) {
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
