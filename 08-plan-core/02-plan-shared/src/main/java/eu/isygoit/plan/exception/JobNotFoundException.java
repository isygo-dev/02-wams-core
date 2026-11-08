package eu.isygoit.plan.exception;

import eu.isygoit.annotation.MsgLocale;
import eu.isygoit.exception.ManagedException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

@MsgLocale(value = "plan.error.job.not.found", httpStatus = HttpStatus.NOT_FOUND)
public final class JobNotFoundException extends ManagedException {
    public JobNotFoundException(UUID jobId) {
        super("Planning job '" + jobId + "' was not found");
    }
}
