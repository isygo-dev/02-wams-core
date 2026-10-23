package eu.isygoit.controller;

import eu.isygoit.dto.common.TokenResponseDto;
import eu.isygoit.dto.request.AccessRequestDto;
import eu.isygoit.dto.response.AccessTokenResponseDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumPasswordStatus;
import eu.isygoit.service.IPasswordService;
import eu.isygoit.service.ITenantService;
import eu.isygoit.service.ITokenBuilderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * A1: KMS must never mint tokens unless the credential check is VALID.
 */
@ExtendWith(MockitoExtension.class)
class PublicPasswordControllerAccessTest {

    private static final String TENANT = "acme";
    private static final String USER = "alice";

    @Mock
    private ITokenBuilderService tokenService;
    @Mock
    private IPasswordService passwordService;
    @Mock
    private ITenantService tenantService;
    @InjectMocks
    private PublicPasswordController controller;

    private AccessRequestDto request;

    private static TokenResponseDto tokenOf(String value) {
        TokenResponseDto dto = mock(TokenResponseDto.class);
        when(dto.getToken()).thenReturn(value);
        return dto;
    }

    @BeforeEach
    void setUp() {
        request = AccessRequestDto.builder()
                .tenant(TENANT)
                .userName(USER)
                .application("webapp-gw")
                .password("secret")
                .isAdmin(false)
                .authType(IEnumAuth.Types.PWD)
                .authorities(List.of("ROLE_USER"))
                .build();
        when(tenantService.isEnabled(TENANT)).thenReturn(true);
    }

    @ParameterizedTest
    @EnumSource(value = IEnumPasswordStatus.Types.class, names = "VALID", mode = EnumSource.Mode.EXCLUDE)
    void nonValidStatusReturnsStatusOnlyAndNeverBuildsTokens(IEnumPasswordStatus.Types status) {
        when(passwordService.matches(TENANT, USER, "secret", IEnumAuth.Types.PWD)).thenReturn(status);

        ResponseEntity<AccessTokenResponseDto> response = controller.getAccess(request);

        AccessTokenResponseDto body = response.getBody();
        assertNotNull(body);
        assertEquals(status, body.getStatus());
        assertNull(body.getAccessToken());
        assertNull(body.getRefreshToken());
        assertNull(body.getAuthorityToken());
        verifyNoInteractions(tokenService);
    }

    @Test
    void nullStatusIsTreatedAsUnauthorizedAndNeverBuildsTokens() {
        when(passwordService.matches(TENANT, USER, "secret", IEnumAuth.Types.PWD)).thenReturn(null);

        AccessTokenResponseDto body = controller.getAccess(request).getBody();

        assertNotNull(body);
        assertEquals(IEnumPasswordStatus.Types.UNAUTHORIZED, body.getStatus());
        assertNull(body.getAccessToken());
        assertNull(body.getRefreshToken());
        assertNull(body.getAuthorityToken());
        verifyNoInteractions(tokenService);
    }

    @Test
    void validStatusReturnsAllThreeTokens() {
        when(passwordService.matches(TENANT, USER, "secret", IEnumAuth.Types.PWD))
                .thenReturn(IEnumPasswordStatus.Types.VALID);
        when(tokenService.buildAccessToken(anyString(), anySet(), anyString(), anyBoolean()))
                .thenReturn(tokenOf("access"));
        when(tokenService.buildRefreshToken(anyString(), anySet(), anyString()))
                .thenReturn(tokenOf("refresh"));
        when(tokenService.buildAuthorityToken(anyString(), anySet(), anyString(), anyList()))
                .thenReturn(tokenOf("authority"));

        AccessTokenResponseDto body = controller.getAccess(request).getBody();

        assertNotNull(body);
        assertEquals(IEnumPasswordStatus.Types.VALID, body.getStatus());
        assertEquals("access", body.getAccessToken());
        assertEquals("refresh", body.getRefreshToken());
        assertEquals("authority", body.getAuthorityToken());
    }

    @Test
    void otpAlreadyConsumedByAConcurrentLoginGetsNoTokens() {
        request.setAuthType(IEnumAuth.Types.OTP);
        when(passwordService.matches(TENANT, USER, "secret", IEnumAuth.Types.OTP))
                .thenReturn(IEnumPasswordStatus.Types.VALID);
        when(passwordService.consumeOtp(TENANT, USER)).thenReturn(false);

        AccessTokenResponseDto body = controller.getAccess(request).getBody();

        assertNotNull(body);
        assertEquals(IEnumPasswordStatus.Types.DEPRECATED, body.getStatus());
        assertNull(body.getAccessToken());
        verifyNoInteractions(tokenService);
    }

    @Test
    void validOtpIsConsumedAndTokensAreIssued() {
        request.setAuthType(IEnumAuth.Types.OTP);
        when(passwordService.matches(TENANT, USER, "secret", IEnumAuth.Types.OTP))
                .thenReturn(IEnumPasswordStatus.Types.VALID);
        when(passwordService.consumeOtp(TENANT, USER)).thenReturn(true);
        when(tokenService.buildAccessToken(anyString(), anySet(), anyString(), anyBoolean()))
                .thenReturn(tokenOf("access"));
        when(tokenService.buildRefreshToken(anyString(), anySet(), anyString()))
                .thenReturn(tokenOf("refresh"));
        when(tokenService.buildAuthorityToken(anyString(), anySet(), anyString(), anyList()))
                .thenReturn(tokenOf("authority"));

        AccessTokenResponseDto body = controller.getAccess(request).getBody();

        assertNotNull(body);
        assertEquals(IEnumPasswordStatus.Types.VALID, body.getStatus());
        assertEquals("access", body.getAccessToken());
        verify(passwordService).consumeOtp(TENANT, USER);
    }

    @Test
    void passwordLoginNeverTouchesOtpConsumption() {
        when(passwordService.matches(TENANT, USER, "secret", IEnumAuth.Types.PWD))
                .thenReturn(IEnumPasswordStatus.Types.BAD);

        controller.getAccess(request);

        verify(passwordService, never()).consumeOtp(any(), any());
    }

    @Test
    void wrongPasswordDoesNotReachTokenBuilders() {
        when(passwordService.matches(TENANT, USER, "secret", IEnumAuth.Types.PWD))
                .thenReturn(IEnumPasswordStatus.Types.BAD);

        controller.getAccess(request);

        verify(tokenService, never()).buildAccessToken(any(), any(), any(), any());
        verify(tokenService, never()).buildRefreshToken(any(), any(), any());
        verify(tokenService, never()).buildAuthorityToken(any(), any(), any(), any());
    }
}
