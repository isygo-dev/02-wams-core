package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("CustomKeyStoreNotFoundException.exception")
public class CustomKeyStoreNotFoundException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public CustomKeyStoreNotFoundException(String s) {
        super(s);
    }
}
