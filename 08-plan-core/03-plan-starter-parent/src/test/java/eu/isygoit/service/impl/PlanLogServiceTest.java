package eu.isygoit.service.impl;

import eu.isygoit.dto.data.LogBucket;
import eu.isygoit.dto.data.LogEntry;
import eu.isygoit.enums.IEnumLogLevel;
import eu.isygoit.model.PlanLog;
import eu.isygoit.model.PlanLogKey;
import eu.isygoit.repository.PlanLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.InsertOptions;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlanLogServiceTest {
    private final PlanLogRepository repository = mock(PlanLogRepository.class);
    private final CassandraTemplate template = mock(CassandraTemplate.class);
    private final PlanLogService service = new PlanLogService(repository, template);

    private LogEntry entry(Instant time, IEnumLogLevel.Types level) {
        return new LogEntry(5L, "t1", LogBucket.start(time), time, UUID.randomUUID(), level, "SOLVE", "cp_sat",
                "m", null, 1L, null, null);
    }

    @Test
    void appendWritesTheRowWithTheThirtyDayTtl() {
        service.append(entry(Instant.now(), IEnumLogLevel.Types.INFO));

        org.mockito.ArgumentCaptor<InsertOptions> options = org.mockito.ArgumentCaptor.forClass(InsertOptions.class);
        verify(template).insert(any(PlanLog.class), options.capture());
        assertEquals(Duration.ofSeconds(PlanLog.LOG_TTL_SECONDS), options.getValue().getTtl());
    }

    @Test
    void findLatestReadsTheNewestBucketFirstAndAppliesTheLevelFilter() {
        Instant now = Instant.now();
        PlanLog info = new PlanLog(new PlanLogKey(5L, "t1", LogBucket.start(now), now, UUID.randomUUID()),
                IEnumLogLevel.Types.INFO, "s", "e", "m", null, 1L, null, null);
        PlanLog error = new PlanLog(new PlanLogKey(5L, "t1", LogBucket.start(now), now, UUID.randomUUID()),
                IEnumLogLevel.Types.ERROR, "s", "e", "m", null, 1L, null, null);
        when(repository.findNewestInBucket(eq("t1"), eq(5L), eq(LogBucket.start(now)), any(), any(), anyInt()))
                .thenReturn(List.of(info, error));

        List<LogEntry> logs = service.findLatest("t1", 5L, null, null, IEnumLogLevel.Types.ERROR, 10);

        assertEquals(1, logs.size());
        assertEquals(IEnumLogLevel.Types.ERROR, logs.get(0).level());
    }

    @Test
    void findLatestRejectsAMissingTenantAndANonPositiveLimit() {
        assertThrows(IllegalArgumentException.class, () -> service.findLatest(" ", 5L, null, null, null, 10));
        assertThrows(IllegalArgumentException.class, () -> service.findLatest("t1", 5L, null, null, null, 0));
    }
}
