package eu.isygoit.plan.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import eu.isygoit.plan.application.port.JobStatusCache;
import eu.isygoit.plan.domain.JobStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.plan.cache.redis-enabled", havingValue = "false", matchIfMissing = true)
public class CaffeineJobStatusCache implements JobStatusCache {
    private final Cache<UUID, JobStatus> cache;

    public CaffeineJobStatusCache(@Value("${app.plan.cache.ttl:30m}") Duration ttl) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(100_000)
                .build();
    }

    @Override
    public Optional<JobStatus> get(UUID jobId) {
        return Optional.ofNullable(cache.getIfPresent(jobId));
    }

    @Override
    public void put(JobStatus status) {
        cache.put(status.jobId(), status);
    }

    @Override
    public void evict(UUID jobId) {
        cache.invalidate(jobId);
    }
}
