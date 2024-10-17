package eu.isygoit.service;

import org.jasypt.digest.StringDigester;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.util.password.PasswordEncryptor;

/**
 * The interface Crypto service.
 */
public interface ICryptoService {

    /**
     * Gets peb encryptor.
     *
     * @param domain the domain
     * @return the peb encryptor
     */
    StringEncryptor getPebEncryptor(String domain);

    /**
     * Gets digest encryptor.
     *
     * @param domain the domain
     * @return the digest encryptor
     */
    StringDigester getDigestEncryptor(String domain);

    /**
     * Gets password encryptor.
     *
     * @param domain the domain
     * @return the password encryptor
     */
    PasswordEncryptor getPasswordEncryptor(String domain);
}
