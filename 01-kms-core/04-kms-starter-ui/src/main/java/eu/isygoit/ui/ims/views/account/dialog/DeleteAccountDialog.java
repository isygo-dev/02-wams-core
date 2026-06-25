package eu.isygoit.ui.ims.views.account.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.ims.views.account.AccountManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class DeleteAccountDialog extends PinBaseActionDialog {

    private final AccountManagementView parentView;
    private final AccountService accountService;
    private final Long accountId;

    public DeleteAccountDialog(AccountManagementView parentView,
                               AccountService accountService,
                               Long accountId,
                               Runnable onSuccess) {
        super(I18n.t("account.dialog.delete.title"),
                I18n.t("account.dialog.delete.message"),
                onSuccess);
        this.parentView = parentView;
        this.accountService = accountService;
        this.accountId = accountId;

        setOkButtonText(I18n.t("account.dialog.delete.button"));
        addThemeVariantsOkButton(com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(I18n.t("account.dialog.delete.invalid.code"));
            return false;
        }

        parentView.showLoading(true);
        try {
            ResponseEntity<?> response = accountService.delete(accountId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("account.dialog.delete.failed", response.getStatusCode()));
                return false;
            }

            append(I18n.t("account.dialog.delete.success"));
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("account.dialog.delete.error", e.getMessage()));
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