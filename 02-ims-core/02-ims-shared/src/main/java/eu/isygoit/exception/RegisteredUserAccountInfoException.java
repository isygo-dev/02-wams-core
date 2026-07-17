package eu.isygoit.exception;


import eu.isygoit.annotation.MsgLocale;


/**
 * The type Registered user not found exception.
 */
@MsgLocale("registered.user.account.info.exception")
public class RegisteredUserAccountInfoException extends ManagedException {

    /**
     * Instantiates a new Account authentication exception.
     *
     * @param s the s
     */
    public RegisteredUserAccountInfoException(String s) {
        super(s);
    }
}
