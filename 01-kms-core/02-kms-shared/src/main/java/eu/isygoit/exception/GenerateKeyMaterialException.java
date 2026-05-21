package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("generate.key.material.exception")
public class GenerateKeyMaterialException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public GenerateKeyMaterialException(String s) {
        super(s);
    }

    public GenerateKeyMaterialException(String s, Throwable cause) {
        super(s, cause);
    }
}
