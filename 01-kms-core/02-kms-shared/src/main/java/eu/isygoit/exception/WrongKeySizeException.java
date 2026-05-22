package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("wrong.key.size.exception")
public class WrongKeySizeException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public WrongKeySizeException(String s) {
        super(s);
    }

    /**
     * Instantiates a new Alias not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public WrongKeySizeException(String message, Throwable cause) {
        super(message, cause);
    }
}
