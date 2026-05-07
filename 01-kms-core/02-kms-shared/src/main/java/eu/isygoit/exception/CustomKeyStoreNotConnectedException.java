package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;


@MsgLocale("customkeystorenotconnectedexception.exception")
public class CustomKeyStoreNotConnectedException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public CustomKeyStoreNotConnectedException(String s) {
        super(s);
    }
}
