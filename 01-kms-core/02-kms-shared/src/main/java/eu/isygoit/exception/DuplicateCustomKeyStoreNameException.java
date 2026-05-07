package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("duplicate.custom.key.store.name.exception")
public class DuplicateCustomKeyStoreNameException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public DuplicateCustomKeyStoreNameException(String s) {
        super(s);
    }
}
