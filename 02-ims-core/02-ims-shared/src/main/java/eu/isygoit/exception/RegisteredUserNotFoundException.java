package eu.isygoit.exception;


import eu.isygoit.annotation.MsgLocale;


/**
 * The type Registered user not found exception.
 */
@MsgLocale("registered.user.not.found.exception")
public class RegisteredUserNotFoundException extends ManagedException {

    /**
     * Instantiates a new Account authentication exception.
     *
     * @param s the s
     */
    public RegisteredUserNotFoundException(String s) {
        super(s);
    }
}
