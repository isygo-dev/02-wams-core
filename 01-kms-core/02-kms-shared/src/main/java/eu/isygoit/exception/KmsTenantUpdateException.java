package eu.isygoit.exception;


import eu.isygoit.annotation.MsgLocale;


/**
 * The type Kms tenant update exception.
 */
@MsgLocale("kms.tenant.update.exception")
public class KmsTenantUpdateException extends ManagedException {

    /**
     * Instantiates a new Kms tenant update exception.
     *
     * @param s the s
     */
    public KmsTenantUpdateException(String s) {
        super(s);
    }
}
