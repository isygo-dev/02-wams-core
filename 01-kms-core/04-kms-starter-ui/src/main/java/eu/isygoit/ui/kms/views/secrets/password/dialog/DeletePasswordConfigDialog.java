package eu.isygoit.ui.kms.views.secrets.password.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.PasswordConfigService;
import eu.isygoit.ui.common.dialog.DeleteActionDialog;

public class DeletePasswordConfigDialog extends DeleteActionDialog {

    public DeletePasswordConfigDialog(PasswordConfigService configService, Long configId, String code, Runnable onSuccess) {
        super(new Texts(
                        I18n.t("kms.password.dialog.delete.title"),
                        I18n.t("kms.password.dialog.delete.confirmation", code),
                        I18n.t("kms.password.dialog.delete.button"),
                        I18n.t("kms.password.dialog.delete.invalid.code"),
                        I18n.t("kms.password.dialog.delete.success"),
                        detail -> I18n.t("kms.password.dialog.delete.failed", detail)),
                () -> configService.delete(configId),
                onSuccess,
                null,
                "kms-dialog");
    }
}
