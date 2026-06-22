package eu.isygoit.ui.ims.views.account.dialog;

import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.ims.views.account.AccountManagementView;
import feign.FeignException;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import org.springframework.http.ResponseEntity;

public class DeleteAccountDialog extends PinBaseActionDialog {

    private final AccountManagementView parentView;
    private final AccountService accountService;
    private final Long accountId;

    public DeleteAccountDialog(AccountManagementView parentView,
                               AccountService accountService,
                               Long accountId,
                               Runnable onSuccess) {
        super("Delete account",
                "This action is irreversible. The account will be permanently removed.",
                onSuccess);
        this.parentView = parentView;
        this.accountService = accountService;
        this.accountId = accountId;

        setOkButtonText("Delete permanently");
        addThemeVariantsOkButton(com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append("Invalid confirmation code");
            return false;
        }

        parentView.showLoading(true);
        try {
            ResponseEntity<?> response = accountService.delete(accountId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append("Deletion failed: " + response.getStatusCode());
                return false;
            }

            append("Account deleted successfully");
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