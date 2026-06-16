package eu.isygoit.exception;


import eu.isygoit.annotation.MsgLocale;


/**
 * The type Digest config not found exception.
 */
@MsgLocale("digest.config.composition.exception")
public class DigestConfigCompositionException extends ManagedException {

    /**
     * Instantiates a new Digest config not found exception.
     *
     * @param s the s
     */
    public DigestConfigCompositionException(String s) {
        super(s);
    }

    public DigestConfigCompositionException(String s, Throwable cause) {
        super(s, cause);
    }
}
