package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("TenantCustomKeyStoreLimitExceededException.exception")
public class TenantCustomKeyStoreLimitExceededException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public TenantCustomKeyStoreLimitExceededException(String s) {
        super(s);
    }
}