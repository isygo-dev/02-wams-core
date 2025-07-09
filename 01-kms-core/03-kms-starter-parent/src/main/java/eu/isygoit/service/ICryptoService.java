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
     * @param tenant the tenant
     * @return the peb encryptor
     */
    StringEncryptor getPebEncryptor(String tenant);

    /**
     * Gets digest encryptor.
     *
     * @param tenant the tenant
     * @return the digest encryptor
     */
    StringDigester getDigestEncryptor(String tenant);

    /**
     * Gets password encryptor.
     *
     * @param tenant the tenant
     * @return the password encryptor
     */
    PasswordEncryptor getPasswordEncryptor(String tenant);
}
