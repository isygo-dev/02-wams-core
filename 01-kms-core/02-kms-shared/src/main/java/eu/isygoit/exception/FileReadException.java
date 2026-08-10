package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("file.read.exception")
public class FileReadException extends ManagedException {

    public FileReadException(String message) {
        super(message);
    }

    public FileReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
