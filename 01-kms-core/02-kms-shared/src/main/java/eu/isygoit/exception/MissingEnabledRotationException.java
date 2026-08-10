package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("missing.enabled.rotation.exception")
public class MissingEnabledRotationException extends ManagedException {

    public MissingEnabledRotationException(String message) {
        super(message);
    }

    public MissingEnabledRotationException(String message, Throwable cause) {
        super(message, cause);
    }
}
