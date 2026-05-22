package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("invalid.cipher.text.exception")
public class InvalidCipherTextException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public InvalidCipherTextException(String s) {
        super(s);
    }

    /**
     * Instantiates a new Alias not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public InvalidCipherTextException(String message, Throwable cause) {
        super(message, cause);
    }
}
