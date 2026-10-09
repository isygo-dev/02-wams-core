package eu.isygoit.ui.kms.views.cryptography.key.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.PinConfirmActionDialog;

public class EnableKeyVersionDialog extends PinConfirmActionDialog {

    public EnableKeyVersionDialog(KmsApiService kmsApiService,
                                  String keyId,
                                  String versionId,
                                  Runnable onSuccess) {
        super(new Texts(
                        I18n.t("kms.key.dialog.enable.version.title"),
                        I18n.t("kms.key.dialog.enable.version.message"),
                        I18n.t("kms.key.dialog.enable.version.button"),
                        I18n.t("common.dialog.pin.invalid"),
                        I18n.t("kms.key.dialog.enable.version.success"),
                        detail -> detail != null && detail.startsWith("HTTP ")
                                ? I18n.t("kms.key.dialog.enable.version.failed", detail.substring(5))
                                : I18n.t("kms.key.dialog.enable.version.error", detail)),
                () -> kmsApiService.enableKeyVersion(keyId, versionId),
                onSuccess,
                null,
                "kms-dialog",
                true,
                ButtonVariant.LUMO_PRIMARY);
    }
}
