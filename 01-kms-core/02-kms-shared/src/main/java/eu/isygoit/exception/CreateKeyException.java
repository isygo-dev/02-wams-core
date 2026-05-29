package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("create.key.exception")
public class CreateKeyException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public CreateKeyException(String s) {
        super(s);
    }

    public CreateKeyException(String s, Throwable cause) {
        super(s, cause);
    }
}
