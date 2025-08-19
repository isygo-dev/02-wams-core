package eu.isygoit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.isygoit.config.AppProperties;
import eu.isygoit.constants.AppParameterConstants;
import eu.isygoit.constants.JwtConstants;
import eu.isygoit.dto.common.ContextRequestDto;
import eu.isygoit.dto.common.TokenResponseDto;
import eu.isygoit.dto.data.MailMessageDto;
import eu.isygoit.enums.IEnumEmailTemplate;
import eu.isygoit.enums.IEnumToken;
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

/**
 * The type Token service.
 */
@Slf4j
@Service
@Transactional
public class TokenService extends JwtService implements ITokenService {

    private final AppProperties appProperties;

    @Autowired
    private ITokenConfigService tokenConfigService;
    @Autowired
    private IDomainService tenantService;
    @Autowired
    private IMsgService msgService;
    @Autowired
    private IAccessTokenService accessTokenService;
    @Autowired
    private ImsAppParameterService imsAppParameterService;

    /**
     * Instantiates a new Token service.
     *
     * @param appProperties the app properties
     */
    public TokenService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public TokenResponseDto buildTokenAndSave(String tenant, String application, IEnumToken.Types tokenType, String subject, Map<String, Object> claims) {
        //Get Token config configured by tenant and type, otherwise, default one
        TokenConfig tokenConfig = tokenConfigService.buildTokenConfig(tenant, tokenType);
        if (tokenConfig != null) {
            TokenResponseDto token = super.createToken(new StringBuilder(subject.toLowerCase()).append("@").append(tenant).toString(),
                    claims,
                    tokenConfig.getIssuer(),
                    tokenConfig.getAudience(),
                    SignatureAlgorithm.valueOf(tokenConfig.getSignatureAlgorithm()),
                    tokenConfig.getSecretKey(),
                    tokenConfig.getLifeTimeInMs());
            //Save generated token
            AccessToken accessToken = AccessToken.builder()
                    .tokenType(tokenType)
                    .application(application)
                    .token(token.getToken())
                    .expiryDate(token.getExpiryDate())
                    .accountCode(subject)
                    .deprecated(Boolean.FALSE)
                    .build();
            accessTokenService.create(accessToken);
            return token;
        } else {
            throw new TokenConfigNotFoundException("for tenant: " + tenant + "/" + tokenType.name());
        }
    }

    public TokenResponseDto buildToken(String tenant, String application, IEnumToken.Types tokenType, String subject, Map<String, Object> claims) {
        //Get Token config configured by tenant and type, otherwise, default one
        TokenConfig tokenConfig = tokenConfigService.buildTokenConfig(tenant, tokenType);
        if (tokenConfig != null) {
            TokenResponseDto token = super.createToken(new StringBuilder(subject.toLowerCase()).append("@").append(tenant).toString(),
                    claims,
                    tokenConfig.getIssuer(),
                    tokenConfig.getAudience(),
                    SignatureAlgorithm.valueOf(tokenConfig.getSignatureAlgorithm()),
                    tokenConfig.getSecretKey(),
                    tokenConfig.getLifeTimeInMs());
            return token;
        } else {
            throw new TokenConfigNotFoundException("for tenant: " + tenant + "/" + tokenType.name());
        }
    }

    @Override
    public boolean isTokenValid(String tenant, String application, IEnumToken.Types tokenType, String token, String subject) {
        //Get Token config configured by tenant and type, otherwise, default one
        TokenConfig tokenConfig = tokenConfigService.buildTokenConfig(tenant, tokenType);
        if (tokenConfig != null) {
            //Validate token content
            super.validateToken(token, subject, tokenConfig.getSecretKey());
        } else {
            log.error("Token config not found for tenant: " + tenant + "/" + tokenType.name());
            throw new TokenConfigNotFoundException("for tenant: " + tenant + "/" + tokenType.name());
        }

        //Validate token existance
        String[] userNameArray = subject.split("@");
        //TEMP COMMENTED: AccessToken accessToken = accessTokenService.findByApplicationAndAccountCodeAndTokenAndTokenType(application, userNameArray[0], token, tokenType);
        AccessToken accessToken = accessTokenService.findByAccountCodeAndTokenAndTokenType(userNameArray[0], token, tokenType);
        if (accessToken == null) {
            log.error("Token not found for tenant: " + tenant + "/" + tokenType.name());
            throw new TokenInvalidException("Invalid JWT: not found or deprecated");
        }
        return true;
    }

    @Override
    public void createForgotPasswordAccessToken(String tenant, String application, String accountCode) throws JsonProcessingException {
        //Get the account
        Account account = tenantService.checkAccountIfExists(tenant, null, null, accountCode, null, false);
        if (account == null) {
            log.error("Account not found for tenant/username: " + tenant + "/" + accountCode);
            throw new UserNotFoundException("tenant/username: " + tenant + "/" + accountCode);
        }

        //Generate reset password token
        TokenResponseDto token = this.buildTokenAndSave(tenant, application, IEnumToken.Types.RSTPWD, accountCode,
                Map.of(JwtConstants.JWT_SENDER_TENANT, tenant,
                        JwtConstants.JWT_SENDER_USER, accountCode,
                        JwtConstants.JWT_LOG_APP, application));

        //Send reset password email
        sendForgotPasswordEmail(tenant, application, account, token);
    }

    private void sendForgotPasswordEmail(String tenant, String application, Account account, TokenResponseDto token) throws JsonProcessingException {
        //Build Email template data
        String resetPwdUrl = "http://localhost:4000/reset-password/";
        try {
            ResponseEntity<String> result = imsAppParameterService.getValueByTenantAndName(ContextRequestDto.builder().build(),
                    tenant, AppParameterConstants.APPURL + "." + application.toUpperCase(), true, "http://localhost:4000/reset-password/");
            if (result.getStatusCode().is2xxSuccessful() && result.hasBody() && StringUtils.hasText(result.getBody())) {
                resetPwdUrl = result.getBody();
            }
        } catch (Exception e) {
            log.error("Remote feign call failed : ", e);
            //throw new RemoteCallFailedException(e);
        }

        MailMessageDto mailMessageDto = MailMessageDto.builder()
                .subject(EmailSubjects.FORGOT_PASSWORD_EMAIL_SUBJECT)
                .tenant(tenant)
                .toAddr(account.getEmail())
                .templateName(IEnumEmailTemplate.Types.FORGOT_PASSWORD_TEMPLATE)
                .variables(MailMessageDto.getVariablesAsString(Map.of(
                        //Common vars
                        MsgTemplateVariables.V_USER_NAME, account.getCode(),
                        MsgTemplateVariables.V_FULLNAME, account.getFullName(),
                        MsgTemplateVariables.V_TENANT_NAME, account.getTenant(),
                        //Specific vars
                        MsgTemplateVariables.V_RESET_TOKEN, token.getToken(),
                        MsgTemplateVariables.V_RESET_PWD_URL, resetPwdUrl + "/reset-password/?token=")))
                .build();
        //Send the email message
        msgService.sendMessage(tenant, mailMessageDto, appProperties.isSendAsyncEmail());
    }

    @Override
    public TokenResponseDto createAccessToken(String tenant, String application, String userName, Boolean isAdmin) {
        TokenResponseDto token = this.buildTokenAndSave(tenant, application, IEnumToken.Types.ACCESS, userName,
                Map.of(JwtConstants.JWT_SENDER_TENANT, tenant,
                        JwtConstants.JWT_SENDER_USER, userName,
                        JwtConstants.JWT_LOG_APP, application,
                        JwtConstants.JWT_IS_ADMIN, isAdmin)
        );
        return token;
    }

    @Override
    public TokenResponseDto createRefreshToken(String tenant, String application, String userName) {
        TokenResponseDto token = this.buildTokenAndSave(tenant, application, IEnumToken.Types.REFRESH, userName,
                Map.of(JwtConstants.JWT_SENDER_TENANT, tenant,
                        JwtConstants.JWT_SENDER_USER, userName,
                        JwtConstants.JWT_LOG_APP, application)
        );
        return token;
    }

    @Override
    public TokenResponseDto createAuthorityToken(String tenant, String application, String userName, List<String> authorities) {
        TokenResponseDto token = this.buildToken(tenant, application, IEnumToken.Types.AUTHORITY, userName,
                Map.of(JwtConstants.JWT_SENDER_TENANT, tenant,
                        JwtConstants.JWT_SENDER_USER, userName,
                        JwtConstants.JWT_LOG_APP, application,
                        JwtConstants.JWT_GRANTED_AUTHORITY, authorities)
        );
        return token;
    }
}
