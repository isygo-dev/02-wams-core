package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("MissingXksProxyAuthCredentialException.exception")
public class MissingXksProxyAuthCredentialException extends ManagedException {

    /**
     * Instantiates a new Kms tenant update exception.
     *
     * @param s the s
     */
    public MissingXksProxyAuthCredentialException(String s) {
        super(s);
    }
}



