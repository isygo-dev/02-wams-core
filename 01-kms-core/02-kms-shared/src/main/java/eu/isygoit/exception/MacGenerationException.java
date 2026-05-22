package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("mac.generation.exception")
public class MacGenerationException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public MacGenerationException(String s) {
        super(s);
    }

    /**
     * Instantiates a new Alias not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public MacGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
