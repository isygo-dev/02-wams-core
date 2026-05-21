package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("CustomKeyStoreDisconnectionException.exception")
public class CustomKeyStoreDisconnectionException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public CustomKeyStoreDisconnectionException(String s) {
        super(s);
    }

    public CustomKeyStoreDisconnectionException(String s, Throwable cause) {
        super(s, cause);
    }
}
