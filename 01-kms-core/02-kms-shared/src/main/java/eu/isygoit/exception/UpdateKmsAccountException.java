package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("update.kms.account.exception")
public class UpdateKmsAccountException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public UpdateKmsAccountException(String s) {
        super(s);
    }

    /**
     * Instantiates a new Alias not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public UpdateKmsAccountException(String message, Throwable cause) {
        super(message, cause);
    }
}