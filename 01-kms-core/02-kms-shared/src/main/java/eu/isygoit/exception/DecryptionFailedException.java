package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("decryption.failed.exception")
public class DecryptionFailedException extends ManagedException {

    public DecryptionFailedException(String message) {
        super(message);
    }

    public DecryptionFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
