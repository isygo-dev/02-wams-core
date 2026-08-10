package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("file.conversion.exception")
public class FileConversionException extends ManagedException {

    public FileConversionException(String message) {
        super(message);
    }

    public FileConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
