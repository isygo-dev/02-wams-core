package eu.isygoit.exception;


import eu.isygoit.annotation.MsgLocale;


/**
 * The type Account authentication exception.
 */
@MsgLocale("account.authentication.exception")
public class AccountAuthenticationException extends ManagedException {

    /**
     * Instantiates a new Account authentication exception.
     *
     * @param s the s
     */
    public AccountAuthenticationException(String s) {
        super(s);
    }
}
