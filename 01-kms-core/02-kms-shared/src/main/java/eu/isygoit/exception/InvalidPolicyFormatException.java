package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("invalid.policy.format.exception")
public class InvalidPolicyFormatException extends ManagedException {

    public InvalidPolicyFormatException(String message) {
        super(message);
    }

    public InvalidPolicyFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
