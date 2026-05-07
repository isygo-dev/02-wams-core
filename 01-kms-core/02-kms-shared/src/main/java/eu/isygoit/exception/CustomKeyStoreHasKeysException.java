package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("customkeystorehaskeysexception.exception")
public class CustomKeyStoreHasKeysException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public CustomKeyStoreHasKeysException(String s) {
        super(s);
    }
}