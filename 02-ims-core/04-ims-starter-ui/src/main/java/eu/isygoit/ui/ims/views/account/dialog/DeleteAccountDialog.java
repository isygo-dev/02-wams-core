package eu.isygoit.ui.ims.views.account.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.ui.common.dialog.DeleteActionDialog;
import eu.isygoit.ui.ims.views.account.AccountManagementView;

public class DeleteAccountDialog extends DeleteActionDialog {

    public DeleteAccountDialog(AccountManagementView parentView,
                               AccountService accountService,
                               Long accountId,
                               Runnable onSuccess) {
        super(new Texts(
                        I18n.t("ims.account.dialog.delete.title"),
                        I18n.t("ims.account.dialog.delete.message"),
                        I18n.t("ims.account.dialog.delete.button"),
                        I18n.t("ims.account.dialog.delete.invalid.code"),
                        I18n.t("ims.account.dialog.delete.success"),
                        detail -> I18n.t("ims.account.dialog.delete.error", detail)),
                () -> accountService.delete(accountId),
                onSuccess,
                parentView::showLoading,
                "ims-dialog");
    }
}
