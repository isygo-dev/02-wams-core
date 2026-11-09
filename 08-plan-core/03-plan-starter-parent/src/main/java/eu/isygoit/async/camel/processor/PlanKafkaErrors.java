package eu.isygoit.async.camel.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.camel.InvalidPayloadException;
import org.apache.camel.NoTypeConversionAvailableException;

/**
 * Classification of consumer failures. The non-retryable set must match the exception classes listed in the
 * non-retryable onException blocks of plan-job-camel-routes.xml and plan-log-camel-routes.xml.
 */
public final class PlanKafkaErrors {
    private PlanKafkaErrors() {
    }

    /** True when retrying the same record can never succeed (parse / deserialization / validation failures). */
    public static boolean isNonRetryable(Throwable failure) {
        int depth = 0;
        for (Throwable current = failure; current != null && depth < 20; current = current.getCause(), depth++) {
            if (current instanceof JsonProcessingException
                    || current instanceof PoisonMessageException
                    || current instanceof InvalidPayloadException
                    || current instanceof NoTypeConversionAvailableException) {
                return true;
            }
        }
        return false;
    }
}
