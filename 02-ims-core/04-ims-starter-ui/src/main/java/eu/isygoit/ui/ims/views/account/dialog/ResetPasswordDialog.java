package eu.isygoit.ui.ims.views.account.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.ui.common.dialog.PinConfirmActionDialog;
import eu.isygoit.ui.ims.views.account.AccountManagementView;

/**
 * Reset password confirmation. The remote call is still a placeholder (simulated latency).
 */
public class ResetPasswordDialog extends PinConfirmActionDialog {

    public ResetPasswordDialog(AccountManagementView parentView,
                               AccountService accountService,
                               Long accountId,
                               String email,
                               Runnable onSuccess) {
        super(new Texts(
                        I18n.t("ims.account.dialog.reset.title"),
                        I18n.t("ims.account.dialog.reset.message", email),
                        I18n.t("ims.account.dialog.reset.button"),
                        I18n.t("common.dialog.pin.invalid"),
                        I18n.t("ims.account.dialog.reset.success", email),
                        detail -> I18n.t("ims.account.dialog.reset.interrupted").equals(detail)
                                ? detail
                                : I18n.t("ims.account.dialog.reset.failed", detail)),
                () -> {
                    // Placeholder - replace with actual API call when available
                    // For example:
                    // ResetPasswordRequest request = new ResetPasswordRequest(accountId, email);
                    // return accountService.resetPassword(request);

                    // Simulate network call
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(I18n.t("ims.account.dialog.reset.interrupted"), e);
                    }
                    return null;
                },
                onSuccess,
                parentView::showLoading,
                "ims-dialog",
                false, // simple confirmation, no PIN
                ButtonVariant.LUMO_PRIMARY);
    }
}
