package eu.isygoit.ui.ims.views.account.dialog;

import eu.isygoit.constants.AccountTypeConstants;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AccountImageService;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.remote.ims.TenantService;
import eu.isygoit.ui.ims.views.account.AccountManagementView;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;

/**
 * Create dialog for {@link AccountDto}. The form itself lives in
 * {@link AbstractAccountFormDialog}; this class only creates the account.
 */
public class CreateAccountDialog extends AbstractAccountFormDialog {

    public CreateAccountDialog(AccountManagementView parentView,
                               AccountService accountService,
                               AccountImageService accountImageService,
                               TenantService tenantService,
                               Runnable onSuccess) {
        super(I18n.t("ims.account.dialog.create.title"), onSuccess, "ims.account.dialog.create",
                parentView, accountService, accountImageService, tenantService);
        setOkButtonText(I18n.t("ims.account.dialog.create.button"));

        buildForm();
        accountTypeCombo.setValue(AccountTypeConstants.TENANT_USER);
        languageCombo.setValue(IEnumLanguage.Types.EN);
        adminStatusCombo.setValue(IEnumEnabledBinaryStatus.Types.ENABLED);
    }

    @Override
    AccountDto target() {
        AccountDto account = new AccountDto();
        account.setRoleInfo(new ArrayList<>());
        return account;
    }

    @Override
    boolean isTenantReadOnly() {
        return false;
    }

    @Override
    String uploadLabelKey() {
        return "ims.account.dialog.field.upload.image";
    }

    @Override
    boolean imageRequired() {
        return true;
    }

    @Override
    Long persist(AccountDto dto) {
        ResponseEntity<AccountDto> response = accountService.create(dto);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            append(I18n.t("ims.account.dialog.create.failed", response.getStatusCodeValue()));
            return null;
        }
        Long accountId = response.getBody().getId();
        if (accountId == null) {
            append(I18n.t("ims.account.dialog.create.no.id"));
        }
        return accountId;
    }
}
