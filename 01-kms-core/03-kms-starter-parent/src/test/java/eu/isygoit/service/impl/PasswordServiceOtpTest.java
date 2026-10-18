package eu.isygoit.service.impl;

import eu.isygoit.config.AppProperties;
import eu.isygoit.dto.response.AccessKeyResponseDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumCharSet;
import eu.isygoit.enums.IEnumPasswordStatus;
import eu.isygoit.model.Account;
import eu.isygoit.model.PasswordConfig;
import eu.isygoit.model.PasswordInfo;
import eu.isygoit.repository.PasswordConfigRepository;
import eu.isygoit.repository.PasswordInfoRepository;
import eu.isygoit.service.ICryptoService;
import eu.isygoit.service.ITenantService;
import org.jasypt.util.password.PasswordEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * B3: one-time codes are at least 6 digits, hashed, single-use, capped at 5 wrong attempts and not re-issued
 * inside the resend cooldown.
 */
@ExtendWith(MockitoExtension.class)
class PasswordServiceOtpTest {

    private static final String TENANT = "acme";
    private static final String USER = "alice";
    private static final long ACCOUNT_ID = 7L;
    private static final long INFO_ID = 99L;

    @Mock
    private PasswordConfigRepository passwordConfigRepository;
    @Mock
    private PasswordInfoRepository passwordInfoRepository;
    @Mock
    private ITenantService tenantService;
    @Mock
    private RandomKeyGenerator randomKeyGenerator;
    @Mock
    private ICryptoService cryptoService;
    @Mock
    private EmailSenderService emailSenderService;
    @Mock
    private PasswordEncryptor encryptor;

    private PasswordService service;
    private Account account;

    private static long anyLong() {
        return org.mockito.ArgumentMatchers.anyLong();
    }

    @BeforeEach
    void setUp() {
        service = new PasswordService(mock(AppProperties.class));
        ReflectionTestUtils.setField(service, "passwordConfigRepository", passwordConfigRepository);
        ReflectionTestUtils.setField(service, "passwordInfoRepository", passwordInfoRepository);
        ReflectionTestUtils.setField(service, "tenantService", tenantService);
        ReflectionTestUtils.setField(service, "randomKeyGenerator", randomKeyGenerator);
        ReflectionTestUtils.setField(service, "cryptoService", cryptoService);
        ReflectionTestUtils.setField(service, "emailSenderService", emailSenderService);

        account = mock(Account.class);
        lenient().when(account.getId()).thenReturn(ACCOUNT_ID);
        lenient().when(tenantService.checkAccountIfExists(eq(TENANT), any(), any(), eq(USER), any(), eq(false)))
                .thenReturn(account);
        lenient().when(cryptoService.getPasswordEncryptor(anyString())).thenReturn(encryptor);
    }

    private PasswordInfo otpInfo(IEnumPasswordStatus.Types status, boolean expired, LocalDateTime createdAt) {
        String hash = "hashed-otp";
        long[] crc = service.signPassword(hash);
        Date expiry = Date.from(LocalDateTime.now().plusMinutes(expired ? -5 : 5)
                .atZone(ZoneId.systemDefault()).toInstant());
        return PasswordInfo.builder()
                .id(INFO_ID)
                .userId(ACCOUNT_ID)
                .authType(IEnumAuth.Types.OTP)
                .password(hash)
                .status(status)
                .expiryDate(expiry)
                .crc16(crc[0])
                .crc32(crc[1])
                .createDate(createdAt)
                .build();
    }

    private void latestOtp(PasswordInfo info) {
        when(passwordInfoRepository.findByUserIdAndAuthTypeOrderByCreateDateDesc(ACCOUNT_ID, IEnumAuth.Types.OTP))
                .thenReturn(List.of(info));
    }

    // ---- verification, attempt cap

    private IEnumPasswordStatus.Types matchOtp(String code) {
        return service.matches(TENANT, USER, code, IEnumAuth.Types.OTP);
    }

    @Test
    void correctCodeMatchesAndIsNotCountedAsFailure() {
        latestOtp(otpInfo(IEnumPasswordStatus.Types.VALID, false, LocalDateTime.now()));
        when(encryptor.checkPassword("123456", "hashed-otp")).thenReturn(true);

        assertEquals(IEnumPasswordStatus.Types.VALID, matchOtp("123456"));

        verify(passwordInfoRepository, never()).incrementFailedAttempts(anyLong());
        verify(passwordInfoRepository, never()).consumeIfValid(anyLong());
    }

    @Test
    void wrongCodeCountsAnAttemptAndKeepsTheCodeUsable() {
        latestOtp(otpInfo(IEnumPasswordStatus.Types.VALID, false, LocalDateTime.now()));
        when(encryptor.checkPassword("000000", "hashed-otp")).thenReturn(false);
        when(passwordInfoRepository.findFailedAttempts(INFO_ID)).thenReturn(1);

        assertEquals(IEnumPasswordStatus.Types.BAD, matchOtp("000000"));

        verify(passwordInfoRepository).incrementFailedAttempts(INFO_ID);
        verify(passwordInfoRepository, never()).lockIfValid(anyLong());
        verify(passwordInfoRepository, never()).save(any());
    }

    @Test
    void fifthWrongCodeLocksTheCode() {
        latestOtp(otpInfo(IEnumPasswordStatus.Types.VALID, false, LocalDateTime.now()));
        when(encryptor.checkPassword("000000", "hashed-otp")).thenReturn(false);
        when(passwordInfoRepository.findFailedAttempts(INFO_ID)).thenReturn(5);

        assertEquals(IEnumPasswordStatus.Types.LOCKED, matchOtp("000000"));

        verify(passwordInfoRepository).lockIfValid(INFO_ID);
    }

    @Test
    void emptyCodeIsAWrongAttempt() {
        latestOtp(otpInfo(IEnumPasswordStatus.Types.VALID, false, LocalDateTime.now()));
        when(passwordInfoRepository.findFailedAttempts(INFO_ID)).thenReturn(1);

        assertEquals(IEnumPasswordStatus.Types.BAD, matchOtp(""));

        verify(passwordInfoRepository).incrementFailedAttempts(INFO_ID);
        verifyNoInteractions(encryptor);
    }

    @Test
    void nonValidStatusesAreFinalAndNeverChecked() {
        for (IEnumPasswordStatus.Types status : List.of(IEnumPasswordStatus.Types.LOCKED,
                IEnumPasswordStatus.Types.DEPRECATED, IEnumPasswordStatus.Types.EXPIRED,
                IEnumPasswordStatus.Types.BAD, IEnumPasswordStatus.Types.BROKEN)) {
            latestOtp(otpInfo(status, false, LocalDateTime.now()));

            assertEquals(status, matchOtp("123456"));
        }

        verifyNoInteractions(encryptor);
        verify(passwordInfoRepository, never()).incrementFailedAttempts(anyLong());
    }

    @Test
    void expiredCodeIsMarkedExpired() {
        latestOtp(otpInfo(IEnumPasswordStatus.Types.VALID, true, LocalDateTime.now().minusMinutes(10)));

        assertEquals(IEnumPasswordStatus.Types.EXPIRED, matchOtp("123456"));

        verify(passwordInfoRepository).save(any(PasswordInfo.class));
        verifyNoInteractions(encryptor);
    }

    // ---- single use

    @Test
    void tamperedRowIsBroken() {
        PasswordInfo info = otpInfo(IEnumPasswordStatus.Types.VALID, false, LocalDateTime.now());
        info.setCrc32(info.getCrc32() + 1);
        latestOtp(info);

        assertEquals(IEnumPasswordStatus.Types.BROKEN, matchOtp("123456"));
    }

    @Test
    void consumeOtpSucceedsOnlyForTheCallerThatWinsTheAtomicUpdate() {
        latestOtp(otpInfo(IEnumPasswordStatus.Types.VALID, false, LocalDateTime.now()));
        when(passwordInfoRepository.consumeIfValid(INFO_ID)).thenReturn(1, 0);

        assertTrue(service.consumeOtp(TENANT, USER));
        assertFalse(service.consumeOtp(TENANT, USER));
    }

    // ---- length and generation

    @Test
    void consumeOtpWithoutACodeIsFalse() {
        when(passwordInfoRepository.findByUserIdAndAuthTypeOrderByCreateDateDesc(ACCOUNT_ID, IEnumAuth.Types.OTP))
                .thenReturn(List.of());

        assertFalse(service.consumeOtp(TENANT, USER));
    }

    @Test
    void defaultCodeIsSixDigits() {
        when(passwordConfigRepository.findByTenantIgnoreCaseAndType(TENANT, IEnumAuth.Types.OTP))
                .thenReturn(Optional.empty());
        when(randomKeyGenerator.nextGuid(6, IEnumCharSet.Types.NUMERIC)).thenReturn("123456");
        when(encryptor.encryptPassword("123456")).thenReturn("hashed-otp");
        when(passwordInfoRepository.save(any(PasswordInfo.class))).thenAnswer(call -> call.getArgument(0));

        AccessKeyResponseDto response = service.registerNewPassword(TENANT, account, null, IEnumAuth.Types.OTP);

        assertEquals(6, response.getLength());
        verify(randomKeyGenerator).nextGuid(6, IEnumCharSet.Types.NUMERIC);
    }

    @Test
    void tenantConfiguredShortLengthIsRaisedToTheMinimum() {
        PasswordConfig config = PasswordConfig.builder().maxLength(4).lifeTime(3)
                .charSetType(IEnumCharSet.Types.NUMERIC).build();
        when(passwordConfigRepository.findByTenantIgnoreCaseAndType(TENANT, IEnumAuth.Types.OTP))
                .thenReturn(Optional.of(config));
        when(randomKeyGenerator.nextGuid(6, IEnumCharSet.Types.NUMERIC)).thenReturn("123456");
        when(encryptor.encryptPassword("123456")).thenReturn("hashed-otp");
        when(passwordInfoRepository.save(any(PasswordInfo.class))).thenAnswer(call -> call.getArgument(0));

        assertEquals(6, service.registerNewPassword(TENANT, account, null, IEnumAuth.Types.OTP).getLength());
    }

    @Test
    void tenantConfiguredLongLengthIsCappedForTheLoginForm() {
        PasswordConfig config = PasswordConfig.builder().maxLength(12).lifeTime(3)
                .charSetType(IEnumCharSet.Types.NUMERIC).build();
        when(passwordConfigRepository.findByTenantIgnoreCaseAndType(TENANT, IEnumAuth.Types.OTP))
                .thenReturn(Optional.of(config));
        when(randomKeyGenerator.nextGuid(8, IEnumCharSet.Types.NUMERIC)).thenReturn("12345678");
        when(encryptor.encryptPassword("12345678")).thenReturn("hashed-otp");
        when(passwordInfoRepository.save(any(PasswordInfo.class))).thenAnswer(call -> call.getArgument(0));

        assertEquals(8, service.registerNewPassword(TENANT, account, null, IEnumAuth.Types.OTP).getLength());
    }

    // ---- resend cooldown

    @Test
    void storedValueIsTheHashNotTheCode() {
        when(passwordConfigRepository.findByTenantIgnoreCaseAndType(TENANT, IEnumAuth.Types.OTP))
                .thenReturn(Optional.empty());
        when(randomKeyGenerator.nextGuid(6, IEnumCharSet.Types.NUMERIC)).thenReturn("123456");
        when(encryptor.encryptPassword("123456")).thenReturn("hashed-otp");
        org.mockito.ArgumentCaptor<PasswordInfo> saved = org.mockito.ArgumentCaptor.forClass(PasswordInfo.class);
        when(passwordInfoRepository.save(saved.capture())).thenAnswer(call -> call.getArgument(0));

        service.registerNewPassword(TENANT, account, null, IEnumAuth.Types.OTP);

        assertEquals("hashed-otp", saved.getValue().getPassword());
        assertEquals(IEnumPasswordStatus.Types.VALID, saved.getValue().getStatus());
    }

    @Test
    void requestInsideTheCooldownReusesTheCodeAndSendsNothing() throws Exception {
        when(tenantService.isEnabled(TENANT)).thenReturn(true);
        when(tenantService.checkAccountIfExists(eq(TENANT), any(), any(), eq(USER), any(), eq(true)))
                .thenReturn(account);
        latestOtp(otpInfo(IEnumPasswordStatus.Types.VALID, false, LocalDateTime.now().minusSeconds(10)));
        when(passwordConfigRepository.findByTenantIgnoreCaseAndType(TENANT, IEnumAuth.Types.OTP))
                .thenReturn(Optional.empty());

        AccessKeyResponseDto response = service.generateAccountPasswordAndSendEmail(
                TENANT, TENANT, null, "a@acme.com", USER, "Alice", IEnumAuth.Types.OTP);

        assertEquals(6, response.getLength());
        verify(passwordInfoRepository, never()).save(any());
        verify(passwordInfoRepository, never()).deactivateOldPasswords(anyLong(), any());
        verifyNoInteractions(emailSenderService);
        verify(randomKeyGenerator, never()).nextGuid(anyInt(), any());
    }

    @Test
    void requestAfterTheCooldownIssuesAFreshCode() throws Exception {
        when(tenantService.isEnabled(TENANT)).thenReturn(true);
        when(tenantService.checkAccountIfExists(eq(TENANT), any(), any(), eq(USER), any(), eq(true)))
                .thenReturn(account);
        latestOtp(otpInfo(IEnumPasswordStatus.Types.VALID, false, LocalDateTime.now().minusSeconds(120)));
        when(passwordConfigRepository.findByTenantIgnoreCaseAndType(TENANT, IEnumAuth.Types.OTP))
                .thenReturn(Optional.empty());
        when(randomKeyGenerator.nextGuid(6, IEnumCharSet.Types.NUMERIC)).thenReturn("654321");
        when(encryptor.encryptPassword("654321")).thenReturn("hashed-otp");
        when(passwordInfoRepository.save(any(PasswordInfo.class))).thenAnswer(call -> call.getArgument(0));

        service.generateAccountPasswordAndSendEmail(TENANT, TENANT, null, "a@acme.com", USER, "Alice", IEnumAuth.Types.OTP);

        verify(passwordInfoRepository).deactivateOldPasswords(ACCOUNT_ID, IEnumAuth.Types.OTP);
        verify(emailSenderService).sendOTPEmail(any(), any(), eq("654321"), any());
    }
}
