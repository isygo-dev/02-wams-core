package eu.isygoit.service.impl;

import eu.isygoit.dto.request.AuthenticationContextRequest;
import eu.isygoit.dto.request.ChangeAuthTypeRequestDto;
import eu.isygoit.dto.request.MatchesRequestDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumPasswordStatus;
import eu.isygoit.model.Account;
import eu.isygoit.remote.kms.KmsPasswordService;
import eu.isygoit.remote.kms.KmsPublicPasswordService;
import eu.isygoit.service.IAccountService;
import eu.isygoit.service.impl.AuthTypeChangeService.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * A7: only an authenticated, re-authenticated caller may change their own auth type (or a tenant admin another
 * account of the same tenant).
 */
@ExtendWith(MockitoExtension.class)
class AuthTypeChangeServiceTest {

    private static final String TENANT = "acme";

    @Mock
    private IAccountService accountService;
    @Mock
    private KmsPasswordService kmsPasswordService;
    @Mock
    private KmsPublicPasswordService kmsPublicPasswordService;

    private AuthTypeChangeService service() {
        return new AuthTypeChangeService(accountService, kmsPasswordService, kmsPublicPasswordService);
    }

    private Account account(String code, IEnumAuth.Types type, boolean admin) {
        Account account = mock(Account.class);
        lenient().when(account.getCode()).thenReturn(code);
        lenient().when(account.getTenant()).thenReturn(TENANT);
        lenient().when(account.getAuthType()).thenReturn(type);
        lenient().when(account.getIsAdmin()).thenReturn(admin);
        return account;
    }

    private ChangeAuthTypeRequestDto request(String userName, IEnumAuth.Types target, String credential) {
        return ChangeAuthTypeRequestDto.builder().userName(userName).authType(target).currentCredential(credential).build();
    }

    @Test
    void unsupportedTargetTypeIsRejectedBeforeAnyLookup() {
        Result result = service().changeAuthType(TENANT, "alice", request(null, IEnumAuth.Types.TOKEN, "pwd"));

        assertEquals(Result.INVALID_AUTH_TYPE, result);
        verifyNoInteractions(accountService, kmsPasswordService, kmsPublicPasswordService);
    }

    @Test
    void unknownCallerIsNotAllowed() {
        when(accountService.findByTenantAndUserName(TENANT, "ghost")).thenReturn(null);

        Result result = service().changeAuthType(TENANT, "ghost", request(null, IEnumAuth.Types.PWD, "pwd"));

        assertEquals(Result.NOT_ALLOWED, result);
        verify(accountService, never()).switchAuthType(any(), any());
    }

    @Test
    void nonAdminCannotTargetAnotherAccountAndKmsIsNeverCalled() {
        when(accountService.findByTenantAndUserName(TENANT, "alice")).thenReturn(account("alice", IEnumAuth.Types.PWD, false));

        Result result = service().changeAuthType(TENANT, "alice", request("bob", IEnumAuth.Types.PWD, "pwd"));

        assertEquals(Result.NOT_ALLOWED, result);
        verifyNoInteractions(kmsPasswordService, kmsPublicPasswordService);
        verify(accountService, never()).switchAuthType(any(), any());
    }

    @Test
    void wrongCredentialFailsReauthenticationAndChangesNothing() {
        when(accountService.findByTenantAndUserName(TENANT, "alice")).thenReturn(account("alice", IEnumAuth.Types.PWD, false));
        when(kmsPasswordService.matchesPassword(any())).thenReturn(ResponseEntity.ok(IEnumPasswordStatus.Types.BAD));

        Result result = service().changeAuthType(TENANT, "alice", request(null, IEnumAuth.Types.OTP, "wrong"));

        assertEquals(Result.REAUTH_FAILED, result);
        verify(accountService, never()).switchAuthType(any(), any());
    }

    @Test
    void nullKmsBodyFailsClosed() {
        when(accountService.findByTenantAndUserName(TENANT, "alice")).thenReturn(account("alice", IEnumAuth.Types.PWD, false));
        when(kmsPasswordService.matchesPassword(any())).thenReturn(ResponseEntity.ok().build());

        Result result = service().changeAuthType(TENANT, "alice", request(null, IEnumAuth.Types.OTP, "pwd"));

        assertEquals(Result.REAUTH_FAILED, result);
        verify(accountService, never()).switchAuthType(any(), any());
    }

    @Test
    void kmsFailureFailsClosed() {
        when(accountService.findByTenantAndUserName(TENANT, "alice")).thenReturn(account("alice", IEnumAuth.Types.PWD, false));
        when(kmsPasswordService.matchesPassword(any())).thenThrow(new RuntimeException("kms down"));

        Result result = service().changeAuthType(TENANT, "alice", request(null, IEnumAuth.Types.OTP, "pwd"));

        assertEquals(Result.REAUTH_FAILED, result);
        verify(accountService, never()).switchAuthType(any(), any());
    }

    @Test
    void otpCallerIsReauthenticatedWithTheOtpEndpoint() {
        when(accountService.findByTenantAndUserName(TENANT, "alice")).thenReturn(account("alice", IEnumAuth.Types.OTP, false));
        when(kmsPublicPasswordService.matchesOtp(any())).thenReturn(ResponseEntity.ok(IEnumPasswordStatus.Types.VALID));

        Result result = service().changeAuthType(TENANT, "alice", request(null, IEnumAuth.Types.PWD, "1234"));

        assertEquals(Result.CHANGED, result);
        verifyNoInteractions(kmsPasswordService);
    }

    @Test
    void qrCallerConfirmsWithTheAccountPasswordNotAQrCode() {
        when(accountService.findByTenantAndUserName(TENANT, "alice")).thenReturn(account("alice", IEnumAuth.Types.QRC, false));
        when(kmsPasswordService.matchesPassword(any())).thenReturn(ResponseEntity.ok(IEnumPasswordStatus.Types.VALID));

        Result result = service().changeAuthType(TENANT, "alice", request(null, IEnumAuth.Types.PWD, "pwd"));

        assertEquals(Result.CHANGED, result);
        ArgumentCaptor<MatchesRequestDto> captor = ArgumentCaptor.forClass(MatchesRequestDto.class);
        verify(kmsPasswordService).matchesPassword(captor.capture());
        assertEquals(IEnumAuth.Types.PWD, captor.getValue().getAuthType());
        verifyNoInteractions(kmsPublicPasswordService);
    }

    @Test
    void switchingToQrIsAllowedWithAValidCredential() {
        when(accountService.findByTenantAndUserName(TENANT, "alice")).thenReturn(account("alice", IEnumAuth.Types.PWD, false));
        when(kmsPasswordService.matchesPassword(any())).thenReturn(ResponseEntity.ok(IEnumPasswordStatus.Types.VALID));

        Result result = service().changeAuthType(TENANT, "alice", request(null, IEnumAuth.Types.QRC, "pwd"));

        assertEquals(Result.CHANGED, result);
        ArgumentCaptor<AuthenticationContextRequest> captor = ArgumentCaptor.forClass(AuthenticationContextRequest.class);
        verify(accountService).switchAuthType(eq(TENANT), captor.capture());
        assertEquals(IEnumAuth.Types.QRC, captor.getValue().getAuthType());
    }

    @Test
    void selfChangeWithValidCredentialUsesCallerTenantAndOwnAccount() {
        when(accountService.findByTenantAndUserName(TENANT, "alice")).thenReturn(account("alice", IEnumAuth.Types.PWD, false));
        when(kmsPasswordService.matchesPassword(any())).thenReturn(ResponseEntity.ok(IEnumPasswordStatus.Types.VALID));

        Result result = service().changeAuthType(TENANT, "alice", request(null, IEnumAuth.Types.OTP, "pwd"));

        assertEquals(Result.CHANGED, result);
        ArgumentCaptor<AuthenticationContextRequest> captor = ArgumentCaptor.forClass(AuthenticationContextRequest.class);
        verify(accountService).switchAuthType(eq(TENANT), captor.capture());
        assertEquals(TENANT, captor.getValue().getTenant());
        assertEquals("alice", captor.getValue().getUserName());
        assertEquals(IEnumAuth.Types.OTP, captor.getValue().getAuthType());
    }

    @Test
    void tenantAdminCanChangeAnotherAccountInTheSameTenant() {
        when(accountService.findByTenantAndUserName(TENANT, "admin")).thenReturn(account("admin", IEnumAuth.Types.PWD, true));
        when(accountService.findByTenantAndUserName(TENANT, "bob")).thenReturn(account("bob", IEnumAuth.Types.OTP, false));
        when(kmsPasswordService.matchesPassword(any())).thenReturn(ResponseEntity.ok(IEnumPasswordStatus.Types.VALID));

        Result result = service().changeAuthType(TENANT, "admin", request("bob", IEnumAuth.Types.PWD, "admin-pwd"));

        assertEquals(Result.CHANGED, result);
        ArgumentCaptor<AuthenticationContextRequest> captor = ArgumentCaptor.forClass(AuthenticationContextRequest.class);
        verify(accountService).switchAuthType(eq(TENANT), captor.capture());
        assertEquals("bob", captor.getValue().getUserName());
    }

    @Test
    void adminTargetingMissingAccountGetsNotFound() {
        when(accountService.findByTenantAndUserName(TENANT, "admin")).thenReturn(account("admin", IEnumAuth.Types.PWD, true));
        when(accountService.findByTenantAndUserName(TENANT, "nobody")).thenReturn(null);
        when(kmsPasswordService.matchesPassword(any())).thenReturn(ResponseEntity.ok(IEnumPasswordStatus.Types.VALID));

        Result result = service().changeAuthType(TENANT, "admin", request("nobody", IEnumAuth.Types.PWD, "admin-pwd"));

        assertEquals(Result.NOT_FOUND, result);
        verify(accountService, never()).switchAuthType(any(), any());
    }
}
