package eu.isygoit.ui.kms.views.cryptography.incremental.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsAppNextCodeService;
import eu.isygoit.ui.common.dialog.DeleteActionDialog;

public class DeleteNextCodeConfigDialog extends DeleteActionDialog {

    public DeleteNextCodeConfigDialog(KmsAppNextCodeService nextCodeService,
                                      Long configId,
                                      String entity,
                                      String attribute,
                                      Runnable onSuccess) {
        super(new Texts(
                        I18n.t("kms.delete.nextcode.dialog.title"),
                        I18n.t("kms.delete.nextcode.dialog.message", entity, attribute),
                        I18n.t("kms.delete.nextcode.dialog.button"),
                        I18n.t("kms.delete.nextcode.dialog.invalid.code"),
                        I18n.t("kms.delete.nextcode.dialog.success"),
                        detail -> I18n.t("kms.delete.nextcode.dialog.failed", detail)),
                () -> nextCodeService.delete(configId),
                onSuccess,
                null,
                "kms-dialog");
    }
}
