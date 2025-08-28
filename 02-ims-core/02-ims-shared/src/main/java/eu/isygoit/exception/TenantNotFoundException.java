package eu.isygoit.exception;


import eu.isygoit.annotation.MsgLocale;


/**
 * The type Account not found exception.
 */
@MsgLocale("tenant.not.found.exception")
public class TenantNotFoundException extends ManagedException {

    /**
     * Instantiates a new Account not found exception.
     *
     * @param s the s
     */
    public TenantNotFoundException(String s) {
        super(s);
    }
}
