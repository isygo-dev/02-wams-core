package eu.isygoit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.isygoit.config.AppProperties;
import eu.isygoit.constants.AppParameterConstants;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.common.ResetPwdViaTokenRequestDto;
import eu.isygoit.dto.common.UserContextDto;
import eu.isygoit.dto.data.MailMessageDto;
import eu.isygoit.dto.response.AccessKeyResponseDto;
import eu.isygoit.encrypt.helper.CRC16;
import eu.isygoit.encrypt.helper.CRC32;
import eu.isygoit.enums.*;
import eu.isygoit.exception.*;
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
import java.util.*;

/**
 * The type Password service.
 */
@Slf4j
@Service
@Transactional
public class PasswordService implements IPasswordService {

    private final AppProperties appProperties;

    private final PasswordConfigRepository passwordConfigRepository;
    private final IDomainService domainService;
    private final PasswordInfoRepository passwordInfoRepository;
    private final RandomKeyGenerator randomKeyGenerator;
    private final ICryptoService cryptoService;
    private final IJwtService jwtService;
    private final ITokenConfigService tokenConfigService;
    private final IAccessTokenService accessTokenService;
    private final IMsgService msgService;
    private final ImsAppParameterService imsAppParameterService;

    /**
     * Instantiates a new Password service.
     *
     * @param appProperties the app properties
     */
    @Autowired
    public PasswordService(AppProperties appProperties, PasswordConfigRepository passwordConfigRepository, IDomainService domainService, PasswordInfoRepository passwordInfoRepository, RandomKeyGenerator randomKeyGenerator, ICryptoService cryptoService, IJwtService jwtService, ITokenConfigService tokenConfigService, IAccessTokenService accessTokenService, IMsgService msgService, ImsAppParameterService imsAppParameterService) {
        this.appProperties = appProperties;
        this.passwordConfigRepository = passwordConfigRepository;
        this.domainService = domainService;
        this.passwordInfoRepository = passwordInfoRepository;
        this.randomKeyGenerator = randomKeyGenerator;
        this.cryptoService = cryptoService;
        this.jwtService = jwtService;
        this.tokenConfigService = tokenConfigService;
        this.accessTokenService = accessTokenService;
        this.msgService = msgService;
        this.imsAppParameterService = imsAppParameterService;
    }

    @Override
    public AccessKeyResponseDto generateRandomPassword(String domain, String domainUrl, String email, String userName, String fullName, IEnumAuth.Types authType) throws JsonProcessingException {
        if (Objects.isNull(authType)) {
            throw new UnsuportedAuthTypeException("null authType value");
        }

        if (!domainService.isEnabled(domain)) {
            throw new AccountAuthenticationException("domain disabled: " + domain);
        }
        //Verify the account
        Optional<Account> optional = domainService.checkAccountIfExists(domain, domainUrl, email, userName, fullName, true);
        if (optional.isPresent()) {
            Account account = optional.get();
            //Generate password
            AccessKeyResponseDto accessKeyResponse = this.registerNewPassword(domain, account, null, authType);
            switch (authType) {
                case PWD -> {
                    //Get gateway url
                    String gatewayUrl = "http://localhost:4001";
                    try {
                        ResponseEntity<String> result = imsAppParameterService.getValueByDomainAndName(RequestContextDto.builder().build(),
                                domain, AppParameterConstants.GATEWAY_URL, true, gatewayUrl);
                        if (result.getStatusCode().is2xxSuccessful() && result.hasBody() && StringUtils.hasText(result.getBody())) {
                            gatewayUrl = result.getBody();
                        }
                    } catch (Exception e) {
                        log.error("Remote feign call failed : ", e);
                        //throw new RemoteCallFailedException(e);
                    }

                    //Build message data object
                    MailMessageDto mailMessageDto = MailMessageDto.builder()
                            .subject(EmailSubjects.USER_CREATED_EMAIL_SUBJECT)
                            .domain(domain)
                            .toAddr(account.getEmail())
                            .templateName(IEnumMsgTemplateName.Types.USER_CREATED_TEMPLATE)
                            .variables(MailMessageDto.getVariablesAsString(Map.of(
                                    //Common vars
                                    MsgTemplateVariables.V_USER_NAME, account.getCode(),
                                    MsgTemplateVariables.V_FULLNAME, account.getFullName(),
                                    MsgTemplateVariables.V_DOMAIN_NAME, account.getDomain(),
                                    //Specific vars
                                    MsgTemplateVariables.V_GATEWAY_URL, gatewayUrl,
                                    MsgTemplateVariables.V_PASSWORD, accessKeyResponse.getKey())))
                            .build();
                    //Send the message
                    msgService.sendMessage(domain, mailMessageDto, appProperties.isSendAsyncEmail());
                    return accessKeyResponse;
                }
                case OTP -> {
                    //Build message data object
                    MailMessageDto mailMessageDto = MailMessageDto.builder()
                            .subject(EmailSubjects.OTP_CODE_ACCESS_EMAIL_SUBJECT)
                            .domain(domain)
                            .toAddr(account.getEmail())
                            .templateName(IEnumMsgTemplateName.Types.AUTH_OTP_TEMPLATE)
                            .variables(MailMessageDto.getVariablesAsString(Map.of(
                                    //Common vars
                                    MsgTemplateVariables.V_USER_NAME, account.getCode(),
                                    MsgTemplateVariables.V_FULLNAME, account.getFullName(),
                                    MsgTemplateVariables.V_DOMAIN_NAME, account.getDomain(),
                                    //Specific vars
                                    MsgTemplateVariables.V_OTP_CODE, accessKeyResponse.getKey(),
                                    MsgTemplateVariables.V_OTP_LIFETIME_IN_M, String.valueOf(accessKeyResponse.getLifeTime()))))
                            .build();
                    //Send the message
                    msgService.sendMessage(domain, mailMessageDto, appProperties.isSendAsyncEmail());
                    return accessKeyResponse;
                }
                case QRC -> {
                    return accessKeyResponse;
                }
            }
        }

        throw new UserNotFoundException("domain/username: " + domain + "/" + userName);
    }

    @Override
    public void forceChangePassword(String domain, String userName, String newPassword) {
        Optional<Account> optional = domainService.checkAccountIfExists(domain, null, null, userName, null, false);
        optional.ifPresentOrElse(account -> {
                    registerNewPassword(domain, account, newPassword, IEnumAuth.Types.PWD);
                    //TODO add email to inform and validate user that the password has been changed
                },
                () -> new UserNotFoundException("domain/username: " + domain + "/" + userName));
    }

    @Override
    public void changePassword(String domain, String userName, String oldPassword, String newPassword) {
        IEnumPasswordStatus.Types passwordMatches = matches(domain, userName, oldPassword, IEnumAuth.Types.PWD);
        if (passwordMatches == IEnumPasswordStatus.Types.VALID) {
            forceChangePassword(domain, userName, newPassword);
        } else {
            throw new PasswordNotValidException("Password not valid");
        }
    }

    @Override
    public AccessKeyResponseDto registerNewPassword(String domain, Account account, String newPassword, IEnumAuth.Types authType)
            throws UnsuportedAuthTypeException {
        LocalDateTime expiryDate = null;
        Integer length = null;
        IEnumCharSet.Types charSetType = null;
        Integer lifetime = null;
        Optional<PasswordConfig> passwordConfigOptional = passwordConfigRepository.findByDomainIgnoreCaseAndType(domain, authType);
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

        String encodedPassword = cryptoService.getPasswordEncryptor(domain).encryptPassword(newPassword);
        int[] crc = this.signPassword(encodedPassword);

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
    public boolean checkForPattern(String domain, String plainPassword) {
        Optional<PasswordConfig> passwordConfigOptional = passwordConfigRepository.findByDomainIgnoreCaseAndType(domain, IEnumAuth.Types.PWD);
        if (passwordConfigOptional.isPresent() && StringUtils.hasText(passwordConfigOptional.get().getPattern())) {
            return plainPassword.matches(passwordConfigOptional.get().getPattern());
        }

        log.warn("password config not found for domain: {}" + domain);
        return plainPassword.matches("^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[/@#$%^&+-=(){}\\[\\]])(?=\\S+$).{8,}$");
    }

    @Override
    public IEnumPasswordStatus.Types matches(String domain, String userName, String plainPassword, IEnumAuth.Types authType)
            throws UserPasswordNotFoundException, UserNotFoundException {
        Optional<Account> optional = domainService.checkAccountIfExists(domain, null, null, userName, null, false);
        if (optional.isPresent()) {
            if (IEnumAuth.Types.TOKEN == authType) {
                return IEnumPasswordStatus.Types.VALID;
            }
            List<PasswordInfo> passwordInfos = passwordInfoRepository.findByUserIdAndAuthTypeOrderByCreateDateDesc(optional.get().getId(), authType);
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
                        int[] crc = this.signPassword(passwordInfo.getPassword());
                        if (passwordInfo.getCrc16() != crc[0] || passwordInfo.getCrc32() != crc[1]) {
                            newStatus = IEnumPasswordStatus.Types.BROKEN;
                        } else if (passwordInfo.isExpired()) {
                            newStatus = IEnumPasswordStatus.Types.EXPIRED;
                        } else if (!cryptoService.getPasswordEncryptor(domain).checkPassword(plainPassword, passwordInfo.getPassword())) {
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
            }

            throw new UserPasswordNotFoundException("for user name " + userName);
        }

        throw new UserNotFoundException("domain/username: " + domain + "/" + userName);
    }

    @Override
    public int[] signPassword(String password) {
        return new int[]{CRC16.calculate(password.getBytes()), CRC32.calculate(password.getBytes())};
    }

    @Override
    public Boolean isExpired(String domain, String email, String userName, IEnumAuth.Types authType)
            throws UserPasswordNotFoundException, UserNotFoundException {
        Optional<Account> optional = domainService.checkAccountIfExists(domain, null, null, userName, null, false);
        if (optional.isPresent()) {
            if (IEnumAuth.Types.TOKEN == authType) {
                return Boolean.FALSE;
            }
            List<PasswordInfo> passwordInfos = passwordInfoRepository.findByUserIdAndAuthTypeOrderByCreateDateDesc(optional.get().getId(), authType);
            if (!CollectionUtils.isEmpty(passwordInfos)) {
                PasswordInfo passwordInfo = passwordInfos.get(0);
                return passwordInfo.getStatus() == IEnumPasswordStatus.Types.EXPIRED;
            } else {
                throw new UserPasswordNotFoundException("for user name " + userName);
            }
        }
        throw new UserNotFoundException("domain/username: " + domain + "/" + userName);
    }

    @Override
    public void resetPasswordViaToken(ResetPwdViaTokenRequestDto resetPwdViaTokenRequestDto)
            throws TokenInvalidException {
        Optional<String> optional = jwtService.extractSubject(resetPwdViaTokenRequestDto.getToken());
        if (optional.isPresent() && StringUtils.hasText(optional.get())) {
            String[] split = optional.get().split("@");
            Optional<AccessToken> optionalAccessToken = accessTokenService.findByApplicationAndAccountCodeAndTokenAndTokenType(resetPwdViaTokenRequestDto.getApplication(), split[0], resetPwdViaTokenRequestDto.getToken(), IEnumAppToken.Types.RSTPWD);
            if (split.length >= 2
                    && optionalAccessToken.isPresent()
                    && StringUtils.hasText(optionalAccessToken.get().getToken())
                    && optionalAccessToken.get().getToken().equals(resetPwdViaTokenRequestDto.getToken())) {
                UserContextDto userContext = UserContextDto.builder()
                        .domain(split[1])
                        .userName(split[0])
                        .build();
                TokenConfig tokenConfig = tokenConfigService.buildTokenConfig(userContext.getDomain(), IEnumAppToken.Types.RSTPWD);
                jwtService.validateToken(resetPwdViaTokenRequestDto.getToken(), optional.get(), tokenConfig.getSecretKey());
                this.forceChangePassword(userContext.getDomain(), userContext.getUserName()
                        , resetPwdViaTokenRequestDto.getPassword());
            } else {
                throw new TokenInvalidException("Invalid JWT:malformed");
            }
        }
    }
}
