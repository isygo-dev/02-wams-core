package eu.isygoit.ui.ims.views.account.dialog;

import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.i18n.I18n;
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

        setOkButtonText(currentlyEnabled ? I18n.t("account.dialog.disable.button") : I18n.t("account.dialog.enable.button"));
        setWidth("450px");
    }

    private static String determineTitle(AccountService accountService, Long accountId) {
        boolean enabled = fetchCurrentStatusStatic(accountService, accountId);
        return enabled ? I18n.t("account.dialog.disable.title") : I18n.t("account.dialog.enable.title");
    }

    private static String determineWarning(AccountService accountService, Long accountId) {
        boolean enabled = fetchCurrentStatusStatic(accountService, accountId);
        return enabled ?
                I18n.t("account.dialog.disable.message") :
                I18n.t("account.dialog.enable.message");
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
                append(I18n.t("account.dialog.toggle.not.found"));
                return false;
            }
            AccountDto account = response.getBody();
            account.setAdminStatus(currentlyEnabled ?
                    IEnumEnabledBinaryStatus.Types.DISABLED :
                    IEnumEnabledBinaryStatus.Types.ENABLED);

            ResponseEntity<AccountDto> updateResponse = accountService.update(accountId, account);
            if (!updateResponse.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("account.dialog.toggle.failed"));
                return false;
            }

            append(currentlyEnabled ? I18n.t("account.dialog.disable.success") : I18n.t("account.dialog.enable.success"));
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("account.dialog.toggle.error", e.getMessage()));
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