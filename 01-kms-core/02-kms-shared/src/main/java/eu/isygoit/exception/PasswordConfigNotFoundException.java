package eu.isygoit.exception;


import eu.isygoit.annotation.MsgLocale;


/**
 * The type Password config not found exception.
 */
@MsgLocale("password.config.not.found.exception")
public class PasswordConfigNotFoundException extends ManagedException {

    /**
     * Instantiates a new Password config not found exception.
     *
     * @param s the s
     */
    public PasswordConfigNotFoundException(String s) {
        super(s);
    }

    /**
     * Instantiates a new Password config not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public PasswordConfigNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
