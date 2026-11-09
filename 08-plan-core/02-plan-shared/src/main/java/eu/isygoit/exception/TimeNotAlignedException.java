package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;
import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Thrown by {@code Horizon.toTick} (and {@code Horizon.ticks}) when an instant is not on a tick boundary of the
 * horizon or lies outside {@code [start, end]}. Unchecked: the validators catch it and turn it into the issues
 * {@code TIME_NOT_ALIGNED} / {@code TIME_OUT_OF_HORIZON} with the path of the offending field.
 */
@MsgLocale(value = "plan.error.time.not.aligned", httpStatus = HttpStatus.BAD_REQUEST)
public final class TimeNotAlignedException extends ManagedException {

    /**
     * Stable code of the not-aligned case.
     */
    public static final String CODE_NOT_ALIGNED = "TIME_NOT_ALIGNED";
    /**
     * Stable code of the outside-of-horizon case.
     */
    public static final String CODE_OUT_OF_HORIZON = "TIME_OUT_OF_HORIZON";

    private final Instant instant;
    private final boolean outOfHorizon;

    /**
     * @param instant      the offending instant (may be null when a null was passed)
     * @param outOfHorizon {@code true} when the instant is aligned (or not checked) but outside the horizon
     * @param message      human readable detail
     */
    public TimeNotAlignedException(Instant instant, boolean outOfHorizon, String message) {
        super(message);
        this.instant = instant;
        this.outOfHorizon = outOfHorizon;
    }

    /**
     * @return the offending instant
     */
    public Instant instant() {
        return instant;
    }

    /**
     * @return {@code true} when the problem is "outside the horizon" rather than "not on a tick boundary"
     */
    public boolean outOfHorizon() {
        return outOfHorizon;
    }

    /**
     * @return {@value #CODE_OUT_OF_HORIZON} or {@value #CODE_NOT_ALIGNED}
     */
    public String code() {
        return outOfHorizon ? CODE_OUT_OF_HORIZON : CODE_NOT_ALIGNED;
    }
}
