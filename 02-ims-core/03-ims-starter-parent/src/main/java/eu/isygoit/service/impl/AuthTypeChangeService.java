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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Authorizes and re-authenticates an auth-type change. The caller identity comes from the verified JWT context,
 * never from the request body.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthTypeChangeService {

    private static final Set<IEnumAuth.Types> ALLOWED_TARGET_TYPES =
            EnumSet.of(IEnumAuth.Types.PWD, IEnumAuth.Types.OTP, IEnumAuth.Types.QRC);

    private final IAccountService accountService;
    private final KmsPasswordService kmsPasswordService;
    private final KmsPublicPasswordService kmsPublicPasswordService;

    public enum Result {CHANGED, NOT_ALLOWED, REAUTH_FAILED, NOT_FOUND, INVALID_AUTH_TYPE}

    public Result changeAuthType(String callerTenant, String callerUser, ChangeAuthTypeRequestDto request) {
        if (request.getAuthType() == null || !ALLOWED_TARGET_TYPES.contains(request.getAuthType())) {
            return Result.INVALID_AUTH_TYPE;
        }

        Account caller = accountService.findByTenantAndUserName(callerTenant, callerUser);
        if (caller == null) {
            return Result.NOT_ALLOWED;
        }

        String targetUser = StringUtils.hasText(request.getUserName()) ? request.getUserName().trim() : callerUser;
        boolean self = targetUser.equalsIgnoreCase(caller.getCode());
        // Admin rights come from the stored account, not from a token claim.
        if (!self && !Boolean.TRUE.equals(caller.getIsAdmin())) {
            return Result.NOT_ALLOWED;
        }

        if (!credentialIsValid(caller, request.getCurrentCredential())) {
            return Result.REAUTH_FAILED;
        }

        Account target = self ? caller : accountService.findByTenantAndUserName(callerTenant, targetUser);
        if (target == null) {
            return Result.NOT_FOUND;
        }

        accountService.switchAuthType(callerTenant, AuthenticationContextRequest.builder()
                .tenant(callerTenant)
                .userName(target.getCode())
                .authType(request.getAuthType())
                .build());
        log.info("Auth type changed to {} (self={})", request.getAuthType(), self);
        return Result.CHANGED;
    }

    /**
     * Sends a one-time code to the caller so it can be used as the current credential. Only meaningful for an OTP
     * account; the code generator applies its own resend cooldown.
     *
     * @return true if a code was requested, false if the caller does not use OTP
     */
    public boolean sendReauthenticationCode(String callerTenant, String callerUser) {
        Account caller = accountService.findByTenantAndUserName(callerTenant, callerUser);
        if (caller == null || caller.getAuthType() != IEnumAuth.Types.OTP) {
            return false;
        }
        accountService.resolveAuthContext(AuthenticationContextRequest.builder()
                .tenant(callerTenant)
                .userName(callerUser)
                .build());
        return true;
    }

    /**
     * Verifies the caller's own credential using the caller's current auth type. Only VALID counts; any other
     * status, a null body or a KMS failure fails closed.
     */
    private boolean credentialIsValid(Account caller, String credential) {
        if (!StringUtils.hasText(credential) || caller.getAuthType() == null) {
            return false;
        }
        // A QR code cannot be typed, so QR accounts confirm a change with the account password instead.
        IEnumAuth.Types verifyWith = caller.getAuthType() == IEnumAuth.Types.QRC
                ? IEnumAuth.Types.PWD
                : caller.getAuthType();
        MatchesRequestDto matches = MatchesRequestDto.builder()
                .tenant(caller.getTenant().toLowerCase(Locale.ROOT))
                .userName(caller.getCode().toLowerCase(Locale.ROOT))
                .authType(verifyWith)
                .password(credential)
                .build();
        try {
            IEnumPasswordStatus.Types status = switch (verifyWith) {
                case PWD -> kmsPasswordService.matchesPassword(matches).getBody();
                case OTP -> kmsPublicPasswordService.matchesOtp(matches).getBody();
                default -> null;
            };
            return status == IEnumPasswordStatus.Types.VALID;
        } catch (Exception e) {
            log.warn("Re-authentication check failed: {}", e.getClass().getSimpleName());
            return false;
        }
    }
}
