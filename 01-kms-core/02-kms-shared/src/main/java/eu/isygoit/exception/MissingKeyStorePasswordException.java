package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("MissingKeyStorePasswordException.exception")
public class MissingKeyStorePasswordException extends ManagedException {

    /**
     * Instantiates a new Kms tenant update exception.
     *
     * @param s the s
     */
    public MissingKeyStorePasswordException(String s) {
        super(s);
    }

    public MissingKeyStorePasswordException(String s, Throwable cause) {
        super(s, cause);
    }
}
