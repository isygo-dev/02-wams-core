package eu.isygoit.controller;

import eu.isygoit.annotation.InjectExceptionHandler;
import eu.isygoit.api.PasswordServiceApi;
import eu.isygoit.api.ProfileServiceApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.impl.ControllerUtils;
import eu.isygoit.config.AppProperties;
import eu.isygoit.dto.common.ChangePasswordRequestDto;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.common.ResourceDto;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.data.AccountStatDto;
import eu.isygoit.dto.data.ConnectionPageDto;
import eu.isygoit.dto.data.DailyLoginCountDto;
import eu.isygoit.dto.request.ChangeAuthTypeRequestDto;
import eu.isygoit.dto.request.MatchesRequestDto;
import eu.isygoit.enums.IEnumPasswordStatus;
import eu.isygoit.exception.*;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.mapper.AccountMapper;
import eu.isygoit.model.Account;
import eu.isygoit.model.AccountDetails;
import eu.isygoit.model.ConnectionTracking;
import eu.isygoit.service.IAccountService;
import eu.isygoit.service.impl.AuthTypeChangeService;
import eu.isygoit.service.impl.ProfileActivityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Profile Controller for managing user profiles.
 * Uses RequestContext to identify the authenticated user (tenant + username).
 */
@Slf4j
@Validated
@RestController
@InjectExceptionHandler(ImsExceptionHandler.class)
@RequestMapping(path = "/api/v1/private/profile")
public class ProfileController extends ControllerUtils implements ProfileServiceApi {

    private final AppProperties appProperties;


    @Autowired
    private IAccountService accountService;
    @Autowired
    private PasswordServiceApi passwordServiceApi;
    @Autowired
    private AccountMapper accountMapper;
    @Autowired
    private AuthTypeChangeService authTypeChangeService;
    @Autowired
    private ProfileActivityService profileActivityService;

    /**
     * Instantiates a new Profile controller.
     *
     * @param appProperties the app properties
     */
    public ProfileController(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /**
     * Gets the authenticated user from the request context.
     *
     * @return the authenticated account
     * @throws OperationNotAllowedException if user is not authenticated
     * @throws AccountNotFoundException     if account is not found
     */
    private Account getAuthenticatedAccount() {
        RequestContextDto requestContext = requestContextService().getCurrentContext();

        if (requestContext == null) {
            log.error("Request context is null");
            throw new OperationNotAllowedException("User not authenticated");
        }

        String senderUser = requestContext.getSenderUser();
        String senderTenant = requestContext.getSenderTenant();

        if (senderUser == null || senderUser.isEmpty()) {
            log.error("Sender user is null or empty");
            throw new OperationNotAllowedException("User not authenticated");
        }

        if (senderTenant == null || senderTenant.isEmpty()) {
            log.error("Sender tenant is null or empty");
            throw new OperationNotAllowedException("User not authenticated");
        }

        log.debug("Authenticated user: {} in tenant: {}", senderUser, senderTenant);

        // Find account by tenant and username
        Account account = accountService.findByTenantAndUserName(senderTenant, senderUser);
        if (account == null) {
            throw new AccountNotFoundException("with username: " + senderUser + " in tenant: " + senderTenant);
        }

        return account;
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> changeAuthType(ChangeAuthTypeRequestDto request) {
        Account caller = getAuthenticatedAccount();
        return switch (authTypeChangeService.changeAuthType(caller.getTenant(), caller.getCode(), request)) {
            case CHANGED -> ResponseFactory.responseOk(true);
            case NOT_ALLOWED -> ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            case REAUTH_FAILED -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            case INVALID_AUTH_TYPE -> ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        };
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> sendReauthenticationCode() {
        Account caller = getAuthenticatedAccount();
        return ResponseFactory.responseOk(
                authTypeChangeService.sendReauthenticationCode(caller.getTenant(), caller.getCode()));
    }

    @Override
    public ResponseEntity<AccountDto> getProfile() {
        log.info("Getting profile for current user");

        Account account = getAuthenticatedAccount();
        return ResponseFactory.responseOk(accountMapper.entityToProfileDto(account));
    }

    @Override
    public ResponseEntity<AccountDto> updateProfile(AccountDto accountDto) {
        log.info("Updating profile for current user");

        // The work is done in one transaction by the service, on a freshly loaded account: the edited fields are
        // validated, written and flushed to the database, and what comes back is what was stored.
        Account caller = getAuthenticatedAccount();
        Account saved = accountService.updateProfile(caller.getTenant(), caller.getCode(), accountDto);
        return ResponseFactory.responseOk(accountMapper.entityToProfileDto(saved));
    }

    @Override
    public ResponseEntity<Void> changePassword(ChangePasswordRequestDto changePasswordRequest) {
        log.info("Changing password for current user");

        Account account = getAuthenticatedAccount();

        // Validate new password and confirmation
        if (!changePasswordRequest.getNewPassword().equals(changePasswordRequest.getConfirmPassword())) {
            throw new PasswordNotValidException("New password and confirmation do not match");
        }

        // Password validation (minimum length, complexity, etc.)
        if (changePasswordRequest.getNewPassword().length() < 8) {
            throw new PasswordNotValidException("Password must be at least 8 characters long");
        }

        // First, verify the current password is correct
        try {
            MatchesRequestDto matchesRequest = MatchesRequestDto.builder()
                    .tenant(account.getTenant())
                    .userName(account.getCode())
                    .password(changePasswordRequest.getCurrentPassword())
                    .authType(account.getAuthType())
                    .build();

            ResponseEntity<IEnumPasswordStatus.Types> matchesResponse =
                    passwordServiceApi.matchesPassword(matchesRequest);

            if (matchesResponse.getBody() == null ||
                    matchesResponse.getBody() != IEnumPasswordStatus.Types.VALID) {
                log.error("Current password does not match for user: {}", account.getCode());
                throw new PasswordNotMatchesException("Current password is incorrect");
            }

            // Now change the password using PasswordServiceApi
            ResponseEntity<String> changeResponse = passwordServiceApi.changePassword(
                    changePasswordRequest.getCurrentPassword(),
                    changePasswordRequest.getNewPassword()
            );

            if (!changeResponse.getStatusCode().is2xxSuccessful()) {
                log.error("Failed to change password for user: {}", account.getCode());
                throw new PasswordUpdateException("Failed to change password");
            }

            log.info("Password changed successfully for user: {} in tenant: {}",
                    account.getCode(), account.getTenant());

        } catch (Exception e) {
            log.error("Error changing password for user: {}", account.getCode(), e);
            if (e instanceof PasswordUpdateException) {
                throw e;
            }
            throw new PasswordUpdateException("Failed to change password");
        }

        return ResponseFactory.responseOk();
    }

    @Override
    public ResponseEntity<AccountDto> uploadAvatar(MultipartFile file) throws IOException {
        log.info("Uploading avatar for current user");

        Account account = getAuthenticatedAccount();

        if (file == null || file.isEmpty()) {
            throw new AvatarUploadException("File is empty");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new AvatarUploadException("Only image files are allowed");
        }

        // Validate file size (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new AvatarUploadException("File size exceeds 5MB limit");
        }

        Account updatedAccount = accountService.uploadImage(account.getTenant(), account.getId(), file);
        return ResponseFactory.responseOk(accountMapper.entityToProfileDto(updatedAccount));
    }

    @Override
    public ResponseEntity<Resource> downloadAvatar() throws IOException {
        log.info("Downloading avatar for current user");

        Account account = getAuthenticatedAccount();

        ResourceDto resourceDto = accountService.downloadImage(account.getTenant(), account.getId());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resourceDto.getResource());
    }

    @Override
    public ResponseEntity<Void> deleteAvatar() {
        log.info("Deleting avatar for current user");

        Account account = getAuthenticatedAccount();

        account.setImagePath(null);
        accountService.update(account.getTenant(), account);

        return ResponseFactory.responseOk();
    }

    @Override
    public ResponseEntity<AccountStatDto> getUserStatistics() {
        log.info("Getting statistics for current user");

        Account account = getAuthenticatedAccount();

        // Build statistics from account data
        AccountStatDto.AccountStatDtoBuilder builder = AccountStatDto.builder();

        if (account.getCreateDate() != null) {
            builder.createDate(account.getCreateDate());
        }

        // Count and newest login straight from the database, without loading the whole connection list.
        Page<ConnectionTracking> latest = profileActivityService.latestConnection(account.getId());
        builder.totalConnections((int) Math.min(Integer.MAX_VALUE, latest.getTotalElements()));
        if (latest.hasContent()) {
            ConnectionTracking last = latest.getContent().get(0);
            builder.lastLogin(DateHelper.legacyDatetoLocalDateTime(last.getLoginDate()))
                    .lastLoginDevice(last.getDevice())
                    .lastLoginBrowser(last.getBrowser())
                    .lastLoginIp(last.getIpAddress())
                    .lastLoginApp(last.getLogApp());
        }

        if (account.getRoleInfo() != null) {
            builder.roleCount(account.getRoleInfo().size());

            // Calculate total permissions
            int totalPermissions = account.getRoleInfo().stream()
                    .flatMap(role -> role.getPermissions().stream())
                    .mapToInt(permission -> 1)
                    .sum();
            builder.totalPermissions(totalPermissions);
        }

        return ResponseFactory.responseOk(builder.build());
    }

    @Override
    public ResponseEntity<ConnectionPageDto> getConnections(int page, int size) {
        Account account = getAuthenticatedAccount();
        return ResponseFactory.responseOk(profileActivityService.getConnections(account.getId(), page, size));
    }

    @Override
    public ResponseEntity<List<DailyLoginCountDto>> getLoginActivity(int days) {
        Account account = getAuthenticatedAccount();
        return ResponseFactory.responseOk(profileActivityService.getLoginActivity(account.getId(), days));
    }
}