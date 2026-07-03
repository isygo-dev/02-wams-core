package eu.isygoit.ui.kms.views.cryptography.random.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.RandomKeyService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;

public class DeleteRandomKeyDialog extends PinBaseActionDialog {

    private final RandomKeyService keyService;
    private final String keyName;

    public DeleteRandomKeyDialog(RandomKeyService keyService, String keyName, Runnable onSuccess) {
        super(I18n.t("kms.random.key.dialog.delete.title"),
                I18n.t("kms.random.key.dialog.delete.message"),
                onSuccess, true);
        this.keyService = keyService;
        this.keyName = keyName;
        setOkButtonText(I18n.t("kms.random.key.dialog.delete.button"));
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(I18n.t("kms.random.key.dialog.delete.invalid.code"));
            return false;
        }

        try {
            keyService.deleteRandomKey(keyName);
            append(I18n.t("kms.random.key.dialog.delete.success"));
            return true;
        } catch (Exception e) {
            append(I18n.t("kms.random.key.dialog.delete.failed", e.getMessage()));
            return false;
        }
    }
}