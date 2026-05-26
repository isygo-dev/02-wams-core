package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("import.keymaterial.exception")
public class ImportKeyMaterialException extends ManagedException {

    /**
     * Instantiates a new Token config not found exception.
     *
     * @param s the s
     */
    public ImportKeyMaterialException(String s) {
        super(s);
    }

    /**
     * Instantiates a new Alias not found exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public ImportKeyMaterialException(String message, Throwable cause) {
        super(message, cause);
    }
}
