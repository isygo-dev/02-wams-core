package eu.isygoit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.isygoit.config.AppProperties;
import eu.isygoit.constants.AppParameterConstants;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.common.ResetPwdViaTokenRequestDto;
import eu.isygoit.dto.common.UserContextDto;
import eu.isygoit.dto.data.MailMessageDto;
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
    private IDomainService domainService;
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
    public String generateRandomPassword(String domain, String domainUrl, String email, String userName, String fullName, IEnumAuth.Types authType) throws JsonProcessingException {
        //Verify the account
        Account account = domainService.checkAccountIfExists(domain, domainUrl, email, userName, fullName, true);
        if (account == null) {
            throw new UserNotFoundException("domain/username: " + domain + "/" + userName);
        }
        if (!domainService.isEnabled(domain)) {
            throw new AccountAuthenticationException("domain disabled: " + domain);
        }
        switch (authType) {
            case PWD -> {
                //Get gatway url
                String gatewayUrl = "http://localhost:4001";
                try {
                    ResponseEntity<String> result = imsAppParameterService.getValueByDomainAndName(RequestContextDto.builder().build(),
                            domain, AppParameterConstants.GATEWAY_URL, true, "http://localhost:4001");
                    if (result.getStatusCode().is2xxSuccessful() && result.hasBody() && StringUtils.hasText(result.getBody())) {
                        gatewayUrl = result.getBody();
                    }
                } catch (Exception e) {
                    log.error("Remote feign call failed : ", e);
                    //throw new RemoteCallFailedException(e);
                }

                //Generate password
                String newPassword = this.registerNewPassword(domain, account, null, authType);
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
                                MsgTemplateVariables.V_PASSWORD, newPassword)))
                        .build();
                //Send the message
                msgService.sendMessage(domain, mailMessageDto, appProperties.isSendAsyncEmail());
                return newPassword;
            }
            case OTP -> {
                //Generate OTP code
                String newOtpCode = this.registerNewPassword(domain, account, null, authType);
                Optional<PasswordConfig> passwordConfigOptional = passwordConfigRepository.findByDomainIgnoreCaseAndType(domain, authType);
                int otpLifeTime = 3;
                if (passwordConfigOptional.isPresent()) {
                    otpLifeTime = passwordConfigOptional.get().getLifeTime();
                }
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
                                MsgTemplateVariables.V_OTP_CODE, newOtpCode,
                                MsgTemplateVariables.V_OTP_LIFETIME_IN_M, String.valueOf(otpLifeTime))))
                        .build();
                //Send the message
                msgService.sendMessage(domain, mailMessageDto, appProperties.isSendAsyncEmail());
                return newOtpCode;
            }
            case QRC -> {
                //Genrate QRC code
                return this.registerNewPassword(domain, account, null, authType);
            }
            default -> {
                log.error("Auth type is missing or not supported: " + authType);
                return null;
            }
        }
    }

    @Override
    public void forceChangePassword(String domain, String userName, String newPassword) {
        Account account = domainService.checkAccountIfExists(domain, null, null, userName, null, false);
        if (account == null) {
            throw new UserNotFoundException("domain/username: " + domain + "/" + userName);
        }
        registerNewPassword(domain, account, newPassword, IEnumAuth.Types.PWD);
        //TODO add email to inform and validate user that the password has been changed
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
    public String registerNewPassword(String domain, Account account, String newPassword, IEnumAuth.Types authType)
            throws UnsuportedAuthTypeException {
        LocalDateTime expiryDate = null;
        Integer length = null;
        IEnumCharSet.Types charSetType = null;
        Optional<PasswordConfig> passwordConfigOptional = passwordConfigRepository.findByDomainIgnoreCaseAndType(domain, authType);
        switch (authType) {
            case PWD -> {
                length = passwordConfigOptional.isPresent() ? passwordConfigOptional.get().getMaxLenth() : 12;
                charSetType = passwordConfigOptional.isPresent() ? passwordConfigOptional.get().getCharSetType() : IEnumCharSet.Types.ALL;
                expiryDate = passwordConfigOptional.isPresent() ? LocalDateTime.now().plusDays(passwordConfigOptional.get().getLifeTime()) : LocalDateTime.now().plusDays(90);
                break;
            }
            case OTP -> {
                length = passwordConfigOptional.isPresent() ? passwordConfigOptional.get().getMaxLenth() : 4;
                charSetType = passwordConfigOptional.isPresent() ? passwordConfigOptional.get().getCharSetType() : IEnumCharSet.Types.NUMERIC;
                expiryDate = passwordConfigOptional.isPresent() ? LocalDateTime.now().plusMinutes(passwordConfigOptional.get().getLifeTime()) : LocalDateTime.now().plusMinutes(3);
                break;
            }
            default -> {
                log.error("Auth type is missing or not supported: " + authType);
                throw new UnsuportedAuthTypeException("Auth type is missing or not supported: " + authType);
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
        return newPassword;
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
        Account account = domainService.checkAccountIfExists(domain, null, null, userName, null, false);
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
            } else {
                throw new UserPasswordNotFoundException("for user name " + userName);
            }
        } else {
            throw new UserNotFoundException("domain/username: " + domain + "/" + userName);
        }
    }

    @Override
    public int[] signPassword(String password) {
        return new int[]{CRC16.calculate(password.getBytes()), CRC32.calculate(password.getBytes())};
    }

    @Override
    public Boolean isExpired(String domain, String email, String userName, IEnumAuth.Types authType)
            throws UserPasswordNotFoundException, UserNotFoundException {
        Account account = domainService.checkAccountIfExists(domain, null, null, userName, null, false);
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
        throw new UserNotFoundException("domain/username: " + domain + "/" + userName);
    }

    @Override
    public void resetPasswordViaToken(ResetPwdViaTokenRequestDto resetPwdViaTokenRequestDto)
            throws TokenInvalidException {
        String userContextString = jwtService.extractSubject(resetPwdViaTokenRequestDto.getToken());
        if (StringUtils.hasText(userContextString)) {
            String[] split = userContextString.split("@");
            AccessToken accessToken = accessTokenService.findByApplicationAndAccountCodeAndTokenAndTokenType(resetPwdViaTokenRequestDto.getApplication(), split[0], resetPwdViaTokenRequestDto.getToken(), IEnumAppToken.Types.RSTPWD);
            if (split.length >= 2 && accessToken != null && StringUtils.hasText(accessToken.getToken()) && accessToken.getToken().equals(resetPwdViaTokenRequestDto.getToken())) {
                UserContextDto userContext = UserContextDto.builder()
                        .domain(split[1])
                        .userName(split[0])
                        .build();
                TokenConfig tokenConfig = tokenConfigService.buildTokenConfig(userContext.getDomain(), IEnumAppToken.Types.RSTPWD);
                jwtService.validateToken(resetPwdViaTokenRequestDto.getToken(), userContextString, tokenConfig.getSecretKey());
                this.forceChangePassword(userContext.getDomain(), userContext.getUserName()
                        , resetPwdViaTokenRequestDto.getPassword());
            } else {
                throw new TokenInvalidException("Invalid JWT:malformed");
            }
        }
    }
}
