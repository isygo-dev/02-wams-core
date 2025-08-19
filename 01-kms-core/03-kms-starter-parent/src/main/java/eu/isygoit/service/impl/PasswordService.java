package eu.isygoit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.isygoit.config.AppProperties;
import eu.isygoit.constants.AppParameterConstants;
import eu.isygoit.dto.common.ContextRequestDto;
import eu.isygoit.dto.common.ResetPwdViaTokenRequestDto;
import eu.isygoit.dto.common.UserContextRequestDto;
import eu.isygoit.dto.data.MailMessageDto;
import eu.isygoit.dto.response.AccessKeyResponseDto;
import eu.isygoit.enums.*;
import eu.isygoit.exception.*;
import eu.isygoit.helper.CRC16Helper;
import eu.isygoit.helper.CRC32Helper;
import eu.isygoit.jwt.IJwtService;
import eu.isygoit.model.*;
import eu.isygoit.remote.ims.ImsAppParameterService;
import eu.isygoit.repository.PasswordConfigRepository;
import eu.isygoit.repository.PasswordInfoRepository;
import eu.isygoit.service.*;
import eu.isygoit.types.EmailSubjects;
import eu.isygoit.types.MsgTemplateVariables;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The type Password service.
 */
@Slf4j
@Service
@Transactional
public class PasswordService implements IPasswordService {

    private final AppProperties appProperties;

    @Autowired
    private PasswordConfigRepository passwordConfigRepository;
    @Autowired
    private IDomainService tenantService;
    @Autowired
    private PasswordInfoRepository passwordInfoRepository;
    @Autowired
    private RandomKeyGenerator randomKeyGenerator;
    @Autowired
    private ICryptoService cryptoService;
    @Autowired
    private IJwtService jwtService;
    @Autowired
    private ITokenConfigService tokenConfigService;
    @Autowired
    private IAccessTokenService accessTokenService;
    @Autowired
    private IMsgService msgService;
    @Autowired
    private ImsAppParameterService imsAppParameterService;

    /**
     * Instantiates a new Password service.
     *
     * @param appProperties the app properties
     */
    public PasswordService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public AccessKeyResponseDto generateRandomPassword(String tenant, String tenantUrl, String email, String userName, String fullName, IEnumAuth.Types authType) throws JsonProcessingException {
        //Verify the account
        Account account = tenantService.checkAccountIfExists(tenant, tenantUrl, email, userName, fullName, true);
        if (account == null) {
            throw new UserNotFoundException("tenant/username: " + tenant + "/" + userName);
        }

        if (!tenantService.isEnabled(tenant)) {
            throw new AccountAuthenticationException("tenant disabled: " + tenant);
        }

        switch (authType) {
            case PWD -> {
                //Get gateway url
                String gatewayUrl = "http://localhost:4001";
                try {
                    ResponseEntity<String> result = imsAppParameterService.getValueByTenantAndName(ContextRequestDto.builder().build(),
                            tenant, AppParameterConstants.GATEWAY_URL, true, gatewayUrl);
                    if (result.getStatusCode().is2xxSuccessful() && result.hasBody() && StringUtils.hasText(result.getBody())) {
                        gatewayUrl = result.getBody();
                    }
                } catch (Exception e) {
                    log.error("Remote feign call failed : ", e);
                    //throw new RemoteCallFailedException(e);
                }

                //Generate password
                AccessKeyResponseDto accessKeyResponse = this.registerNewPassword(tenant, account, null, authType);
                //Build message data object
                MailMessageDto mailMessageDto = MailMessageDto.builder()
                        .subject(EmailSubjects.USER_CREATED_EMAIL_SUBJECT)
                        .tenant(tenant)
                        .toAddr(account.getEmail())
                        .templateName(IEnumEmailTemplate.Types.USER_CREATED_TEMPLATE)
                        .variables(MailMessageDto.getVariablesAsString(Map.of(
                                //Common vars
                                MsgTemplateVariables.V_USER_NAME, account.getCode(),
                                MsgTemplateVariables.V_FULLNAME, account.getFullName(),
                                MsgTemplateVariables.V_TENANT_NAME, account.getTenant(),
                                //Specific vars
                                MsgTemplateVariables.V_GATEWAY_URL, gatewayUrl,
                                MsgTemplateVariables.V_PASSWORD, accessKeyResponse.getKey())))
                        .build();
                //Send the message
                msgService.sendMessage(tenant, mailMessageDto, appProperties.isSendAsyncEmail());
                return accessKeyResponse;
            }
            case OTP -> {
                //Generate OTP code
                AccessKeyResponseDto accessKeyResponse = this.registerNewPassword(tenant, account, null, authType);
                //Build message data object
                MailMessageDto mailMessageDto = MailMessageDto.builder()
                        .subject(EmailSubjects.OTP_CODE_ACCESS_EMAIL_SUBJECT)
                        .tenant(tenant)
                        .toAddr(account.getEmail())
                        .templateName(IEnumEmailTemplate.Types.AUTH_OTP_TEMPLATE)
                        .variables(MailMessageDto.getVariablesAsString(Map.of(
                                //Common vars
                                MsgTemplateVariables.V_USER_NAME, account.getCode(),
                                MsgTemplateVariables.V_FULLNAME, account.getFullName(),
                                MsgTemplateVariables.V_TENANT_NAME, account.getTenant(),
                                //Specific vars
                                MsgTemplateVariables.V_OTP_CODE, accessKeyResponse.getKey(),
                                MsgTemplateVariables.V_OTP_LIFETIME_IN_M, String.valueOf(accessKeyResponse.getLifeTime()))))
                        .build();
                //Send the message
                msgService.sendMessage(tenant, mailMessageDto, appProperties.isSendAsyncEmail());
                return accessKeyResponse;
            }
            case QRC -> {
                //Genrate QRC code
                return this.registerNewPassword(tenant, account, null, authType);
            }
            default -> {
                log.error("Auth type is missing or not supported: " + authType);
                return null;
            }
        }
    }

    @Override
    public void forceChangePassword(String tenant, String userName, String newPassword) {
        Account account = tenantService.checkAccountIfExists(tenant, null, null, userName, null, false);
        if (account == null) {
            throw new UserNotFoundException("tenant/username: " + tenant + "/" + userName);
        }
        registerNewPassword(tenant, account, newPassword, IEnumAuth.Types.PWD);
        //TODO add email to inform and validate user that the password has been changed
    }

    @Override
    public void changePassword(String tenant, String userName, String oldPassword, String newPassword) {
        IEnumPasswordStatus.Types passwordMatches = matches(tenant, userName, oldPassword, IEnumAuth.Types.PWD);
        if (passwordMatches == IEnumPasswordStatus.Types.VALID) {
            forceChangePassword(tenant, userName, newPassword);
        } else {
            throw new PasswordNotValidException("Password not valid");
        }
    }

    @Override
    public AccessKeyResponseDto registerNewPassword(String tenant, Account account, String newPassword, IEnumAuth.Types authType)
            throws UnsuportedAuthTypeException {
        LocalDateTime expiryDate = null;
        Integer length = null;
        IEnumCharSet.Types charSetType = null;
        Integer lifetime = null;
        Optional<PasswordConfig> passwordConfigOptional = passwordConfigRepository.findByTenantIgnoreCaseAndType(tenant, authType);
        if (passwordConfigOptional.isPresent()) {
            PasswordConfig passwordConfig = passwordConfigOptional.get();
            switch (authType) {
                case PWD -> {
                    length = passwordConfig.getMaxLength();
                    charSetType = passwordConfig.getCharSetType();
                    expiryDate = LocalDateTime.now().plusDays(passwordConfig.getLifeTime());
                    lifetime = passwordConfig.getLifeTime();
                    break;
                }
                case OTP -> {
                    length = passwordConfig.getMaxLength();
                    charSetType = passwordConfig.getCharSetType();
                    expiryDate = LocalDateTime.now().plusMinutes(passwordConfig.getLifeTime());
                    lifetime = passwordConfig.getLifeTime();
                    break;
                }
                default -> {
                    log.error("Auth type is missing or not supported: " + authType);
                    throw new UnsuportedAuthTypeException("Auth type is missing or not supported: " + authType);
                }
            }
        } else {
            switch (authType) {
                case PWD -> {
                    length = 12;
                    charSetType = IEnumCharSet.Types.ALL;
                    lifetime = 90;
                    expiryDate = LocalDateTime.now().plusDays(90);
                }
                case OTP -> {
                    length = 4;
                    charSetType = IEnumCharSet.Types.NUMERIC;
                    lifetime = 3;
                    expiryDate = LocalDateTime.now().plusMinutes(3);
                }
                default -> {
                    log.error("Auth type is missing or not supported: " + authType);
                    throw new UnsuportedAuthTypeException("Auth type is missing or not supported: " + authType);
                }
            }
        }

        if (!StringUtils.hasText(newPassword)) {
            newPassword = randomKeyGenerator.nextGuid(length, charSetType);
        }

        String encodedPassword = cryptoService.getPasswordEncryptor(tenant).encryptPassword(newPassword);
        long[] crc = this.signPassword(encodedPassword);

        //Deactivate all old passwords before saving a new one
        passwordInfoRepository.deactivateOldPasswords(account.getId(), authType);

        //Save new password
        passwordInfoRepository.save(PasswordInfo.builder()
                .userId(account.getId())
                .expiryDate(Date.from(expiryDate.atZone(ZoneId.systemDefault()).toInstant()))
                .password(encodedPassword)
                .status(IEnumPasswordStatus.Types.VALID)
                .crc16(crc[0])
                .crc32(crc[1])
                .authType(authType)
                .build()
        );

        return AccessKeyResponseDto.builder()
                .key(newPassword)
                .length(length)
                .lifeTime(lifetime)
                .build();
    }

    @Override
    public boolean checkForPattern(String tenant, String plainPassword) {
        Optional<PasswordConfig> passwordConfigOptional = passwordConfigRepository.findByTenantIgnoreCaseAndType(tenant, IEnumAuth.Types.PWD);
        if (passwordConfigOptional.isPresent() && StringUtils.hasText(passwordConfigOptional.get().getPattern())) {
            return plainPassword.matches(passwordConfigOptional.get().getPattern());
        }

        log.warn("password config not found for tenant: {}" + tenant);
        return plainPassword.matches("^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[/@#$%^&+-=(){}\\[\\]])(?=\\S+$).{8,}$");
    }

    @Override
    public IEnumPasswordStatus.Types matches(String tenant, String userName, String plainPassword, IEnumAuth.Types authType)
            throws UserPasswordNotFoundException, UserNotFoundException {
        Account account = tenantService.checkAccountIfExists(tenant, null, null, userName, null, false);
        if (account != null) {
            if (IEnumAuth.Types.TOKEN == authType) {
                return IEnumPasswordStatus.Types.VALID;
            }
            List<PasswordInfo> passwordInfos = passwordInfoRepository.findByUserIdAndAuthTypeOrderByCreateDateDesc(account.getId(), authType);
            if (!CollectionUtils.isEmpty(passwordInfos)) {
                PasswordInfo passwordInfo = passwordInfos.get(0);
                IEnumPasswordStatus.Types newStatus = passwordInfo.getStatus();
                switch (passwordInfo.getStatus()) {
                    case LOCKED:
                    case EXPIRED:
                    case BROKEN: {
                        break;
                    }
                    case DEPRECATED:
                    case VALID: {
                        long[] crc = this.signPassword(passwordInfo.getPassword());
                        if (passwordInfo.getCrc16() != crc[0] || passwordInfo.getCrc32() != crc[1]) {
                            newStatus = IEnumPasswordStatus.Types.BROKEN;
                        } else if (passwordInfo.isExpired()) {
                            newStatus = IEnumPasswordStatus.Types.EXPIRED;
                        } else if (!cryptoService.getPasswordEncryptor(tenant).checkPassword(plainPassword, passwordInfo.getPassword())) {
                            newStatus = IEnumPasswordStatus.Types.BAD;
                        }
                        break;
                    }
                }
                //Update password status after check
                if (passwordInfo.getStatus() != newStatus) {
                    passwordInfo.setStatus(newStatus);
                    passwordInfo = passwordInfoRepository.save(passwordInfo);
                }
                return passwordInfo.getStatus();
            } else {
                throw new UserPasswordNotFoundException("for user name " + userName);
            }
        } else {
            throw new UserNotFoundException("tenant/username: " + tenant + "/" + userName);
        }
    }

    @Override
    public long[] signPassword(String password) {
        return new long[]{CRC16Helper.calculate(password.getBytes()), CRC32Helper.calculate(password.getBytes())};
    }

    @Override
    public Boolean isExpired(String tenant, String email, String userName, IEnumAuth.Types authType)
            throws UserPasswordNotFoundException, UserNotFoundException {
        Account account = tenantService.checkAccountIfExists(tenant, null, null, userName, null, false);
        if (account != null) {
            if (IEnumAuth.Types.TOKEN == authType) {
                return Boolean.FALSE;
            }
            List<PasswordInfo> passwordInfos = passwordInfoRepository.findByUserIdAndAuthTypeOrderByCreateDateDesc(account.getId(), authType);
            if (!CollectionUtils.isEmpty(passwordInfos)) {
                PasswordInfo passwordInfo = passwordInfos.get(0);
                return passwordInfo.getStatus() == IEnumPasswordStatus.Types.EXPIRED;
            } else {
                throw new UserPasswordNotFoundException("for user name " + userName);
            }
        }
        throw new UserNotFoundException("tenant/username: " + tenant + "/" + userName);
    }

    @Override
    public void resetPasswordViaToken(ResetPwdViaTokenRequestDto resetPwdViaTokenRequestDto)
            throws TokenInvalidException {
        Optional<String> optional = jwtService.extractSubject(resetPwdViaTokenRequestDto.getToken());
        if (optional.isPresent()) {
            String userContextString = optional.get();
            if (StringUtils.hasText(userContextString)) {
                String[] split = userContextString.split("@");
                AccessToken accessToken = accessTokenService.findByApplicationAndAccountCodeAndTokenAndTokenType(resetPwdViaTokenRequestDto.getApplication(), split[0], resetPwdViaTokenRequestDto.getToken(), IEnumToken.Types.RSTPWD);
                if (split.length >= 2 && accessToken != null && StringUtils.hasText(accessToken.getToken()) && accessToken.getToken().equals(resetPwdViaTokenRequestDto.getToken())) {
                    UserContextRequestDto userContext = UserContextRequestDto.builder()
                            .tenant(split[1])
                            .userName(split[0])
                            .build();
                    TokenConfig tokenConfig = tokenConfigService.buildTokenConfig(userContext.getTenant(), IEnumToken.Types.RSTPWD);
                    jwtService.validateToken(resetPwdViaTokenRequestDto.getToken(), userContextString, tokenConfig.getSecretKey());
                    this.forceChangePassword(userContext.getTenant(), userContext.getUserName()
                            , resetPwdViaTokenRequestDto.getPassword());
                } else {
                    throw new TokenInvalidException("Invalid JWT:malformed");
                }
            }
        }
    }
}
