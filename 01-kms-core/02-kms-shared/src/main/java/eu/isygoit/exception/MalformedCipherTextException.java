package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("malformed.cipher.text.exception")
public class MalformedCipherTextException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public MalformedCipherTextException(String s) {
        super(s);
    }

    /**
     * Instantiates a new Alias not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public MalformedCipherTextException(String message, Throwable cause) {
        super(message, cause);
    }
}
