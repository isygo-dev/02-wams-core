package eu.isygoit.controller;

import eu.isygoit.annotation.InjectExceptionHandler;
import eu.isygoit.api.PasswordServiceApi;
import eu.isygoit.api.ProfileServiceApi;
import eu.isygoit.com.rest.controller.ResponseFactory;
import eu.isygoit.com.rest.controller.impl.ControllerExceptionHandler;
import eu.isygoit.config.AppProperties;
import eu.isygoit.dto.common.ChangePasswordRequestDto;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.common.ResourceDto;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.data.AccountStatDto;
import eu.isygoit.dto.request.MatchesRequestDto;
import eu.isygoit.enums.IEnumPasswordStatus;
import eu.isygoit.exception.*;
import eu.isygoit.exception.handler.ImsExceptionHandler;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.mapper.AccountMapper;
import eu.isygoit.model.Account;
import eu.isygoit.model.AccountDetails;
import eu.isygoit.service.IAccountService;
import eu.isygoit.service.RequestContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Profile Controller for managing user profiles.
 * Uses RequestContext to identify the authenticated user (tenant + username).
 */
@Slf4j
@Validated
@RestController
@InjectExceptionHandler(ImsExceptionHandler.class)
@RequestMapping(path = "/api/v1/private/profile")
public class ProfileController extends ControllerExceptionHandler implements ProfileServiceApi {

    private final AppProperties appProperties;

    @Autowired
    private RequestContextService requestContextService;
    @Autowired
    private IAccountService accountService;
    @Autowired
    private PasswordServiceApi passwordServiceApi;
    @Autowired
    private AccountMapper accountMapper;

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
     * @throws AccountNotFoundException if account is not found
     */
    private Account getAuthenticatedAccount() {
        RequestContextDto requestContext = requestContextService.getCurrentContext();

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
    public ResponseEntity<AccountDto> getProfile() {
        log.info("Getting profile for current user");

        Account account = getAuthenticatedAccount();
        return ResponseFactory.responseOk(accountMapper.entityToDto(account));
    }

    @Override
    public ResponseEntity<AccountDto> updateProfile(AccountDto accountDto) {
        log.info("Updating profile for current user");

        Account account = getAuthenticatedAccount();

        // Update account fields
        if (accountDto.getPhoneNumber() != null) {
            account.setPhoneNumber(accountDto.getPhoneNumber());
        }
        if (accountDto.getLanguage() != null) {
            account.setLanguage(accountDto.getLanguage());
        }
        if (accountDto.getEmail() != null) {
            account.setEmail(accountDto.getEmail());
        }

        // Update account details
        AccountDetails details = account.getAccountDetails();
        if (details == null) {
            details = AccountDetails.builder().build();
            account.setAccountDetails(details);
        }

        if (accountDto.getAccountDetails() != null) {
            if (accountDto.getAccountDetails().getFirstName() != null) {
                details.setFirstName(accountDto.getAccountDetails().getFirstName());
            }
            if (accountDto.getAccountDetails().getLastName() != null) {
                details.setLastName(accountDto.getAccountDetails().getLastName());
            }
            if (accountDto.getAccountDetails().getCountry() != null) {
                details.setCountry(accountDto.getAccountDetails().getCountry());
            }
        }

        Account updatedAccount = accountService.update(account.getTenant(), account);
        return ResponseFactory.responseOk(accountMapper.entityToDto(updatedAccount));
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
        return ResponseFactory.responseOk(accountMapper.entityToDto(updatedAccount));
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

        if (account.getConnectionTracking() != null && !account.getConnectionTracking().isEmpty()) {
            builder.lastLogin(DateHelper.legacyDatetoLocalDateTime(account.getConnectionTracking().get(0).getLoginDate()));
            builder.totalConnections(account.getConnectionTracking().size());
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
}