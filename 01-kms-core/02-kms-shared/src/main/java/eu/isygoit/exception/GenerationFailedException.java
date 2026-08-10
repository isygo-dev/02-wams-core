package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("generation.failed.exception")
public class GenerationFailedException extends ManagedException {

    public GenerationFailedException(String message) {
        super(message);
    }

    public GenerationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
