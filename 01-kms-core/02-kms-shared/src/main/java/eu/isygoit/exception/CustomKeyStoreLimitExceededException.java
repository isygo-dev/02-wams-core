package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("TenantCustomKeyStoreLimitExceededException.exception")
public class CustomKeyStoreLimitExceededException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public CustomKeyStoreLimitExceededException(String s) {
        super(s);
    }

    public CustomKeyStoreLimitExceededException(String s, Throwable cause) {
        super(s, cause);
    }
}