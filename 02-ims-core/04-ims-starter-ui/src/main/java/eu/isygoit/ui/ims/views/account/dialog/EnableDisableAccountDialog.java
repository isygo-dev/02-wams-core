package eu.isygoit.ui.ims.views.account.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.ui.common.dialog.PinConfirmActionDialog;
import eu.isygoit.ui.ims.views.account.AccountManagementView;
import org.springframework.http.ResponseEntity;

public class EnableDisableAccountDialog extends PinConfirmActionDialog {

    public EnableDisableAccountDialog(AccountManagementView parentView,
                                      AccountService accountService,
                                      Long accountId,
                                      Runnable onSuccess) {
        // The current status is read once, then drives texts and action
        this(parentView, accountService, accountId, onSuccess,
                fetchCurrentStatus(accountService, accountId));
    }

    private EnableDisableAccountDialog(AccountManagementView parentView,
                                       AccountService accountService,
                                       Long accountId,
                                       Runnable onSuccess,
                                       boolean currentlyEnabled) {
        super(new Texts(
                        currentlyEnabled ? I18n.t("ims.account.dialog.disable.title") : I18n.t("ims.account.dialog.enable.title"),
                        currentlyEnabled ? I18n.t("ims.account.dialog.disable.message") : I18n.t("ims.account.dialog.enable.message"),
                        currentlyEnabled ? I18n.t("ims.account.dialog.disable.button") : I18n.t("ims.account.dialog.enable.button"),
                        I18n.t("common.dialog.pin.invalid"),
                        currentlyEnabled ? I18n.t("ims.account.dialog.disable.success") : I18n.t("ims.account.dialog.enable.success"),
                        detail -> detail != null && detail.startsWith("HTTP ")
                                ? I18n.t("ims.account.dialog.toggle.failed")
                                : I18n.t("ims.account.dialog.toggle.error", detail)),
                () -> {
                    ResponseEntity<AccountDto> response = accountService.findById(accountId);
                    AccountDto account = response.getBody();
                    if (account == null) {
                        throw new IllegalStateException(I18n.t("ims.account.dialog.toggle.not.found"));
                    }
                    account.setAdminStatus(currentlyEnabled
                            ? IEnumEnabledBinaryStatus.Types.DISABLED
                            : IEnumEnabledBinaryStatus.Types.ENABLED);
                    return accountService.update(accountId, account);
                },
                onSuccess,
                parentView::showLoading,
                "ims-dialog",
                false, // simple confirmation, no PIN
                ButtonVariant.LUMO_PRIMARY);
    }

    private static boolean fetchCurrentStatus(AccountService accountService, Long accountId) {
        try {
            ResponseEntity<AccountDto> response = accountService.findById(accountId);
            if (response.getBody() != null) {
                return response.getBody().getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED;
            }
        } catch (Exception ignored) {
        }
        return true; // default
    }
}
