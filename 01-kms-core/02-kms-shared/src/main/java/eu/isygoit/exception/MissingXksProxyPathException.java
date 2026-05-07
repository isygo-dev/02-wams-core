package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("MissingXksProxyPathException.exception")
public class MissingXksProxyPathException extends ManagedException {

    /**
     * Instantiates a new Kms tenant update exception.
     *
     * @param s the s
     */
    public MissingXksProxyPathException(String s) {
        super(s);
    }
}
