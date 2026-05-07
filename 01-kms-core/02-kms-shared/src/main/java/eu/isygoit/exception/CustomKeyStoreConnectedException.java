package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("custom.key.store.connected.exception")
public class CustomKeyStoreConnectedException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public CustomKeyStoreConnectedException(String s) {
        super(s);
    }
}

