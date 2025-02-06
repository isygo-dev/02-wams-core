package eu.isygoit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.isygoit.config.AppProperties;
import eu.isygoit.constants.AppParameterConstants;
import eu.isygoit.constants.JwtConstants;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.common.TokenDto;
import eu.isygoit.dto.data.MailMessageDto;
import eu.isygoit.enums.IEnumAppToken;
import eu.isygoit.enums.IEnumMsgTemplateName;
import eu.isygoit.exception.TokenConfigNotFoundException;
import eu.isygoit.exception.TokenInvalidException;
import eu.isygoit.exception.UserNotFoundException;
import eu.isygoit.jwt.JwtService;
import eu.isygoit.model.AccessToken;
import eu.isygoit.model.Account;
import eu.isygoit.model.TokenConfig;
import eu.isygoit.remote.ims.ImsAppParameterService;
import eu.isygoit.service.*;
import eu.isygoit.types.EmailSubjects;
import eu.isygoit.types.MsgTemplateVariables;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The type Token service.
 */
@Slf4j
@Service
@Transactional
public class TokenService extends JwtService implements ITokenService {

    private final AppProperties appProperties;

    private final ITokenConfigService tokenConfigService;
    private final IDomainService domainService;
    private final IMsgService msgService;
    private final IAccessTokenService accessTokenService;
    private final ImsAppParameterService imsAppParameterService;

    @Autowired
    public TokenService(AppProperties appProperties, ITokenConfigService tokenConfigService, IDomainService domainService, IMsgService msgService, IAccessTokenService accessTokenService, ImsAppParameterService imsAppParameterService) {
        this.appProperties = appProperties;
        this.tokenConfigService = tokenConfigService;
        this.domainService = domainService;
        this.msgService = msgService;
        this.accessTokenService = accessTokenService;
        this.imsAppParameterService = imsAppParameterService;
    }

    public TokenDto buildTokenAndSave(String domain, String application, IEnumAppToken.Types tokenType, String subject, Map<String, Object> claims) {
        // Get the TokenConfig, throw an exception if not found
        TokenConfig tokenConfig = tokenConfigService.buildTokenConfig(domain, tokenType)
                .orElseThrow(() -> new TokenConfigNotFoundException("for domain: " + domain + "/" + tokenType.name()));

        // Prepare the token subject with proper formatting
        String tokenSubject = subject.toLowerCase() + "@" + domain;

        // Create the token using the fetched config details
        TokenDto token = super.createToken(
                tokenSubject,
                claims,
                tokenConfig.getIssuer(),
                tokenConfig.getAudience(),
                SignatureAlgorithm.valueOf(tokenConfig.getSignatureAlgorithm()),
                tokenConfig.getSecretKey(),
                tokenConfig.getLifeTimeInMs()
        );

        // Save the generated token
        saveAccessToken(token, application, tokenType, subject, token);

        return token;
    }

    private void saveAccessToken(TokenDto token, String application, IEnumAppToken.Types tokenType, String subject, TokenDto generatedToken) {
        AccessToken accessToken = AccessToken.builder()
                .tokenType(tokenType)
                .application(application)
                .token(generatedToken.getToken())
                .expiryDate(generatedToken.getExpiryDate())
                .accountCode(subject)
                .deprecated(Boolean.FALSE)
                .build();
        accessTokenService.create(accessToken);
    }

    @Override
    public TokenDto buildToken(String domain, String application, IEnumAppToken.Types tokenType, String subject, Map<String, Object> claims) {
        // Get TokenConfig, throw exception if not present
        TokenConfig tokenConfig = tokenConfigService.buildTokenConfig(domain, tokenType)
                .orElseThrow(() -> new TokenConfigNotFoundException("for domain: " + domain + "/" + tokenType.name()));

        // Create token subject (use simple string concatenation or String.format)
        String tokenSubject = subject.toLowerCase() + "@" + domain;

        // Create and return the token using the tokenConfig
        return super.createToken(
                tokenSubject,
                claims,
                tokenConfig.getIssuer(),
                tokenConfig.getAudience(),
                SignatureAlgorithm.valueOf(tokenConfig.getSignatureAlgorithm()),
                tokenConfig.getSecretKey(),
                tokenConfig.getLifeTimeInMs()
        );
    }

    @Override
    public boolean isTokenValid(String domain, String application, IEnumAppToken.Types tokenType, String token, String subject) {
        // Get TokenConfig, throw exception if not found
        TokenConfig tokenConfig = tokenConfigService.buildTokenConfig(domain, tokenType)
                .orElseThrow(() -> new TokenConfigNotFoundException("for domain: " + domain + "/" + tokenType.name()));

        // Validate token content
        super.validateToken(token, subject, tokenConfig.getSecretKey());

        // Extract the username from subject (split only once)
        String userName = subject.split("@")[0];

        // Validate token existence
        accessTokenService.findByAccountCodeAndTokenAndTokenType(userName, token, tokenType)
                .ifPresentOrElse(
                        accessToken -> {}, // Token found, do nothing
                        () -> {
                            log.error("Token not found for domain: {} / token type: {}", domain, tokenType.name());
                            throw new TokenInvalidException("Invalid JWT: not found or deprecated");
                        });

        return true;
    }


    @Override
    public void createForgotPasswordAccessToken(String domain, String application, String accountCode) throws JsonProcessingException {
        // Get the account, throw an exception if not found
        Account account = domainService.checkAccountIfExists(domain, null, null, accountCode, null, false)
                .orElseThrow(() -> {
                    String errorMessage = String.format("Account not found for domain/username: %s/%s", domain, accountCode);
                    log.error(errorMessage);
                    return new UserNotFoundException(errorMessage);
                });

        // Generate reset password token
        TokenDto token = this.buildTokenAndSave(domain, application, IEnumAppToken.Types.RSTPWD, accountCode,
                Map.of(JwtConstants.JWT_SENDER_DOMAIN, domain,
                        JwtConstants.JWT_SENDER_USER, accountCode,
                        JwtConstants.JWT_LOG_APP, application));

        // Send reset password email
        try {
            sendForgotPasswordEmail(domain, application, account, token);
        } catch (JsonProcessingException e) {
            String errorMessage = String.format("Failed to send forgot password email for domain/username: %s/%s", domain, accountCode);
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }


    private void sendForgotPasswordEmail(String domain, String application, Account account, TokenDto token) throws JsonProcessingException {
        // Build Email template data
        String resetPwdUrl = Optional.ofNullable(imsAppParameterService.getValueByDomainAndName(RequestContextDto.builder().build(),
                        domain, AppParameterConstants.APPURL + "." + application.toUpperCase(), true, "http://localhost:4000/reset-password/"))
                .filter(result -> result.getStatusCode().is2xxSuccessful() && result.hasBody())
                .map(ResponseEntity::getBody)
                .orElse("http://localhost:4000/reset-password/");

        // Create the variables map for the email template
        var variables = Map.of(
                // Common vars
                MsgTemplateVariables.V_USER_NAME, account.getCode(),
                MsgTemplateVariables.V_FULLNAME, account.getFullName(),
                MsgTemplateVariables.V_DOMAIN_NAME, account.getDomain(),
                // Specific vars
                MsgTemplateVariables.V_RESET_TOKEN, token.getToken(),
                MsgTemplateVariables.V_RESET_PWD_URL, resetPwdUrl + "/reset-password/?token="
        );

        // Build the mail message
        var mailMessageDto = MailMessageDto.builder()
                .subject(EmailSubjects.FORGOT_PASSWORD_EMAIL_SUBJECT)
                .domain(domain)
                .toAddr(account.getEmail())
                .templateName(IEnumMsgTemplateName.Types.FORGOT_PASSWORD_TEMPLATE)
                .variables(MailMessageDto.getVariablesAsString(variables))
                .build();

        // Send the email message asynchronously or synchronously based on the configuration
        msgService.sendMessage(domain, mailMessageDto, appProperties.isSendAsyncEmail());
    }


    @Override
    public TokenDto createAccessToken(String domain, String application, String userName, Boolean isAdmin) {
        return this.buildTokenAndSave(domain, application, IEnumAppToken.Types.ACCESS, userName,
                Map.of(JwtConstants.JWT_SENDER_DOMAIN, domain,
                        JwtConstants.JWT_SENDER_USER, userName,
                        JwtConstants.JWT_LOG_APP, application,
                        JwtConstants.JWT_IS_ADMIN, isAdmin)
        );
    }

    @Override
    public TokenDto createRefreshToken(String domain, String application, String userName) {
        return this.buildTokenAndSave(domain, application, IEnumAppToken.Types.REFRESH, userName,
                Map.of(JwtConstants.JWT_SENDER_DOMAIN, domain,
                        JwtConstants.JWT_SENDER_USER, userName,
                        JwtConstants.JWT_LOG_APP, application)
        );
    }

    @Override
    public TokenDto createAuthorityToken(String domain, String application, String userName, List<String> authorities) {
       return this.buildToken(domain, application, IEnumAppToken.Types.AUTHORITY, userName,
                Map.of(JwtConstants.JWT_SENDER_DOMAIN, domain,
                        JwtConstants.JWT_SENDER_USER, userName,
                        JwtConstants.JWT_LOG_APP, application,
                        JwtConstants.JWT_GRANTED_AUTHORITY, authorities)
        );
    }
}
