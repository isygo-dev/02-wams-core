package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a request exceeds one of the configured {@code app.plan.limits.*} safety limits.
 */
@MsgLocale(value = "plan.error.limit.exceeded", httpStatus = HttpStatus.BAD_REQUEST)
public final class LimitExceededException extends InvalidRequestException {
    public static final String CODE = "LIMIT_EXCEEDED";

    private final String limitName;
    private final long limit;

    public LimitExceededException(String limitName, long actual, long limit) {
        super(CODE, "Limit '" + limitName + "' exceeded: " + actual + " > " + limit);
        this.limitName = limitName;
        this.limit = limit;
    }

    public String limitName() {
        return limitName;
    }

    public long limit() {
        return limit;
    }
}
