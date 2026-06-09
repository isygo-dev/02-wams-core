package eu.isygoit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.config.AppProperties;
import eu.isygoit.constants.AppParameterConstants;
import eu.isygoit.constants.JwtConstants;
import eu.isygoit.dto.common.TokenResponseDto;
import eu.isygoit.dto.data.MailMessageDto;
import eu.isygoit.enums.IEnumEmailTemplate;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.exception.*;
import eu.isygoit.helper.CRC16Helper;
import eu.isygoit.helper.CRC32Helper;
import eu.isygoit.jwt.JwtService;
import eu.isygoit.model.AccessToken;
import eu.isygoit.model.Account;
import eu.isygoit.model.TokenConfig;
import eu.isygoit.remote.ims.ImsAppParameterService;
import eu.isygoit.service.*;
import eu.isygoit.types.EmailSubjects;
import eu.isygoit.types.MsgTemplateVariables;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SecureDigestAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * The type Token service.
 */
@Slf4j
@Service
@Transactional
public class TokenService implements ITokenBuilderService {

    private final AppProperties appProperties;

    @Autowired
    private JwtService jwtService;
    @Autowired
    private ITokenConfigService tokenConfigService;
    @Autowired
    private ITenantService tenantService;
    @Autowired
    private IMsgService msgService;
    @Autowired
    private IAccessTokenService accessTokenService;
    @Autowired
    private ImsAppParameterService imsAppParameterService;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Instantiates a new Token service.
     *
     * @param appProperties the app properties
     */
    public TokenService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public TokenResponseDto buildTokenAndSave(String tenant, Set<String> audience, IEnumToken.Types tokenType, String subject, Map<String, Object> claims) {
        TokenResponseDto token = this.buildToken(tenant, audience, tokenType, subject, claims);

        Long crc16 = CRC16Helper.calculate(token.getToken().getBytes());
        Long crc32 = CRC32Helper.calculate(token.getToken().getBytes());
        //Save generated token
        AccessToken accessToken = null;
        try {
            accessToken = AccessToken.builder()
                    .tokenType(tokenType)
                    .application(objectMapper.writeValueAsString(audience))
                    .crc16(crc16)
                    .crc32(crc32)
                    .expiryDate(token.getExpiryDate())
                    .accountCode(subject)
                    .deprecated(Boolean.FALSE)
                    .build();
        } catch (JsonProcessingException e) {
            throw new TokenAudienceException("Error occurred while processing token audience", e);
        }
        accessTokenService.create(accessToken);
        return token;
    }

    @Override
    public TokenResponseDto buildToken(String tenant, Set<String> audience, IEnumToken.Types tokenType, String subject, Map<String, Object> claims) {
        //Get Token config configured by tenant and type, otherwise, default one
        TokenConfig tokenConfig = tokenConfigService.prepareTokenConfig(tenant, tokenType, null);
        if (tokenConfig != null) {
            if (CollectionUtils.isEmpty(tokenConfig.getAudience())) {
                log.error("Bad Token config: audience is required for tenant: {} / {}", tenant, tokenType.name());
                throw new TokenAudienceException("Bad Token config: audience is required");
            }

            if (!tokenConfig.getAudience().contains(TokenConfig.ALL_AUDIENCES)) {
                if (!tokenConfig.getAudience().containsAll(audience)) {
                    log.error("Token audience is invalid for tenant: {} / {}", tenant, tokenType.name());
                    throw new TokenAudienceException("Token audience is invalid");
                }
            }

            // Convert string algorithm to SecureDigestAlgorithm safely
            String sigAlgo = tokenConfig.getSignatureAlgorithm().toUpperCase();
            SecureDigestAlgorithm<?, ?> algorithm = Jwts.SIG.get().get(sigAlgo);
            if (algorithm == null) {
                throw new SignaturAlgorithmNotSupportedException("Unsupported signature algorithm: " + sigAlgo);
            }

            TokenResponseDto token = jwtService.createToken(new StringBuilder(subject.toLowerCase()).append("@").append(tenant).toString(),
                    claims,
                    StringUtils.hasText(tokenConfig.getKmsKeyVersion()) ? Map.of(JwtConstants.KID_VERSION, tokenConfig.getKmsKeyVersion()) : Collections.emptyMap(),
                    tokenConfig.getIssuer(),
                    tokenConfig.getAudience(),
                    algorithm,
                    tokenConfig.getSecretKey(),
                    tokenConfig.getLifeTimeInMs());

            return token;
        } else {
            throw new TokenConfigNotFoundException("for tenant: " + tenant + "/" + tokenType.name());
        }
    }

    @Override
    public boolean isTokenValid(String tenant, Set<String> audience, IEnumToken.Types tokenType, String token, String subject) {
        // Get Token config configured by tenant and type, otherwise, default one
        Optional<String> versionIdOpt = jwtService.extractKmsKeyVersionId(token);
        TokenConfig tokenConfig = tokenConfigService.prepareTokenConfig(tenant, tokenType, versionIdOpt.orElse(null));
        if (tokenConfig != null) {
            // Validate token content – pass both keys (the validate method will choose based on algorithm)
            jwtService.validateToken(token, subject, tokenConfig.getIssuer(), audience, tokenConfig.getSecretKey(), tokenConfig.getPublicKey());
        } else {
            log.error("Token config not found for tenant: {} / {}", tenant, tokenType.name());
            throw new TokenConfigNotFoundException("for tenant: " + tenant + "/" + tokenType.name());
        }

        // Validate token existence
        String[] subjectArray = subject.split("@");
        if (subjectArray.length != 2) {
            log.error("Invalid subject format in token: {} / {}", tenant, tokenType.name());
            throw new TokenSubjectException("Invalid JWT: subject format is invalid");
        }
        String tenantFromToken = subjectArray[1];
        String accountCodeFromToken = subjectArray[0];
        Long crc16 = CRC16Helper.calculate(token.getBytes());
        Long crc32 = CRC32Helper.calculate(token.getBytes());
        AccessToken accessToken = accessTokenService.findAccessToken(accountCodeFromToken, crc16, crc32, tokenType);
        if (accessToken == null) {
            log.error("Token not found or deprecated for tenant: {} / {}", tenant, tokenType.name());
            throw new TokenNotFoundException("Invalid JWT: not found or deprecated");
        }
        return true;
    }

    @Override
    public void buildForgotPasswordAccessToken(String tenant, Set<String> audience, String accountCode) throws JsonProcessingException {
        //Get the account
        Account account = tenantService.checkAccountIfExists(tenant, null, null, accountCode, null, false);
        if (account == null) {
            log.error("Account not found for tenant/username: " + tenant + "/" + accountCode);
            throw new UserNotFoundException("tenant/username: " + tenant + "/" + accountCode);
        }

        //Generate reset password token
        TokenResponseDto token = this.buildTokenAndSave(tenant, audience, IEnumToken.Types.RSTPWD, accountCode,
                Map.of(JwtConstants.JWT_SENDER_TENANT, tenant,
                        JwtConstants.JWT_SENDER_USER, accountCode,
                        JwtConstants.JWT_LOG_APP, "application"));

        //Send reset password email
        sendForgotPasswordEmail(tenant, "application", account, token);
    }

    private void sendForgotPasswordEmail(String tenant /*senderTenant*/, String application, Account account, TokenResponseDto token) throws JsonProcessingException {
        //Build Email template data
        String resetPwdUrl = "http://localhost:4000/reset-password/";
        try {
            ResponseEntity<String> result = imsAppParameterService.getValueByTenantAndName(
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
    public TokenResponseDto buildAccessToken(String tenant, Set<String> audience, String userName, Boolean isAdmin) {
        TokenResponseDto token = this.buildTokenAndSave(tenant, audience, IEnumToken.Types.ACCESS, userName,
                Map.of(JwtConstants.JWT_SENDER_TENANT, tenant,
                        JwtConstants.JWT_SENDER_USER, userName,
                        JwtConstants.JWT_LOG_APP, "application",
                        JwtConstants.JWT_IS_ADMIN, isAdmin)
        );
        return token;
    }

    @Override
    public TokenResponseDto buildRefreshToken(String tenant, Set<String> audience, String userName) {
        TokenResponseDto token = this.buildTokenAndSave(tenant, audience, IEnumToken.Types.REFRESH, userName,
                Map.of(JwtConstants.JWT_SENDER_TENANT, tenant,
                        JwtConstants.JWT_SENDER_USER, userName,
                        JwtConstants.JWT_LOG_APP, "application")
        );
        return token;
    }

    @Override
    public TokenResponseDto buildAuthorityToken(String tenant, Set<String> audience, String userName, List<String> authorities) {
        TokenResponseDto token = this.buildToken(tenant, audience, IEnumToken.Types.AUTHORITY, userName,
                Map.of(JwtConstants.JWT_SENDER_TENANT, tenant,
                        JwtConstants.JWT_SENDER_USER, userName,
                        JwtConstants.JWT_LOG_APP, "application",
                        JwtConstants.JWT_GRANTED_AUTHORITY, authorities)
        );
        return token;
    }
}
