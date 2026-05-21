package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("sign.data.exception")
public class SignDataException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public SignDataException(String s) {
        super(s);
    }

    /**
     * Instantiates a new Alias not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public SignDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
