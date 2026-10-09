package eu.isygoit.ui.kms.views.secrets.digest.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.DigestConfigService;
import eu.isygoit.ui.common.dialog.DeleteActionDialog;

public class DeleteDigestConfigDialog extends DeleteActionDialog {

    public DeleteDigestConfigDialog(DigestConfigService configService, Long configId, String code, Runnable onSuccess) {
        super(new Texts(
                        I18n.t("kms.digest.dialog.delete.title"),
                        I18n.t("kms.digest.dialog.delete.confirmation", code),
                        I18n.t("kms.digest.dialog.delete.button"),
                        I18n.t("kms.digest.dialog.delete.invalid.code"),
                        I18n.t("kms.digest.dialog.delete.success"),
                        detail -> I18n.t("kms.digest.dialog.delete.failed", detail)),
                () -> configService.delete(configId),
                onSuccess,
                null,
                "kms-dialog");
    }
}
