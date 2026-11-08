package eu.isygoit.plan.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.plan.application.port.JobStatusCache;
import eu.isygoit.plan.domain.JobStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.plan.cache.redis-enabled", havingValue = "true")
public class RedisJobStatusCache implements JobStatusCache {
    private static final String KEY_PREFIX = "isygo-plan:status:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public RedisJobStatusCache(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.plan.cache.ttl:30m}") Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    @Override
    public Optional<JobStatus> get(UUID jobId) {
        String value = redisTemplate.opsForValue().get(key(jobId));
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, JobStatus.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not deserialize cached planning status", exception);
        }
    }

    @Override
    public void put(JobStatus status) {
        try {
            redisTemplate.opsForValue().set(
                    key(status.jobId()), objectMapper.writeValueAsString(status), ttl);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize cached planning status", exception);
        }
    }

    @Override
    public void evict(UUID jobId) {
        redisTemplate.delete(key(jobId));
    }

    private String key(UUID jobId) {
        return KEY_PREFIX + jobId;
    }
}
