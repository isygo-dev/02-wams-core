package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("generate.key.material.exception")
public class GenerateKeyException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public GenerateKeyException(String s) {
        super(s);
    }

    public GenerateKeyException(String s, Throwable cause) {
        super(s, cause);
    }
}
