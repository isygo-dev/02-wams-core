package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;


@MsgLocale("unsupportedcustomkeystoretypeexception.exception")
public class UnsupportedCustomKeyStoreTypeException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public UnsupportedCustomKeyStoreTypeException(String s) {
        super(s);
    }
}
