package eu.isygoit.ui.ims.views.account.dialog;

import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AccountImageService;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.remote.ims.TenantService;
import eu.isygoit.ui.ims.views.account.AccountManagementView;
import eu.isygoit.ui.ims.views.common.ImsDialogSupport;
import feign.FeignException;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

/**
 * Update dialog for {@link AccountDto}. The form itself lives in
 * {@link AbstractAccountFormDialog}; this class loads the account and only
 * updates it. The tenant is read-only.
 */
public class UpdateAccountDialog extends AbstractAccountFormDialog {

    private final Long accountId;
    private AccountDto currentAccount;

    public UpdateAccountDialog(AccountManagementView parentView,
                               AccountService accountService,
                               AccountImageService accountImageService,
                               TenantService tenantService,
                               Long accountId,
                               Runnable onSuccess) {
        super(I18n.t("ims.account.dialog.update.title"), onSuccess, "ims.account.dialog.update",
                parentView, accountService, accountImageService, tenantService);
        this.accountId = accountId;
        setOkButtonText(I18n.t("ims.account.dialog.update.button"));

        buildForm();
        loadAccountData();
    }

    @Override
    AccountDto target() {
        return currentAccount;
    }

    @Override
    boolean isTenantReadOnly() {
        return true;
    }

    @Override
    String uploadLabelKey() {
        return "ims.account.dialog.field.change.image";
    }

    @Override
    boolean imageRequired() {
        return false;
    }

    @Override
    Long persist(AccountDto dto) {
        ResponseEntity<AccountDto> response = accountService.update(accountId, dto);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            append(I18n.t("ims.account.dialog.update.failed", response.getStatusCodeValue()));
            return null;
        }
        return accountId;
    }

    private void loadAccountData() {
        parentView.showLoading(true);
        try {
            ResponseEntity<AccountDto> response = accountService.findById(accountId);
            if (response.getBody() != null) {
                currentAccount = response.getBody();
                fillFrom(currentAccount);
                loadExistingImage();
            } else {
                append(I18n.t("ims.account.dialog.update.not.found"));
                close();
            }
        } catch (FeignException ex) {
            append(I18n.t("ims.account.dialog.update.load.error", ImsDialogSupport.extractErrorMessage(ex)));
            close();
        } catch (Exception e) {
            append(I18n.t("ims.account.dialog.update.load.error", e.getMessage()));
            close();
        } finally {
            parentView.showLoading(false);
        }
    }

    private void loadExistingImage() {
        try {
            ResponseEntity<Resource> response = accountImageService.downloadImage(accountId);
            photoSection.showExisting(ImsDialogSupport.toImageDataUri(response));
        } catch (Exception ignored) {
            // No existing image: keep the placeholder
        }
    }
}
