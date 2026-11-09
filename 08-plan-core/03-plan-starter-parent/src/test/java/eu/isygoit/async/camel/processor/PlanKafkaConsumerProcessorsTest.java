package eu.isygoit.async.camel.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.dto.data.LogEntry;
import eu.isygoit.service.IPlanLogService;
import eu.isygoit.service.impl.JobProcessor;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PlanKafkaConsumerProcessorsTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void jobOffsetIsCommittedOnlyAfterTheJobWasProcessed() throws Exception {
        JobProcessor jobProcessor = mock(JobProcessor.class);
        ExchangeFixtures fixtures = new ExchangeFixtures("{\"jobId\":123,\"tenant\":\"acme\"}", "123", null);

        new PlanJobKafkaConsumerProcessor(jobProcessor, objectMapper).process(fixtures.exchange);

        InOrder order = inOrder(jobProcessor, fixtures.manualCommit);
        order.verify(jobProcessor).process(123L);
        order.verify(fixtures.manualCommit).commit();
    }

    @Test
    void bodyJobIdWinsOverTheKey() throws Exception {
        JobProcessor jobProcessor = mock(JobProcessor.class);
        ExchangeFixtures fixtures = new ExchangeFixtures("{\"jobId\":5}", "9", null);

        new PlanJobKafkaConsumerProcessor(jobProcessor, objectMapper).process(fixtures.exchange);

        verify(jobProcessor).process(5L);
    }

    @Test
    void blankBodyFallsBackToTheKeyAsJobId() throws Exception {
        JobProcessor jobProcessor = mock(JobProcessor.class);
        ExchangeFixtures fixtures = new ExchangeFixtures("  ", "456", null);

        new PlanJobKafkaConsumerProcessor(jobProcessor, objectMapper).process(fixtures.exchange);

        InOrder order = inOrder(jobProcessor, fixtures.manualCommit);
        order.verify(jobProcessor).process(456L);
        order.verify(fixtures.manualCommit).commit();
    }

    @Test
    void jobOffsetIsNotCommittedWhenProcessingFails() {
        JobProcessor jobProcessor = mock(JobProcessor.class);
        doThrow(new IllegalStateException("db down")).when(jobProcessor).process(123L);
        ExchangeFixtures fixtures = new ExchangeFixtures("{\"jobId\":123}", null, null);

        assertThrows(IllegalStateException.class,
                () -> new PlanJobKafkaConsumerProcessor(jobProcessor, objectMapper).process(fixtures.exchange));

        verify(fixtures.manualCommit, never()).commit();
    }

    @Test
    void unparseableJobMessageThrowsANonRetryableErrorWithoutProcessingOrCommitting() {
        JobProcessor jobProcessor = mock(JobProcessor.class);
        ExchangeFixtures fixtures = new ExchangeFixtures("{not json", "123", null);

        JsonProcessingException failure = assertThrows(JsonProcessingException.class,
                () -> new PlanJobKafkaConsumerProcessor(jobProcessor, objectMapper).process(fixtures.exchange));

        assertTrue(PlanKafkaErrors.isNonRetryable(failure));
        verifyNoInteractions(jobProcessor);
        verify(fixtures.manualCommit, never()).commit();
    }

    @Test
    void blankBodyWithoutAUsableKeyIsPoison() {
        JobProcessor jobProcessor = mock(JobProcessor.class);

        for (String key : new String[]{null, "", "not-a-number"}) {
            ExchangeFixtures fixtures = new ExchangeFixtures("  ", key, null);
            PoisonMessageException failure = assertThrows(PoisonMessageException.class,
                    () -> new PlanJobKafkaConsumerProcessor(jobProcessor, objectMapper).process(fixtures.exchange));
            assertTrue(PlanKafkaErrors.isNonRetryable(failure));
            verify(fixtures.manualCommit, never()).commit();
        }
        verifyNoInteractions(jobProcessor);
    }

    @Test
    void bodyWithoutJobIdIsPoison() {
        JobProcessor jobProcessor = mock(JobProcessor.class);
        ExchangeFixtures fixtures = new ExchangeFixtures("{\"tenant\":\"acme\"}", "123", null);

        assertThrows(PoisonMessageException.class,
                () -> new PlanJobKafkaConsumerProcessor(jobProcessor, objectMapper).process(fixtures.exchange));

        verify(jobProcessor, never()).process(anyLong());
        verify(fixtures.manualCommit, never()).commit();
    }

    @Test
    void missingCommitHandleFailsTheJobExchange() {
        ExchangeFixtures fixtures = new ExchangeFixtures("{\"jobId\":123}", null, null).withoutCommitHandle();

        assertThrows(IllegalStateException.class,
                () -> new PlanJobKafkaConsumerProcessor(mock(JobProcessor.class), objectMapper)
                        .process(fixtures.exchange));
    }

    @Test
    void logOffsetIsCommittedOnlyAfterTheLogWasAppended() throws Exception {
        IPlanLogService logService = mock(IPlanLogService.class);
        ObjectMapper mapper = mock(ObjectMapper.class);
        LogEntry entry = mock(LogEntry.class);
        when(mapper.readValue(anyString(), eq(LogEntry.class))).thenReturn(entry);
        ExchangeFixtures fixtures = new ExchangeFixtures("{}", null, null);

        new PlanLogKafkaConsumerProcessor(logService, mapper).process(fixtures.exchange);

        InOrder order = inOrder(logService, fixtures.manualCommit);
        order.verify(logService).append(entry);
        order.verify(fixtures.manualCommit).commit();
    }

    @Test
    void logOffsetIsNotCommittedWhenTheAppendFails() throws Exception {
        IPlanLogService logService = mock(IPlanLogService.class);
        ObjectMapper mapper = mock(ObjectMapper.class);
        LogEntry entry = mock(LogEntry.class);
        when(mapper.readValue(anyString(), eq(LogEntry.class))).thenReturn(entry);
        doThrow(new IllegalStateException("store down")).when(logService).append(entry);
        ExchangeFixtures fixtures = new ExchangeFixtures("{}", null, null);

        assertThrows(IllegalStateException.class,
                () -> new PlanLogKafkaConsumerProcessor(logService, mapper).process(fixtures.exchange));

        verify(fixtures.manualCommit, never()).commit();
    }

    @Test
    void poisonLogMessageThrowsNonRetryableWithoutWriting() {
        IPlanLogService logService = mock(IPlanLogService.class);
        ExchangeFixtures fixtures = new ExchangeFixtures("{bad", null, null);

        JsonProcessingException failure = assertThrows(JsonProcessingException.class,
                () -> new PlanLogKafkaConsumerProcessor(logService, objectMapper).process(fixtures.exchange));

        assertTrue(PlanKafkaErrors.isNonRetryable(failure));
        verifyNoInteractions(logService);
    }

    @Test
    void emptyLogMessageIsPoison() {
        IPlanLogService logService = mock(IPlanLogService.class);
        ExchangeFixtures fixtures = new ExchangeFixtures(" ", null, null);

        assertThrows(PoisonMessageException.class,
                () -> new PlanLogKafkaConsumerProcessor(logService, objectMapper).process(fixtures.exchange));

        verifyNoInteractions(logService);
    }
}
