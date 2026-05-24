package eu.isygoit.exception;

import eu.isygoit.annotation.MsgLocale;

@MsgLocale("missing.trust.anchor.certificate.exception.exception")
public class MissingTrustAnchorCertificateException extends ManagedException {

    /**
     * Instantiates a new Kms tenant update exception.
     *
     * @param s the s
     */
    public MissingTrustAnchorCertificateException(String s) {
        super(s);
    }

    public MissingTrustAnchorCertificateException(String s, Throwable cause) {
        super(s, cause);
    }
}
