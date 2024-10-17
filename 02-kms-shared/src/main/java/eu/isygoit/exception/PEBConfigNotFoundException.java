package eu.isygoit.exception;


import eu.isygoit.annotation.MsgLocale;


/**
 * The type Peb config not found exception.
 */
@MsgLocale("peb.config.not.found.exception")
public class PEBConfigNotFoundException extends ManagedException {

    /**
     * Instantiates a new Peb config not found exception.
     *
     * @param s the s
     */
    public PEBConfigNotFoundException(String s) {
    }
}
