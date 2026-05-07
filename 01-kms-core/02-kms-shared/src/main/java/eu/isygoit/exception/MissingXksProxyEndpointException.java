package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;


@MsgLocale("MissingXksProxyEndpointException.exception")
public class MissingXksProxyEndpointException extends ManagedException {

    /**
     * Instantiates a new Kms tenant update exception.
     *
     * @param s the s
     */
    public MissingXksProxyEndpointException(String s) {
        super(s);
    }
}
