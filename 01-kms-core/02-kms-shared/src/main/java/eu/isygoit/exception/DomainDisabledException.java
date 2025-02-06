package eu.isygoit.exception;


import eu.isygoit.annotation.MsgLocale;


/**
 * The type Digest config not found exception.
 */
@MsgLocale("domain.disabled.exception")
public class DomainDisabledException extends ManagedException {

    /**
     * Instantiates a new Digest config not found exception.
     *
     * @param s the s
     */
    public DomainDisabledException(String s) {
        super(s);
    }
}
