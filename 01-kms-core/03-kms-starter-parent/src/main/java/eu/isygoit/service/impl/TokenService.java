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

    @Override
    public TokenDto buildToken(String domain, String application, IEnumAppToken.Types tokenType, String subject, Map<String, Object> claims) {
        var tokenConfig = getTokenConfig(domain, tokenType);
        String tokenSubject = subject.toLowerCase() + "@" + domain;

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
    public TokenDto buildTokenAndSave(String domain, String application, IEnumAppToken.Types tokenType, String subject, Map<String, Object> claims) {
        var tokenConfig = getTokenConfig(domain, tokenType);
        String tokenSubject = subject.toLowerCase() + "@" + domain;

        TokenDto token = super.createToken(
                tokenSubject,
                claims,
                tokenConfig.getIssuer(),
                tokenConfig.getAudience(),
                SignatureAlgorithm.valueOf(tokenConfig.getSignatureAlgorithm()),
                tokenConfig.getSecretKey(),
                tokenConfig.getLifeTimeInMs()
        );

        saveAccessToken(token, application, tokenType, subject);
        return token;
    }

    private TokenConfig getTokenConfig(String domain, IEnumAppToken.Types tokenType) {
        return tokenConfigService.buildTokenConfig(domain, tokenType)
                .orElseThrow(() -> new TokenConfigNotFoundException("for domain: " + domain + "/" + tokenType.name()));
    }

    private void saveAccessToken(TokenDto token, String application, IEnumAppToken.Types tokenType, String subject) {
        var accessToken = AccessToken.builder()
                .tokenType(tokenType)
                .application(application)
                .token(token.getToken())
                .expiryDate(token.getExpiryDate())
                .accountCode(subject)
                .deprecated(false)
                .build();
        accessTokenService.create(accessToken);
    }

    @Override
    public boolean isTokenValid(String domain, String application, IEnumAppToken.Types tokenType, String token, String subject) {
        var tokenConfig = getTokenConfig(domain, tokenType);
        super.validateToken(token, subject, tokenConfig.getSecretKey());

        String userName = subject.split("@")[0];

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
        Account account = domainService.checkAccountIfExists(domain, null, null, accountCode, null, false)
                .orElseThrow(() -> {
                    String errorMessage = String.format("Account not found for domain/username: %s/%s", domain, accountCode);
                    log.error(errorMessage);
                    return new UserNotFoundException(errorMessage);
                });

        TokenDto token = generateResetPasswordToken(domain, application, accountCode, account);
        sendForgotPasswordEmail(domain, application, account, token);
    }

    private TokenDto generateResetPasswordToken(String domain, String application, String accountCode, Account account) {
        return this.buildTokenAndSave(domain, application, IEnumAppToken.Types.RSTPWD, accountCode,
                Map.of(JwtConstants.JWT_SENDER_DOMAIN, domain,
                        JwtConstants.JWT_SENDER_USER, accountCode,
                        JwtConstants.JWT_LOG_APP, application));
    }

    private void sendForgotPasswordEmail(String domain, String application, Account account, TokenDto token) throws JsonProcessingException {
        String resetPwdUrl = Optional.ofNullable(imsAppParameterService.getValueByDomainAndName(RequestContextDto.builder().build(),
                        domain, AppParameterConstants.APPURL + "." + application.toUpperCase(), true, "http://localhost:4000/reset-password/"))
                .filter(result -> result.getStatusCode().is2xxSuccessful() && result.hasBody())
                .map(ResponseEntity::getBody)
                .orElse("http://localhost:4000/reset-password/");

        var variables = Map.of(
                MsgTemplateVariables.V_USER_NAME, account.getCode(),
                MsgTemplateVariables.V_FULLNAME, account.getFullName(),
                MsgTemplateVariables.V_DOMAIN_NAME, account.getDomain(),
                MsgTemplateVariables.V_RESET_TOKEN, token.getToken(),
                MsgTemplateVariables.V_RESET_PWD_URL, resetPwdUrl + "/reset-password/?token="
        );

        var mailMessageDto = MailMessageDto.builder()
                .subject(EmailSubjects.FORGOT_PASSWORD_EMAIL_SUBJECT)
                .domain(domain)
                .toAddr(account.getEmail())
                .templateName(IEnumMsgTemplateName.Types.FORGOT_PASSWORD_TEMPLATE)
                .variables(MailMessageDto.getVariablesAsString(variables))
                .build();

        msgService.sendMessage(domain, mailMessageDto, appProperties.isSendAsyncEmail());
    }

    @Override
    public TokenDto createAccessToken(String domain, String application, String userName, Boolean isAdmin) {
        return this.buildTokenAndSave(domain, application, IEnumAppToken.Types.ACCESS, userName,
                Map.of(JwtConstants.JWT_SENDER_DOMAIN, domain,
                        JwtConstants.JWT_SENDER_USER, userName,
                        JwtConstants.JWT_LOG_APP, application,
                        JwtConstants.JWT_IS_ADMIN, isAdmin));
    }

    @Override
    public TokenDto createRefreshToken(String domain, String application, String userName) {
        return this.buildTokenAndSave(domain, application, IEnumAppToken.Types.REFRESH, userName,
                Map.of(JwtConstants.JWT_SENDER_DOMAIN, domain,
                        JwtConstants.JWT_SENDER_USER, userName,
                        JwtConstants.JWT_LOG_APP, application));
    }

    @Override
    public TokenDto createAuthorityToken(String domain, String application, String userName, List<String> authorities) {
        return this.buildToken(domain, application, IEnumAppToken.Types.AUTHORITY, userName,
                Map.of(JwtConstants.JWT_SENDER_DOMAIN, domain,
                        JwtConstants.JWT_SENDER_USER, userName,
                        JwtConstants.JWT_LOG_APP, application,
                        JwtConstants.JWT_GRANTED_AUTHORITY, authorities));
    }
}