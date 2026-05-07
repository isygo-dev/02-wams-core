package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;


@MsgLocale("customkeystoreconnectingexception.exception")
public class CustomKeyStoreConnectingException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public CustomKeyStoreConnectingException(String s) {
        super(s);
    }
}
