package eu.isygoit.service;

/**
 * The interface Auth service.
 */
public interface IAuthService {

    /**
     * Authenticate string.
     *
     * @param userName the user name
     * @param password the password
     * @return the string
     */
    String authenticate(String userName, String password);
}
