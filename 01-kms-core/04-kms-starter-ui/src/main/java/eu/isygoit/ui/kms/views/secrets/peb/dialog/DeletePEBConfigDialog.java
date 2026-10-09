package eu.isygoit.ui.kms.views.secrets.peb.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.PEBConfigService;
import eu.isygoit.ui.common.dialog.DeleteActionDialog;

public class DeletePEBConfigDialog extends DeleteActionDialog {

    public DeletePEBConfigDialog(PEBConfigService configService, Long configId, String code, Runnable onSuccess) {
        super(new Texts(
                        I18n.t("kms.peb.dialog.delete.title"),
                        I18n.t("kms.peb.dialog.delete.confirmation", code),
                        I18n.t("kms.peb.dialog.delete.button"),
                        I18n.t("kms.peb.dialog.delete.invalid.code"),
                        I18n.t("kms.peb.dialog.delete.success"),
                        detail -> I18n.t("kms.peb.dialog.delete.error", detail)),
                () -> configService.delete(configId),
                onSuccess,
                null,
                "kms-dialog");
    }
}
