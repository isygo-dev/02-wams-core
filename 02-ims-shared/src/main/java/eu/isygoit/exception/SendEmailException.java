package eu.isygoit.exception;


import eu.isygoit.annotation.MsgLocale;


/**
 * The type Account authentication exception.
 */
@MsgLocale("send.email.exception")
public class SendEmailException extends ManagedException {

    /**
     * Instantiates a new Account authentication exception.
     *
     * @param s the s
     */
    public SendEmailException(String s) {
        super(s);
    }
}
