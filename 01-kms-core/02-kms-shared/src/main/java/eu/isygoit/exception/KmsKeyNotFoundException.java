package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("kms.key.not.found.exception")
public class KmsKeyNotFoundException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public KmsKeyNotFoundException(String s) {
        super(s);
    }
}
