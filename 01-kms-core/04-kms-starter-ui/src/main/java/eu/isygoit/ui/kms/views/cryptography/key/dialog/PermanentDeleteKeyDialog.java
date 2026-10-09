package eu.isygoit.ui.kms.views.cryptography.key.dialog;

import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.DeleteActionDialog;
import eu.isygoit.ui.kms.views.cryptography.key.KeyManagementView;

public class PermanentDeleteKeyDialog extends DeleteActionDialog {

    public PermanentDeleteKeyDialog(KeyManagementView parentView,
                                    KmsApiService kmsApiService,
                                    String keyId,
                                    Runnable onSuccess) {
        super(new Texts(
                        I18n.t("kms.key.dialog.permanent.title"),
                        I18n.t("kms.key.dialog.permanent.message"),
                        I18n.t("kms.key.dialog.permanent.button"),
                        I18n.t("kms.key.dialog.permanent.invalid.code"),
                        I18n.t("kms.key.dialog.permanent.success"),
                        detail -> I18n.t("kms.key.dialog.permanent.error", detail)),
                () -> kmsApiService.deleteKey(keyId),
                onSuccess,
                parentView::showLoading,
                "kms-dialog");
    }
}
