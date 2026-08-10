package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("invalid.multiregion.key.exception")
public class InvalidMultiRegionKeyException extends ManagedException {

    public InvalidMultiRegionKeyException(String message) {
        super(message);
    }

    public InvalidMultiRegionKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
