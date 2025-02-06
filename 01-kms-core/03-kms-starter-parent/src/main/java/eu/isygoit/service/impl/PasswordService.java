package eu.isygoit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.isygoit.config.AppProperties;
import eu.isygoit.constants.AppParameterConstants;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.common.ResetPwdViaTokenRequestDto;
import eu.isygoit.dto.common.UserContextDto;
import eu.isygoit.dto.data.MailMessageDto;
import eu.isygoit.dto.response.AccessKeyResponseDto;
import eu.isygoit.encrypt.config.EncProperties;
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
import org.springframework.beans.factory.annotation.Value;
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

    private final EncProperties encProperties;
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
    public PasswordService(EncProperties encProperties, AppProperties appProperties, PasswordConfigRepository passwordConfigRepository, IDomainService domainService, PasswordInfoRepository passwordInfoRepository, RandomKeyGenerator randomKeyGenerator, ICryptoService cryptoService, IJwtService jwtService, ITokenConfigService tokenConfigService, IAccessTokenService accessTokenService, IMsgService msgService, ImsAppParameterService imsAppParameterService) {
        this.encProperties = encProperties;
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
    public AccessKeyResponseDto generateRandomPassword(
            String domain, String domainUrl, String email, String userName,
            String fullName, IEnumAuth.Types authType) throws JsonProcessingException {

        Objects.requireNonNull(domain, "null domain value");
        Objects.requireNonNull(userName, "null userName value");
        Objects.requireNonNull(authType, "null authType value");

        if (!domainService.isEnabled(domain)) {
            throw new DomainDisabledException("With name: " + domain);
        }

        Account account = domainService.checkAccountIfExists(domain, domainUrl, email, userName, fullName, true)
                .orElseThrow(() -> new UserNotFoundException("domain/username: " + domain + "/" + userName));

        AccessKeyResponseDto accessKeyResponse = registerNewPassword(domain, account, null, authType);

        switch (authType) {
            case PWD -> handlePwdAuth(domain, account, accessKeyResponse);
            case OTP -> handleOtpAuth(domain, account, accessKeyResponse);
            case QRC -> {} // No additional processing needed
            default -> throw new UnsupportedAuthTypeException("Unsupported authType: " + authType);
        }

        return accessKeyResponse;
    }

    private void handlePwdAuth(String domain, Account account, AccessKeyResponseDto accessKeyResponse) throws JsonProcessingException {
        String gatewayUrl = Optional.ofNullable(imsAppParameterService.getValueByDomainAndName(
                        RequestContextDto.builder().build(), domain, AppParameterConstants.GATEWAY_URL, true, "http://localhost:4001"))
                .filter(res -> res.getStatusCode().is2xxSuccessful() && res.hasBody() && StringUtils.hasText(res.getBody()))
                .map(ResponseEntity::getBody)
                .orElse("http://localhost:4001");

        sendEmail(domain, account, EmailSubjects.USER_CREATED_EMAIL_SUBJECT,
                IEnumMsgTemplateName.Types.USER_CREATED_TEMPLATE,
                Map.of(
                        MsgTemplateVariables.V_USER_NAME, account.getCode(),
                        MsgTemplateVariables.V_FULLNAME, account.getFullName(),
                        MsgTemplateVariables.V_DOMAIN_NAME, account.getDomain(),
                        MsgTemplateVariables.V_GATEWAY_URL, gatewayUrl,
                        MsgTemplateVariables.V_PASSWORD, accessKeyResponse.getKey()
                ));
    }

    private void handleOtpAuth(String domain, Account account, AccessKeyResponseDto accessKeyResponse) throws JsonProcessingException {
        sendEmail(domain, account, EmailSubjects.OTP_CODE_ACCESS_EMAIL_SUBJECT,
                IEnumMsgTemplateName.Types.AUTH_OTP_TEMPLATE,
                Map.of(
                        MsgTemplateVariables.V_USER_NAME, account.getCode(),
                        MsgTemplateVariables.V_FULLNAME, account.getFullName(),
                        MsgTemplateVariables.V_DOMAIN_NAME, account.getDomain(),
                        MsgTemplateVariables.V_OTP_CODE, accessKeyResponse.getKey(),
                        MsgTemplateVariables.V_OTP_LIFETIME_IN_M, String.valueOf(accessKeyResponse.getLifeTime())
                ));
    }

    private void sendEmail(String domain, Account account, String subject, IEnumMsgTemplateName.Types template, Map<String, String> variables) throws JsonProcessingException {
        MailMessageDto mailMessageDto = MailMessageDto.builder()
                .subject(subject)
                .domain(domain)
                .toAddr(account.getEmail())
                .templateName(template)
                .variables(MailMessageDto.getVariablesAsString(variables))
                .build();

        msgService.sendMessage(domain, mailMessageDto, appProperties.isSendAsyncEmail());
    }

    @Override
    public void forceChangePassword(String domain, String userName, String newPassword) {
        Optional<Account> optional = domainService.checkAccountIfExists(domain, null, null, userName, null, false);
        optional.ifPresentOrElse(account -> {
                    registerNewPassword(domain, account, newPassword, IEnumAuth.Types.PWD);
                    //TODO add email to inform and validate user that the password has been changed
                },
                () -> {throw new UserNotFoundException("domain/username: " + domain + "/" + userName);});
    }

    @Override
    public void changePassword(String domain, String userName, String oldPassword, String newPassword) {
        if (IEnumPasswordStatus.Types.VALID == matches(domain, userName, oldPassword, IEnumAuth.Types.PWD)) {
            forceChangePassword(domain, userName, newPassword);
        } else {
            throw new PasswordNotValidException("Password not valid");
        }
    }

    @Override
    public AccessKeyResponseDto registerNewPassword(String domain, Account account, String newPassword, IEnumAuth.Types authType)
            throws UnsupportedAuthTypeException {

        PasswordConfig passwordConfig = passwordConfigRepository.findByDomainIgnoreCaseAndType(domain, authType).orElse(null);

        PasswordProperties properties = getPasswordProperties(authType, passwordConfig);

        if (!StringUtils.hasText(newPassword)) {
            newPassword = randomKeyGenerator.nextGuid(properties.length(), properties.charSetType());
        }

        String encodedPassword = cryptoService.getPasswordEncryptor(domain).encryptPassword(newPassword);
        int[] crc = signPassword(encodedPassword);

        // Deactivate old passwords before saving a new one
        passwordInfoRepository.deactivateOldPasswords(account.getId(), authType);

        // Save new password
        passwordInfoRepository.save(PasswordInfo.builder()
                .userId(account.getId())
                .expiryDate(Date.from(properties.expiryDate().atZone(ZoneId.systemDefault()).toInstant()))
                .password(encodedPassword)
                .status(IEnumPasswordStatus.Types.VALID)
                .crc16(crc[0])
                .crc32(crc[1])
                .authType(authType)
                .build()
        );

        return AccessKeyResponseDto.builder()
                .key(newPassword)
                .length(properties.length())
                .lifeTime(properties.lifetime())
                .build();
    }

    private PasswordProperties getPasswordProperties(IEnumAuth.Types authType, PasswordConfig passwordConfig) {
        return switch (authType) {
            case PWD -> passwordConfig != null
                    ? new PasswordProperties(passwordConfig.getMaxLength(), passwordConfig.getCharSetType(),
                    LocalDateTime.now().plusDays(passwordConfig.getLifeTime()), passwordConfig.getLifeTime())
                    : new PasswordProperties(12, IEnumCharSet.Types.ALL, LocalDateTime.now().plusDays(90), 90);

            case OTP -> passwordConfig != null
                    ? new PasswordProperties(passwordConfig.getMaxLength(), passwordConfig.getCharSetType(),
                    LocalDateTime.now().plusMinutes(passwordConfig.getLifeTime()), passwordConfig.getLifeTime())
                    : new PasswordProperties(4, IEnumCharSet.Types.NUMERIC, LocalDateTime.now().plusMinutes(3), 3);

            default -> throw new UnsupportedAuthTypeException("Auth type is missing or not supported: " + authType);
        };
    }

    private record PasswordProperties(int length, IEnumCharSet.Types charSetType, LocalDateTime expiryDate, int lifetime) {}

    @Override
    public boolean isPasswordPatternValid(String domain, String plainPassword) {
        return passwordConfigRepository.findByDomainIgnoreCaseAndType(domain, IEnumAuth.Types.PWD)
                .map(PasswordConfig::getPattern)
                .filter(StringUtils::hasText)
                .map(plainPassword::matches)
                .orElseGet(() -> {
                    log.warn("Password config not found for domain: {}", domain);
                    return plainPassword.matches(encProperties.getPasswordPattern());
                });
    }

    @Override
    public IEnumPasswordStatus.Types matches(String domain, String userName, String plainPassword, IEnumAuth.Types authType)
            throws UserPasswordNotFoundException, UserNotFoundException {

        Account account = domainService.checkAccountIfExists(domain, null, null, userName, null, false)
                .orElseThrow(() -> new UserNotFoundException("domain/username: " + domain + "/" + userName));

        if (authType == IEnumAuth.Types.TOKEN) {
            return IEnumPasswordStatus.Types.VALID;
        }

        PasswordInfo passwordInfo = passwordInfoRepository
                .findByUserIdAndAuthTypeOrderByCreateDateDesc(account.getId(), authType)
                .stream()
                .findFirst()
                .orElseThrow(() -> new UserPasswordNotFoundException("for user name " + userName));

        IEnumPasswordStatus.Types newStatus = switch (passwordInfo.getStatus()) {
            case LOCKED, EXPIRED, BROKEN, BAD, UNAUTHORIZED -> passwordInfo.getStatus();
            case DEPRECATED, VALID -> determinePasswordStatus(domain, plainPassword, passwordInfo);
        };

        if (passwordInfo.getStatus() != newStatus) {
            passwordInfo.setStatus(newStatus);
            passwordInfoRepository.save(passwordInfo);
        }

        return newStatus;
    }

    private IEnumPasswordStatus.Types determinePasswordStatus(String domain, String plainPassword, PasswordInfo passwordInfo) {
        int[] crc = signPassword(passwordInfo.getPassword());

        if (passwordInfo.getCrc16() != crc[0] || passwordInfo.getCrc32() != crc[1]) {
            return IEnumPasswordStatus.Types.BROKEN;
        }
        if (passwordInfo.isExpired()) {
            return IEnumPasswordStatus.Types.EXPIRED;
        }
        if (!cryptoService.getPasswordEncryptor(domain).checkPassword(plainPassword, passwordInfo.getPassword())) {
            return IEnumPasswordStatus.Types.BAD;
        }

        return IEnumPasswordStatus.Types.VALID;
    }

    @Override
    public int[] signPassword(String password) {
        return new int[]{CRC16.calculate(password.getBytes()),
                CRC32.calculate(password.getBytes())};
    }

    @Override
    public boolean isExpired(String domain, String email, String userName, IEnumAuth.Types authType)
            throws UserPasswordNotFoundException, UserNotFoundException {
        Optional<Account> optional = domainService.checkAccountIfExists(domain, null, null, userName, null, false);
        if (optional.isPresent()) {
            if (IEnumAuth.Types.TOKEN == authType) {
                return Boolean.FALSE;
            }

            return passwordInfoRepository.findByUserIdAndAuthTypeOrderByCreateDateDesc(optional.get().getId(), authType)
                    .stream()
                    .findFirst()
                    .map(passwordInfo -> passwordInfo.getStatus() == IEnumPasswordStatus.Types.EXPIRED)
                    .orElseThrow(() -> new UserPasswordNotFoundException("for user name " + userName));
        }
        throw new UserNotFoundException("domain/username: " + domain + "/" + userName);
    }

    @Override
    public void resetPasswordViaToken(ResetPwdViaTokenRequestDto resetPwdViaTokenRequestDto)
            throws TokenInvalidException {

        var token = resetPwdViaTokenRequestDto.getToken();
        Optional<String> optionalSubject = jwtService.extractSubject(token);

        optionalSubject.filter(StringUtils::hasText)
                .ifPresentOrElse(subject -> {
                    String[] split = subject.split("@");

                    if (split.length < 2) {
                        throw new TokenInvalidException("Invalid JWT: malformed");
                    }

                    // Check access token
                    accessTokenService.findByApplicationAndAccountCodeAndTokenAndTokenType(
                                    resetPwdViaTokenRequestDto.getApplication(),
                                    split[0],
                                    token,
                                    IEnumAppToken.Types.RSTPWD
                            ).filter(accessToken -> token.equals(accessToken.getToken()))
                            .ifPresentOrElse(accessToken -> {
                                // Validate token and force password change
                                Optional<TokenConfig> tokenConfig = tokenConfigService.buildTokenConfig(split[1], IEnumAppToken.Types.RSTPWD);
                                jwtService.validateToken(token, subject, tokenConfig.get().getSecretKey());
                                forceChangePassword(split[1], split[0], resetPwdViaTokenRequestDto.getPassword());
                            }, () -> {
                                throw new TokenInvalidException("Invalid JWT: token mismatch");
                            });
                }, () -> {
                    throw new TokenInvalidException("Invalid JWT: subject is missing or empty");
                });
    }
}
