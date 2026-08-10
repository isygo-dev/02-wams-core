package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("transfer.not.supported.exception")
public class TransferNotSupportedException extends ManagedException {

    public TransferNotSupportedException(String message) {
        super(message);
    }

    public TransferNotSupportedException(String message, Throwable cause) {
        super(message, cause);
    }
}
