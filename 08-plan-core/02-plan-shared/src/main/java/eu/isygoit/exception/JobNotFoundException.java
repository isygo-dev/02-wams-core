package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;
import org.springframework.http.HttpStatus;


@MsgLocale(value = "plan.error.job.not.found", httpStatus = HttpStatus.NOT_FOUND)
public final class JobNotFoundException extends ManagedException {

    public JobNotFoundException(Long id) {
        super("Planning job '" + id + "' was not found");
    }
}
