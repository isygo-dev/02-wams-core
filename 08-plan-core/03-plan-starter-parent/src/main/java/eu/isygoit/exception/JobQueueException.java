package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;
import org.springframework.http.HttpStatus;

@MsgLocale(value = "plan.error.job.queue.unavailable", httpStatus = HttpStatus.SERVICE_UNAVAILABLE)
public final class JobQueueException extends ManagedException {
    public JobQueueException(Throwable cause) {
        super("Job could not be published", cause);
    }
}
