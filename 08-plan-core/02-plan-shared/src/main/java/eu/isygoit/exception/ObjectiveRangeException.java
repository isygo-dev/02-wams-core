package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;
import org.springframework.http.HttpStatus;

/**
 * Thrown by {@code ScaledMath} when an objective term, a tier or a weighted total leaves the supported range
 * (int64 arithmetic, tiers and totals guarded to 2^53). Same stable code as the historical
 * {@code InvalidRequestException} of the engines: {@value #CODE}.
 */
@MsgLocale(value = "plan.error.objective.score.out.of.range", httpStatus = HttpStatus.BAD_REQUEST)
public final class ObjectiveRangeException extends ManagedException {

    /**
     * Stable error code.
     */
    public static final String CODE = "OBJECTIVE_SCORE_OUT_OF_RANGE";

    /**
     * @param message human readable detail (which value overflowed)
     */
    public ObjectiveRangeException(String message) {
        super(message);
    }

    /**
     * @param message human readable detail
     * @param cause   the arithmetic exception that triggered the guard
     */
    public ObjectiveRangeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @return {@value #CODE}
     */
    public String code() {
        return CODE;
    }
}
