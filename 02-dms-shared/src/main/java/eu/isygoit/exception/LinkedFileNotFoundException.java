package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;


/**
 * The type Linked file not found exception.
 */
@MsgLocale("linked.file.not.found.exception")
public class LinkedFileNotFoundException extends ManagedException {

    /**
     * Instantiates a new Linked file not found exception.
     *
     * @param message the message
     */
    public LinkedFileNotFoundException(String message) {
        super(message);
    }

    /**
     * Instantiates a new Linked file not found exception.
     *
     * @param throwable the throwable
     */
    public LinkedFileNotFoundException(Throwable throwable) {
        super(throwable);
    }

    /**
     * Instantiates a new Linked file not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public LinkedFileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
