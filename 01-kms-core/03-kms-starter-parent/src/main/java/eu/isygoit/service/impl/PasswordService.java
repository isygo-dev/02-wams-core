package eu.isygoit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.isygoit.config.AppProperties;
import eu.isygoit.dto.common.ResetPwdViaTokenRequestDto;
import eu.isygoit.dto.response.AccessKeyResponseDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumCharSet;
import eu.isygoit.enums.IEnumPasswordStatus;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.exception.*;
import eu.isygoit.helper.CRC16Helper;
import eu.isygoit.helper.CRC32Helper;
import eu.isygoit.jwt.IJwtService;
import eu.isygoit.model.AccessToken;
import eu.isygoit.model.Account;
import eu.isygoit.model.PasswordConfig;
import eu.isygoit.model.PasswordInfo;
import eu.isygoit.repository.PasswordConfigRepository;
import eu.isygoit.repository.PasswordInfoRepository;
import eu.isygoit.service.IAccessTokenService;
import eu.isygoit.service.ICryptoService;
import eu.isygoit.service.IPasswordService;
import eu.isygoit.service.ITenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private ITenantService tenantService;
    @Autowired
    private PasswordInfoRepository passwordInfoRepository;
    @Autowired
    private RandomKeyGenerator randomKeyGenerator;
    @Autowired
    private ICryptoService cryptoService;
    @Autowired
    private IJwtService jwtService;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private EmailSenderService emailSenderService;
    @Autowired
    private IAccessTokenService accessTokenService;

    /**
     * Instantiates a new Password service.
     *
     * @param appProperties the app properties
     */
    public PasswordService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    //Called when creating a new account, this method will create a new random password
    // and send credentials to the account email
    @Override
    public AccessKeyResponseDto generateAccountPasswordAndSendEmail(String senderTenant,
                                                                    String tenant,
                                                                    String tenantUrl,
                                                                    String email,
                                                                    String userName,
                                                                    String fullName,
                                                                    IEnumAuth.Types authType) throws JsonProcessingException {
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
                //Generate password
                AccessKeyResponseDto accessKeyResponse = this.registerNewPassword(tenant, account, null, authType);
                emailSenderService.sendAccountCreatedEmail(senderTenant, account, accessKeyResponse.getKey());
                return accessKeyResponse;
            }
            case OTP -> {
                //Generate OTP code
                AccessKeyResponseDto accessKeyResponse = this.registerNewPassword(tenant, account, null, authType);
                emailSenderService.sendOTPEmail(senderTenant, account, accessKeyResponse.getKey(), accessKeyResponse.getLifeTime());
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

    //Used by the administrator to force generating a new password for an account and send it on the account email
    @Override
    public void forceChangePassword(String senderTenant, String userName, String newPassword) throws JsonProcessingException {
        Account account = tenantService.checkAccountIfExists(senderTenant, null, null, userName, null, false);
        if (account == null) {
            throw new UserNotFoundException("tenant/username: " + senderTenant + "/" + userName);
        }

        AccessKeyResponseDto accessKeyResponse = registerNewPassword(senderTenant, account, newPassword, IEnumAuth.Types.PWD);
        emailSenderService.sendPasswordRenewEmail(senderTenant, account, accessKeyResponse.getKey());
    }

    //Once the user conncted , he can change his password on profile/settings/security page
    @Override
    public void volontaryChangePassword(String senderTenant, String userName, String oldPassword, String newPassword) throws JsonProcessingException {
        IEnumPasswordStatus.Types passwordMatches = matches(senderTenant, userName, oldPassword, IEnumAuth.Types.PWD);
        if (passwordMatches == IEnumPasswordStatus.Types.VALID) {
            forceChangePassword(senderTenant, userName, newPassword);
        } else {
            throw new PasswordNotValidException("Password not valid");
        }
    }

    @Override
    public AccessKeyResponseDto registerNewPassword(String senderTenant, Account account, String newPassword, IEnumAuth.Types authType)
            throws UnsuportedAuthTypeException {
        LocalDateTime expiryDate = null;
        Integer length = null;
        IEnumCharSet.Types charSetType = null;
        Integer lifetime = null;
        Optional<PasswordConfig> passwordConfigOptional = passwordConfigRepository.findByTenantIgnoreCaseAndType(senderTenant, authType);
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

        String encodedPassword = cryptoService.getPasswordEncryptor(senderTenant).encryptPassword(newPassword);
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
    public boolean checkForPattern(String senderTenant, String plainPassword) {
        Optional<PasswordConfig> passwordConfigOptional = passwordConfigRepository.findByTenantIgnoreCaseAndType(senderTenant, IEnumAuth.Types.PWD);
        if (passwordConfigOptional.isPresent() && StringUtils.hasText(passwordConfigOptional.get().getPattern())) {
            return plainPassword.matches(passwordConfigOptional.get().getPattern());
        }

        log.warn("password config not found for tenant: {}" + senderTenant);
        return plainPassword.matches("^(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[/@#$%^&+-=(){}\\[\\]])(?=\\S+$).{8,}$");
    }

    @Override
    public IEnumPasswordStatus.Types matches(String senderTenant, String userName, String plainPassword, IEnumAuth.Types authType)
            throws UserPasswordNotFoundException, UserNotFoundException {
        Account account = tenantService.checkAccountIfExists(senderTenant, null, null, userName, null, false);
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
                        } else if (!cryptoService.getPasswordEncryptor(senderTenant).checkPassword(plainPassword, passwordInfo.getPassword())) {
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
            throw new UserNotFoundException("tenant/username: " + senderTenant + "/" + userName);
        }
    }

    @Override
    public long[] signPassword(String password) {
        return new long[]{CRC16Helper.calculate(password.getBytes()), CRC32Helper.calculate(password.getBytes())};
    }

    @Override
    public Boolean isExpired(String senderTenant, String email, String userName, IEnumAuth.Types authType)
            throws UserPasswordNotFoundException, UserNotFoundException {
        Account account = tenantService.checkAccountIfExists(senderTenant, null, null, userName, null, false);
        if (account != null) {
            if (IEnumAuth.Types.TOKEN == authType) {
                return Boolean.FALSE;
            }
            List<PasswordInfo> passwordInfos = passwordInfoRepository.findByUserIdAndAuthTypeOrderByCreateDateDesc(account.getId(), authType);
            if (!CollectionUtils.isEmpty(passwordInfos)) {
                PasswordInfo passwordInfo = passwordInfos.get(0);
                return passwordInfo.isExpired() ? true : passwordInfo.getStatus() == IEnumPasswordStatus.Types.EXPIRED;
            } else {
                throw new UserPasswordNotFoundException("for user name " + userName);
            }
        }
        throw new UserNotFoundException("tenant/username: " + senderTenant + "/" + userName);
    }

    //User can use the forgot password feature to receive a password reinitialization email
    @Override
    public void resetPasswordViaToken(ResetPwdViaTokenRequestDto resetPwdViaTokenRequestDto)
            throws TokenInvalidException, JsonProcessingException {
        Optional<String> optional = jwtService.extractSubject(resetPwdViaTokenRequestDto.getToken());
        if (optional.isPresent()) {
            String tokenSubject = optional.get();
            if (StringUtils.hasText(tokenSubject)) {
                String[] split = tokenSubject.split("@");
                if (split.length >= 2 && !StringUtils.hasText(split[0]) || !StringUtils.hasText(split[1])) {
                    throw new TokenInvalidException("Invalid JWT: subject format invalid");
                }

                String accountCode = split[0];
                String tenant = split[1];

                Long crc16 = CRC16Helper.calculate(resetPwdViaTokenRequestDto.getToken().getBytes());
                Long crc32 = CRC32Helper.calculate(resetPwdViaTokenRequestDto.getToken().getBytes());

                AccessToken accessToken = accessTokenService.findAccessToken(resetPwdViaTokenRequestDto.getApplication(),
                        accountCode, crc16, crc32,
                        IEnumToken.Types.RSTPWD
                );

                if (accessToken != null && !accessToken.isExpired()) {
                    tokenService.isTokenValid(tenant,
                            Set.of(resetPwdViaTokenRequestDto.getApplication()),
                            IEnumToken.Types.RSTPWD,
                            resetPwdViaTokenRequestDto.getToken(),
                            tokenSubject
                    );
                    this.forceChangePassword(tenant, accountCode, resetPwdViaTokenRequestDto.getPassword());
                } else {
                    throw new TokenInvalidException("Invalid JWT:malformed");
                }
            }
        }
    }
}
