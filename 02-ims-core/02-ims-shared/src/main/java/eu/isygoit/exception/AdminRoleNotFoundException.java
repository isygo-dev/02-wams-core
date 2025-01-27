package eu.isygoit.exception;


import eu.isygoit.annotation.MsgLocale;


/**
 * The type Account authentication exception.
 */
@MsgLocale("admin.role.not.found.exception")
public class AdminRoleNotFoundException extends ManagedException {

    /**
     * Instantiates a new Account authentication exception.
     *
     * @param s the s
     */
    public AdminRoleNotFoundException(String s) {
        super(s);
    }
}
