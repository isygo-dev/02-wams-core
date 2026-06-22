package eu.isygoit.ui.ims.views.account.dialog;

import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.ims.views.account.AccountManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class EnableDisableAccountDialog extends PinBaseActionDialog {

    private final AccountManagementView parentView;
    private final AccountService accountService;
    private final Long accountId;
    private final boolean currentlyEnabled;

    public EnableDisableAccountDialog(AccountManagementView parentView,
                                      AccountService accountService,
                                      Long accountId,
                                      Runnable onSuccess) {
        super(determineTitle(accountService, accountId),
                determineWarning(accountService, accountId),
                onSuccess,
                false); // requirePin = false (simple confirmation)
        this.parentView = parentView;
        this.accountService = accountService;
        this.accountId = accountId;
        this.currentlyEnabled = fetchCurrentStatus();

        setOkButtonText(currentlyEnabled ? "Disable" : "Enable");
        setWidth("450px");
    }

    private static String determineTitle(AccountService accountService, Long accountId) {
        boolean enabled = fetchCurrentStatusStatic(accountService, accountId);
        return enabled ? "Disable account" : "Enable account";
    }

    private static String determineWarning(AccountService accountService, Long accountId) {
        boolean enabled = fetchCurrentStatusStatic(accountService, accountId);
        return enabled ?
                "Are you sure you want to disable this account? The user will not be able to log in." :
                "Are you sure you want to enable this account? The user will regain access.";
    }

    private static boolean fetchCurrentStatusStatic(AccountService accountService, Long accountId) {
        try {
            ResponseEntity<AccountDto> response = accountService.findById(accountId);
            if (response.getBody() != null) {
                return response.getBody().getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED;
            }
        } catch (Exception ignored) {
        }
        return true; // default
    }

    private boolean fetchCurrentStatus() {
        return fetchCurrentStatusStatic(accountService, accountId);
    }

    @Override
    protected boolean onOk() {
        parentView.showLoading(true);
        try {
            ResponseEntity<AccountDto> response = accountService.findById(accountId);
            if (response.getBody() == null) {
                append("Account not found");
                return false;
            }
            AccountDto account = response.getBody();
            account.setAdminStatus(currentlyEnabled ?
                    IEnumEnabledBinaryStatus.Types.DISABLED :
                    IEnumEnabledBinaryStatus.Types.ENABLED);

            ResponseEntity<AccountDto> updateResponse = accountService.update(accountId, account);
            if (!updateResponse.getStatusCode().is2xxSuccessful()) {
                append("Failed to update account status");
                return false;
            }

            append(currentlyEnabled ? "Account disabled" : "Account enabled");
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append("Failed operation: " + e.getMessage());
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }

    private String extractErrorMessage(FeignException ex) {
        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank()) {
                return ex.contentUTF8();
            }
        } catch (Exception ignored) {
        }
        return ex.getMessage();
    }
}