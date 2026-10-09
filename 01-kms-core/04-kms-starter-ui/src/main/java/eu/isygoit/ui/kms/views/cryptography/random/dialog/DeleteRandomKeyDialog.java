package eu.isygoit.ui.kms.views.cryptography.random.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.RandomKeyService;
import eu.isygoit.ui.common.dialog.DeleteActionDialog;

public class DeleteRandomKeyDialog extends DeleteActionDialog {

    public DeleteRandomKeyDialog(RandomKeyService keyService, String keyName, Runnable onSuccess) {
        super(new Texts(
                        I18n.t("kms.random.key.dialog.delete.title"),
                        I18n.t("kms.random.key.dialog.delete.message"),
                        I18n.t("kms.random.key.dialog.delete.button"),
                        I18n.t("kms.random.key.dialog.delete.invalid.code"),
                        I18n.t("kms.random.key.dialog.delete.success"),
                        detail -> I18n.t("kms.random.key.dialog.delete.failed", detail)),
                () -> keyService.deleteRandomKey(keyName),
                onSuccess,
                null,
                "kms-dialog");
    }
}
