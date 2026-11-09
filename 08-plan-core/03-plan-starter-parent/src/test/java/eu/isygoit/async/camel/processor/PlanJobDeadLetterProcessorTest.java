package eu.isygoit.async.camel.processor;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.async.kafka.PlanDeadLetterProducer;
import eu.isygoit.repository.PlanJobRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.kafka.KafkaException;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlanJobDeadLetterProcessorTest {
    private static final String DLQ = "jobs.dlq";

    private final PlanDeadLetterProducer deadLetterProducer = mock(PlanDeadLetterProducer.class);
    private final PlanJobRepository jobRepository = mock(PlanJobRepository.class);
    private final PlanJobDeadLetterProcessor processor = new PlanJobDeadLetterProcessor(
            deadLetterProducer, jobRepository, new ObjectMapper(), mock(PlatformTransactionManager.class), DLQ);

    @Test
    @SuppressWarnings("unchecked")
    void poisonMessageWithJobIdKeyIsDeadLetteredThenJobFailedThenOffsetCommitted() {
        // truncated JSON: the job id can only come from the record key
        ExchangeFixtures fixtures = new ExchangeFixtures("{\"jobId\":", "123", new PoisonMessageException("invalid"));

        processor.process(fixtures.exchange);

        InOrder order = inOrder(deadLetterProducer, jobRepository, fixtures.manualCommit);
        ArgumentCaptor<Map<String, String>> headers = ArgumentCaptor.forClass(Map.class);
        order.verify(deadLetterProducer).publish(eq(DLQ), eq("123"), any(byte[].class), headers.capture());
        order.verify(jobRepository).failIfNotTerminal(eq(123L), eq("JOB_MESSAGE_REJECTED"), anyString());
        order.verify(fixtures.manualCommit).commit();
        assertEquals(PoisonMessageException.class.getName(),
                headers.getValue().get(PlanDeadLetterProducer.HEADER_EXCEPTION_CLASS));
        assertEquals("source-topic", headers.getValue().get(PlanDeadLetterProducer.HEADER_ORIGINAL_TOPIC));
        assertEquals("0", headers.getValue().get(PlanDeadLetterProducer.HEADER_ORIGINAL_PARTITION));
        assertEquals("42", headers.getValue().get(PlanDeadLetterProducer.HEADER_ORIGINAL_OFFSET));
        assertEquals("4", headers.getValue().get(PlanDeadLetterProducer.HEADER_ATTEMPTS));
    }

    @Test
    void jobIdIsReadFromThePayloadAndExhaustedRetriesUseDispatchFailed() {
        ExchangeFixtures fixtures = new ExchangeFixtures(
                "{\"jobId\":77,\"tenant\":\"acme\"}", null, new IllegalStateException("db down"));

        processor.process(fixtures.exchange);

        verify(jobRepository).failIfNotTerminal(eq(77L), eq("DISPATCH_FAILED"), anyString());
        verify(fixtures.manualCommit).commit();
    }

    @Test
    void payloadJobIdWinsOverTheKeyAndTextualIdsAreAccepted() {
        assertEquals(5L, processor.extractJobId("{\"jobId\":5}".getBytes(), "9"));
        assertEquals(6L, processor.extractJobId("{\"jobId\":\"6\"}".getBytes(), null));
        assertEquals(9L, processor.extractJobId("{\"tenant\":\"acme\"}".getBytes(), "9"));
        assertNull(processor.extractJobId("{\"jobId\":\"abc\"}".getBytes(), "not-a-number"));
        assertNull(processor.extractJobId(null, null));
    }

    @Test
    void unparseableMessageWithoutKeyStillDeadLettersAndCommitsWithoutTouchingJobs() {
        ExchangeFixtures fixtures = new ExchangeFixtures("%%%", null, new JsonParseException(null, "bad json"));

        processor.process(fixtures.exchange);

        verify(deadLetterProducer).publish(eq(DLQ), eq(null), any(byte[].class), any());
        verify(jobRepository, never()).failIfNotTerminal(any(), anyString(), anyString());
        verify(fixtures.manualCommit).commit();
    }

    @Test
    void failingJobTransitionNeverPreventsTheCommit() {
        when(jobRepository.failIfNotTerminal(eq(123L), anyString(), anyString()))
                .thenThrow(new IllegalStateException("db down"));
        ExchangeFixtures fixtures = new ExchangeFixtures("{}", "123", new RuntimeException("x"));

        processor.process(fixtures.exchange);

        verify(fixtures.manualCommit).commit();
    }

    @Test
    void deadLetterPublishFailureLeavesTheOffsetUncommittedAndJobUntouched() {
        doThrow(new KafkaException("broker down")).when(deadLetterProducer)
                .publish(anyString(), any(), any(), any());
        ExchangeFixtures fixtures = new ExchangeFixtures("{}", "123", new RuntimeException("x"));

        assertThrows(KafkaException.class, () -> processor.process(fixtures.exchange));

        verify(fixtures.manualCommit, never()).commit();
        verify(jobRepository, never()).failIfNotTerminal(anyLong(), anyString(), anyString());
    }
}
