package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("invalid.rotation.period.exception")
public class InvalidRotationPeriodException extends ManagedException {

    public InvalidRotationPeriodException(String message) {
        super(message);
    }

    public InvalidRotationPeriodException(String message, Throwable cause) {
        super(message, cause);
    }
}
