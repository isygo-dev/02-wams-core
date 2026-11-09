package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;
import eu.isygoit.service.impl.planning.ValidationResult;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Thrown on the submit path when request validation (design section 7) found at least one error.
 * <p>
 * {@link #code()} is the code of the FIRST error (so {@code PlanExceptionHandler} and the persisted job
 * {@code errorCode} keep working like for every other {@link InvalidRequestException}); {@link #issues()} carries ALL
 * collected issues (errors and warnings) for rich rendering. Immutable, thread-safe.
 */
@MsgLocale(value = "plan.error.invalid.request", httpStatus = HttpStatus.BAD_REQUEST)
public class RequestValidationException extends InvalidRequestException {

    private final transient List<ValidationResult.Issue> issues;

    /**
     * Creates the exception from a failed result.
     *
     * @param result a result that {@linkplain ValidationResult#hasErrors() has errors}
     */
    public RequestValidationException(ValidationResult result) {
        super(result.firstError().code(), describe(result));
        this.issues = List.copyOf(result.issues());
    }

    /** @return all collected issues, errors first as reported (never {@code null}) */
    public List<ValidationResult.Issue> issues() {
        return issues;
    }

    private static String describe(ValidationResult result) {
        ValidationResult.Issue first = result.firstError();
        int errors = result.errors().size();
        String path = first.path() == null || first.path().isEmpty() ? "" : first.path() + ": ";
        return errors <= 1 ? path + first.message() : path + first.message() + " (and " + (errors - 1) + " more error(s))";
    }
}
