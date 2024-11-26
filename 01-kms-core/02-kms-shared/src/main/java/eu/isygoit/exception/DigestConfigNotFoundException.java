package eu.isygoit.exception;


import eu.isygoit.annotation.MsgLocale;


/**
 * The type Digest config not found exception.
 */
@MsgLocale("digest.config.not.found.exception")
public class DigestConfigNotFoundException extends ManagedException {

    /**
     * Instantiates a new Digest config not found exception.
     *
     * @param s the s
     */
    public DigestConfigNotFoundException(String s) {
        super(s);
    }
}
