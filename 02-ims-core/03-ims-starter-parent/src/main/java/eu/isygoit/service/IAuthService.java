package eu.isygoit.service;

import eu.isygoit.dto.request.RequestTrackingDto;
import eu.isygoit.dto.response.AuthResponseDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.model.RegistredUser;

/**
 * The interface Auth service.
 */
public interface IAuthService {

    /**
     * Register new account boolean.
     *
     * @param registredNewAccount the registred new account
     * @return the boolean
     */
    boolean registerUser(RegistredUser registredNewAccount);

    /**
     * Authenticate auth response dto.
     *
     * @param requestTracking the request tracking
     * @param domain          the domain
     * @param userName        the user name
     * @param application     the application
     * @param password        the password
     * @param authType        the auth type
     * @return the auth response dto
     */
    AuthResponseDto authenticate(RequestTrackingDto requestTracking, String domain, String userName, String application, String password, IEnumAuth.Types authType);
}
