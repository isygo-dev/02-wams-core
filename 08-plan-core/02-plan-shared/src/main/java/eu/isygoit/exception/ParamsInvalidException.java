package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;
import org.springframework.http.HttpStatus;

/**
 * Thrown by {@code ParamsBinder} when the {@code params} map of a constraint/objective cannot be bound strictly to
 * the typed params record of its family (unknown property, wrong type, malformed value). Carries the JSON path of
 * the offending property relative to the params object (for example {@code period.unit}); the validators prefix it
 * with the location of the constraint ({@code constraints[2].params.period.unit}) and report {@value #CODE}.
 */
@MsgLocale(value = "plan.error.params.invalid", httpStatus = HttpStatus.BAD_REQUEST)
public final class ParamsInvalidException extends ManagedException {

    /**
     * Stable error code.
     */
    public static final String CODE = "PARAMS_INVALID";

    private final String path;

    /**
     * @param path    path of the offending property inside params (empty when it concerns the whole object)
     * @param message human readable detail
     * @param cause   the Jackson exception (may be null)
     */
    public ParamsInvalidException(String path, String message, Throwable cause) {
        super(message, cause);
        this.path = path == null ? "" : path;
    }

    /**
     * @return the path of the offending property inside params ("" when the whole object)
     */
    public String path() {
        return path;
    }

    /**
     * @return {@value #CODE}
     */
    public String code() {
        return CODE;
    }
}
