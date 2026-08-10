package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("policy.serialization.exception")
public class PolicySerializationException extends ManagedException {

    public PolicySerializationException(String message) {
        super(message);
    }

    public PolicySerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
