package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;


@MsgLocale("customkeystoreconnectionexception.exception")
public class CustomKeyStoreConnectionException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public CustomKeyStoreConnectionException(String s) {
        super(s);
    }

    public CustomKeyStoreConnectionException(String s, Throwable cause) {
        super(s, cause);
    }
}
