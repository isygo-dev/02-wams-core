package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("MissingCloudHsmClusterIdException.exception")
public class MissingCloudHsmClusterIdException extends ManagedException {

    /**
     * Instantiates a new Kms tenant update exception.
     *
     * @param s the s
     */
    public MissingCloudHsmClusterIdException(String s) {
        super(s);
    }

    public MissingCloudHsmClusterIdException(String s, Throwable cause) {
        super(s, cause);
    }
}
