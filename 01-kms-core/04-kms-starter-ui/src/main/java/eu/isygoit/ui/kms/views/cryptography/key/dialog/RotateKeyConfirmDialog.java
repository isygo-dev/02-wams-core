package eu.isygoit.ui.kms.views.cryptography.key.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.PinConfirmActionDialog;
import eu.isygoit.ui.kms.views.cryptography.key.KeyManagementView;

public class RotateKeyConfirmDialog extends PinConfirmActionDialog {

    public RotateKeyConfirmDialog(KeyManagementView parentView,
                                  KmsApiService kmsApiService,
                                  String keyId,
                                  Runnable onSuccess) {
        super(new Texts(
                        I18n.t("kms.key.dialog.rotate.title"),
                        I18n.t("kms.key.dialog.rotate.message"),
                        I18n.t("kms.key.dialog.rotate.button"),
                        I18n.t("common.dialog.pin.invalid"),
                        I18n.t("kms.key.dialog.rotate.success"),
                        detail -> detail != null && detail.startsWith("HTTP ")
                                ? I18n.t("kms.key.dialog.rotate.failed", detail.substring(5))
                                : I18n.t("kms.key.dialog.rotate.error", detail)),
                () -> kmsApiService.rotateKey(keyId),
                onSuccess,
                parentView::showLoading,
                "kms-dialog",
                true,
                ButtonVariant.LUMO_PRIMARY);
    }
}
