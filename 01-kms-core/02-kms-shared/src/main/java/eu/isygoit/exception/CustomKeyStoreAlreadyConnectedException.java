package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("customkeystorealreadyconnectedexception.exception")
public class CustomKeyStoreAlreadyConnectedException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public CustomKeyStoreAlreadyConnectedException(String s) {
        super(s);
    }

    public CustomKeyStoreAlreadyConnectedException(String s, Throwable cause) {
        super(s, cause);
    }
}
