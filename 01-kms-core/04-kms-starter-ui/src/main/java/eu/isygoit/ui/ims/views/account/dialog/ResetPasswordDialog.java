package eu.isygoit.ui.ims.views.account.dialog;

import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.ims.views.account.AccountManagementView;
import feign.FeignException;
import eu.isygoit.ui.common.view.ManagementVerticalView;

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
        super("Reset password",
                "A password reset link will be sent to " + email + ".",
                onSuccess,
                false); // requirePin = false (simple confirmation)
        this.parentView = parentView;
        this.accountService = accountService;
        this.accountId = accountId;
        this.email = email;

        setOkButtonText("Send reset link");
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
            append("Password reset email sent to " + email);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            append("Operation interrupted");
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