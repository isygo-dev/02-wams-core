package eu.isygoit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.isygoit.dto.common.ResetPwdViaTokenRequestDto;
import eu.isygoit.dto.response.AccessKeyResponseDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumPasswordStatus;
import eu.isygoit.exception.TokenInvalidException;
import eu.isygoit.exception.UnsuportedAuthTypeException;
import eu.isygoit.exception.UserNotFoundException;
import eu.isygoit.exception.UserPasswordNotFoundException;
import eu.isygoit.model.Account;

/**
 * The interface Password service.
 */
public interface IPasswordService {

    /**
     * Generate random password access key response dto.
     *
     * @param domain    the domain
     * @param domainUrl the domain url
     * @param email     the email
     * @param userName  the user name
     * @param fullName  the full name
     * @param authType  the auth type
     * @return the access key response dto
     * @throws JsonProcessingException the json processing exception
     */
    AccessKeyResponseDto generateRandomPassword(String domain, String domainUrl, String email, String userName, String fullName, IEnumAuth.Types authType) throws JsonProcessingException;


    /**
     * Force change password.
     *
     * @param domain     the domain
     * @param userName   the user name
     * @param newPasswor the new passwor
     */
    void forceChangePassword(String domain, String userName, String newPasswor);


    /**
     * Change password.
     *
     * @param domain      the domain
     * @param userName    the user name
     * @param oldPassword the old password
     * @param newPassword the new password
     */
    void changePassword(String domain, String userName, String oldPassword, String newPassword);


    /**
     * Check for pattern boolean.
     *
     * @param domain        the domain
     * @param plainPassword the plain password
     * @return the boolean
     */
    boolean checkForPattern(String domain, String plainPassword);

    /**
     * Matches enum password status . types.
     *
     * @param domain        the domain
     * @param userName      the user name
     * @param plainPassword the plain password
     * @param authType      the auth type
     * @return the enum password status . types
     * @throws UserPasswordNotFoundException the user password not found exception
     * @throws UserNotFoundException         the user not found exception
     */
    IEnumPasswordStatus.Types matches(String domain, String userName, String plainPassword, IEnumAuth.Types authType) throws UserPasswordNotFoundException, UserNotFoundException;


    /**
     * Sign password int [ ].
     *
     * @param password the password
     * @return the int [ ]
     */
    long[] signPassword(String password);

    /**
     * Is expired boolean.
     *
     * @param domain   the domain
     * @param email    the email
     * @param userName the user name
     * @param authType the auth type
     * @return the boolean
     * @throws UserPasswordNotFoundException the user password not found exception
     * @throws UserNotFoundException         the user not found exception
     */
    Boolean isExpired(String domain, String email, String userName, IEnumAuth.Types authType) throws UserPasswordNotFoundException, UserNotFoundException;

    /**
     * Reset password via token.
     *
     * @param resetPwdViaTokenRequestDto the reset pwd via token request dto
     * @throws TokenInvalidException the token invalid exception
     */
    void resetPasswordViaToken(ResetPwdViaTokenRequestDto resetPwdViaTokenRequestDto) throws TokenInvalidException;

    /**
     * Register new password access key response dto.
     *
     * @param domain      the domain
     * @param account     the account
     * @param newPassword the new password
     * @param authType    the auth type
     * @return the access key response dto
     * @throws UnsuportedAuthTypeException the unsuported auth type exception
     */
    AccessKeyResponseDto registerNewPassword(String domain, Account account, String newPassword, IEnumAuth.Types authType) throws UnsuportedAuthTypeException;
}
