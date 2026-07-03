package eu.isygoit.ui.ims.views.account.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.ims.views.account.AccountManagementView;
import feign.FeignException;

public class ResetPasswordDialog extends PinBaseActionDialog {

    private final AccountManagementView parentView;
    private final AccountService accountService;
    private final Long accountId;
    private final String email;

    public ResetPasswordDialog(AccountManagementView parentView,
                               AccountService accountService,
                               Long accountId,
                               String email,
                               Runnable onSuccess) {
        super(I18n.t("ims.account.dialog.reset.title"),
                I18n.t("ims.account.dialog.reset.message", email),
                onSuccess,
                false); // requirePin = false (simple confirmation)
        this.parentView = parentView;
        this.accountService = accountService;
        this.accountId = accountId;
        this.email = email;

        setOkButtonText(I18n.t("ims.account.dialog.reset.button"));
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        parentView.showLoading(true);
        try {
            // Placeholder – replace with actual API call when available
            // For example:
            // ResetPasswordRequest request = new ResetPasswordRequest(accountId, email);
            // ResponseEntity<Void> response = accountService.resetPassword(request);
            // if (!response.getStatusCode().is2xxSuccessful()) { ... }

            // Simulate network call
            Thread.sleep(500);
            append(I18n.t("ims.account.dialog.reset.success", email));
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            append(I18n.t("ims.account.dialog.reset.interrupted"));
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("ims.account.dialog.reset.failed", e.getMessage()));
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